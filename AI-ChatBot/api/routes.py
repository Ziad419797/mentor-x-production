import asyncio
import json
import logging
import re
import time as _time
from typing import AsyncGenerator

from fastapi import APIRouter, Depends, HTTPException, Request, Security
from fastapi.security import APIKeyHeader
from fastapi.responses import StreamingResponse
from pydantic import BaseModel, constr
from slowapi import Limiter
from slowapi.util import get_remote_address

from config.settings import MENTOR_API_KEY

router = APIRouter()
logger = logging.getLogger("mentor-x-ai.routes")

_start_time = _time.time()
limiter = Limiter(key_func=get_remote_address)


# ── API Key Auth ──────────────────────────────────────────────
_api_key_header = APIKeyHeader(name="X-API-Key", auto_error=False)

def verify_api_key(api_key: str = Security(_api_key_header)) -> str:
    """
    Validate X-API-Key header against MENTOR_API_KEY env variable.
    If MENTOR_API_KEY is not set, auth is disabled (dev mode).
    """
    if not MENTOR_API_KEY:
        # Auth disabled — log warning once at startup, allow all requests
        return "dev-mode"
    if api_key != MENTOR_API_KEY:
        raise HTTPException(
            status_code=401,
            detail="Invalid or missing API key. Set X-API-Key header.",
        )
    return api_key


# ── Prompt Injection Protection ───────────────────────────────
INJECTION_PATTERNS = [
    r"ignore (all |previous |above )?instructions?",
    r"you are now",
    r"forget (everything|your instructions)",
    r"(system|assistant)\s*:",
    r"<\s*(script|iframe|img)[^>]*>",
    r"prompt\s*injection",
]


def _sanitize_question(text: str) -> str:
    """Detect and reject obvious prompt injection attempts."""
    lower = text.lower()
    for pattern in INJECTION_PATTERNS:
        if re.search(pattern, lower):
            raise HTTPException(
                status_code=400,
                detail="Invalid input detected.",
            )
    return text


# ── Models ────────────────────────────────────────────────────
class ChatRequest(BaseModel):
    question: constr(strip_whitespace=True, min_length=1, max_length=1200)


class IngestRequest(BaseModel):
    file_url: str
    file_name: str
    material_id: int
    lesson_id: int | None = None
    material_type: str = "PDF"   # PDF | DOC | PPT (للـ logging فقط)


class QuizRequest(BaseModel):
    content: constr(strip_whitespace=True, min_length=10, max_length=8000)
    num_questions: int = 5
    difficulty: str = "medium"
    lesson_id: int | None = None


class SummarizeRequest(BaseModel):
    content: constr(strip_whitespace=True, min_length=10, max_length=8000)
    language: str = "ar"
    lesson_id: int | None = None


def get_retriever(request: Request):
    retriever = getattr(request.app.state, "retriever", None)
    if retriever is None:
        logger.warning("Retrieval service requested before startup completion")
        raise HTTPException(
            status_code=503,
            detail="Service is not ready. Please try again later.",
        )
    return retriever


# ── Endpoints ─────────────────────────────────────────────────
@router.get("/health")
async def health(request: Request):
    retriever = getattr(request.app.state, "retriever", None)
    ready = retriever is not None

    info: dict = {
        "status": "ok" if ready else "starting",
        "service": "Mentor-X AI",
        "ready": ready,
        "uptime_seconds": round(_time.time() - _start_time, 1),
    }

    if ready:
        try:
            store_info = retriever.store.info()
            info["vector_store"] = {
                "docs_count": store_info["docs_count"],
                "collection": store_info["collection"],
                "embedding_model": store_info["embedding_model"],
            }
        except Exception:
            info["vector_store"] = {"error": "unavailable"}

        if hasattr(retriever, "semantic_cache"):
            info["semantic_cache"] = {
                "cache_size": len(retriever.semantic_cache.cache),
                "cache_hits": retriever.semantic_cache.hits,
                "cache_misses": retriever.semantic_cache.misses,
                "cache_max_size": retriever.semantic_cache.max_size,
            }

    return info


@router.post("/chat")
@limiter.limit("20/minute")
async def chat(request: Request, body: ChatRequest, retriever=Depends(get_retriever), debug: bool = False, _key: str = Depends(verify_api_key)):
    question = _sanitize_question(body.question)
    start_time = _time.perf_counter()
    try:
        # Run retriever.ask() in thread pool to avoid blocking the event loop
        result = await asyncio.to_thread(retriever.ask, question)
    except Exception:
        logger.exception("Chat request failed")
        raise HTTPException(
            status_code=500,
            detail="Failed to process the chat request.",
        )

    response_time_ms = (_time.perf_counter() - start_time) * 1000
    response = {
        "question": question,
        "answer": result["answer"],
        "sources": result["sources"],
        "response_time_ms": round(response_time_ms, 2),
    }

    if debug:
        response["hallucination"] = result.get("hallucination")

    return response


@router.post("/chat/stream")
@limiter.limit("20/minute")
async def chat_stream(request: Request, body: ChatRequest, retriever=Depends(get_retriever), _key: str = Depends(verify_api_key)):
    """Stream LLM response token by token using Server-Sent Events (SSE)."""
    question = _sanitize_question(body.question)
    start_time = _time.perf_counter()

    async def generate() -> AsyncGenerator[str, None]:
        try:
            logger.info("Streaming chat request: %s", question[:80])

            # Retrieve + rerank in thread pool (CPU-bound)
            result = await asyncio.to_thread(
                _prepare_streaming_context,
                retriever,
                question,
            )

            if "error" in result:
                yield f"data: {json.dumps({'error': result['error']})}\n\n"
                return

            context = result["context"]
            sources = result["sources"]

            # Stream LLM response token by token
            try:
                if hasattr(retriever.chain, "astream"):
                    async for token in retriever.chain.astream({
                        "context": context,
                        "question": question,
                    }):
                        yield f"data: {json.dumps({'token': token})}\n\n"
                else:
                    # Fallback: run sync chain in thread pool, simulate streaming
                    result_text = await asyncio.to_thread(
                        lambda: retriever.chain.invoke({
                            "context": context,
                            "question": question,
                        })
                    )
                    for word in result_text.split():
                        yield f"data: {json.dumps({'token': word + ' '})}\n\n"
                        await asyncio.sleep(0.01)

            except Exception:
                logger.exception("LLM streaming failed")
                yield f"data: {json.dumps({'error': 'LLM streaming failed'})}\n\n"
                return

            # Send sources + timing at the end
            response_time_ms = (_time.perf_counter() - start_time) * 1000
            yield f"data: {json.dumps({'sources': sources, 'response_time_ms': round(response_time_ms, 2)})}\n\n"
            yield "data: [DONE]\n\n"

        except Exception:
            logger.exception("Stream generation failed")
            yield f"data: {json.dumps({'error': 'Stream generation failed'})}\n\n"

    return StreamingResponse(generate(), media_type="text/event-stream")


# ── Ingest: رفع مادة دراسية للـ vector store ──────────────────
@router.post("/ingest")
@limiter.limit("30/minute")
async def ingest_material(request: Request, body: IngestRequest, _key: str = Depends(verify_api_key)):
    """
    Download a lesson material from its Cloudinary URL and add it to the vector store.
    Called automatically by Spring Boot when a teacher uploads a PDF/DOC/PPT.
    """
    retriever = getattr(request.app.state, "retriever", None)
    if retriever is None:
        raise HTTPException(status_code=503, detail="Service is not ready.")

    try:
        from dataIngestion.document_processor import process_from_url
        chunks = await asyncio.to_thread(
            process_from_url,
            body.file_url,
            body.file_name,
            body.material_id,
            body.lesson_id,
        )
    except ValueError as e:
        raise HTTPException(status_code=400, detail=str(e))
    except RuntimeError as e:
        logger.exception("Failed to download material_id=%d", body.material_id)
        raise HTTPException(status_code=502, detail=str(e))
    except Exception:
        logger.exception("Ingest failed for material_id=%d", body.material_id)
        raise HTTPException(status_code=500, detail="Failed to process document.")

    added = retriever.store.ingest_material(chunks, body.material_id, body.lesson_id)

    # Rebuild BM25 index to include new docs
    retriever.hybrid_retriever._bm25 = None

    logger.info(
        "Ingested material_id=%d ('%s') → %d chunks added",
        body.material_id, body.file_name, added,
    )

    return {
        "material_id": body.material_id,
        "lesson_id": body.lesson_id,
        "file_name": body.file_name,
        "chunks_added": added,
        "total_docs": retriever.store.get_collection_count(),
    }


@router.delete("/ingest/{material_id}")
async def delete_ingested_material(request: Request, material_id: int, _key: str = Depends(verify_api_key)):
    """
    Remove all vector store chunks belonging to a deleted lesson material.
    Called automatically by Spring Boot when a teacher deletes a material.
    """
    retriever = getattr(request.app.state, "retriever", None)
    if retriever is None:
        raise HTTPException(status_code=503, detail="Service is not ready.")

    deleted = retriever.store.delete_by_material_id(material_id)

    # Rebuild BM25 index
    retriever.hybrid_retriever._bm25 = None

    logger.info("Deleted %d chunks for material_id=%d", deleted, material_id)

    return {
        "material_id": material_id,
        "chunks_deleted": deleted,
        "total_docs": retriever.store.get_collection_count(),
    }


# ── Quiz Generation ───────────────────────────────────────────
@router.post("/quiz")
@limiter.limit("10/minute")
async def generate_quiz(request: Request, body: QuizRequest, _key: str = Depends(verify_api_key)):
    """Generate MCQ quiz questions from text content using the LLM."""
    from api.main import app as _app
    retriever = getattr(request.app.state, "retriever", None)
    if retriever is None:
        raise HTTPException(status_code=503, detail="Service is not ready.")

    difficulty_map = {"easy": "سهلة", "medium": "متوسطة", "hard": "صعبة"}
    difficulty_label = difficulty_map.get(body.difficulty, "متوسطة")

    lang_instruction = "in Arabic" if True else "in English"

    prompt_text = f"""You are an expert quiz generator. Generate exactly {body.num_questions} multiple-choice questions from the content below.
Difficulty level: {difficulty_label} ({body.difficulty}).
Rules:
- Each question must have 4 options labeled A, B, C, D.
- Indicate the correct answer.
- Add a brief explanation for the correct answer.
- Write questions in Arabic.
- Return ONLY a valid JSON array, no markdown, no extra text.

Format:
[
  {{
    "question": "...",
    "options": {{"A": "...", "B": "...", "C": "...", "D": "..."}},
    "correct_answer": "A",
    "explanation": "..."
  }}
]

Content:
{body.content}
"""

    start_time = _time.perf_counter()
    try:
        import asyncio
        answer = await asyncio.to_thread(
            lambda: retriever.chain.invoke({
                "context": body.content,
                "question": prompt_text,
            })
        )
        # Extract JSON array from response
        import json as _json
        import re as _re
        json_match = _re.search(r'\[.*\]', answer, _re.DOTALL)
        if not json_match:
            raise ValueError("LLM did not return a valid JSON array")
        questions = _json.loads(json_match.group())
    except Exception:
        logger.exception("Quiz generation failed")
        raise HTTPException(status_code=500, detail="Failed to generate quiz questions.")

    return {
        "lesson_id": body.lesson_id,
        "num_questions": len(questions),
        "difficulty": body.difficulty,
        "questions": questions,
        "response_time_ms": round((_time.perf_counter() - start_time) * 1000, 2),
    }


# ── Summarize ──────────────────────────────────────────────────
@router.post("/summarize")
@limiter.limit("10/minute")
async def summarize(request: Request, body: SummarizeRequest, _key: str = Depends(verify_api_key)):
    """Summarize text content using the LLM."""
    retriever = getattr(request.app.state, "retriever", None)
    if retriever is None:
        raise HTTPException(status_code=503, detail="Service is not ready.")

    lang_instruction = "in Arabic" if body.language == "ar" else "in English"
    prompt_text = f"""Summarize the following educational content {lang_instruction}.
- Be concise and clear.
- Use bullet points for key ideas.
- Keep important terms and concepts.
- Output the summary only, no extra commentary.

Content:
{body.content}
"""

    start_time = _time.perf_counter()
    try:
        import asyncio
        summary = await asyncio.to_thread(
            lambda: retriever.chain.invoke({
                "context": body.content,
                "question": prompt_text,
            })
        )
    except Exception:
        logger.exception("Summarize failed")
        raise HTTPException(status_code=500, detail="Failed to summarize content.")

    return {
        "lesson_id": body.lesson_id,
        "language": body.language,
        "summary": summary,
        "response_time_ms": round((_time.perf_counter() - start_time) * 1000, 2),
    }


# ── Helpers ───────────────────────────────────────────────────
def _prepare_streaming_context(retriever, question: str) -> dict:
    """Retrieve and rerank context for streaming (runs in thread pool)."""
    try:
        from retrieval.retriever import (
            _build_context,
            _normalize_query,
            _query_rewrite,
            _prepare_cache_key,
            _reject_empty_query,
            _rerank,
        )

        question = _normalize_query(question)

        if _reject_empty_query(question):
            return {"error": "Empty question"}

        rewritten_query = _query_rewrite(question)
        query_embedding = retriever.embeddings.embed_query(rewritten_query)
        cache_key = _prepare_cache_key(rewritten_query)
        cached_results = retriever.semantic_cache.get(cache_key, query_embedding)

        if cached_results is not None:
            docs_with_scores = cached_results
            logger.info("Streaming using semantic cache for query.")
        else:
            docs_with_scores = retriever.hybrid_retriever.search(rewritten_query, k=8)
            retriever.semantic_cache.set(cache_key, query_embedding, docs_with_scores)

        ranked_results = _rerank(docs_with_scores, rewritten_query)

        if not ranked_results:
            return {"error": "No relevant context found"}

        ranked_docs = [doc for doc, _ in ranked_results]
        context = _build_context(ranked_docs)

        sources = [
            {
                "source": doc.metadata.get("source", doc.metadata.get("filename", "unknown")),
                "page": doc.metadata.get("page_number", "?"),
                "score": round(score, 4),
                "preview": doc.page_content[:150] + "...",
            }
            for doc, score in ranked_results[:3]
        ]

        return {"context": context, "sources": sources}

    except Exception as exc:
        logger.exception("Context preparation failed")
        return {"error": str(exc)}