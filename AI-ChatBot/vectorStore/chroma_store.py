import logging
import os
from typing import List, Tuple

from langchain_chroma import Chroma

try:
    from langchain_core.documents import Document
except ImportError:
    try:
        from langchain.schema import Document
    except ImportError:
        from langchain.docstore.document import Document

from config.settings import CHROMA_DB_DIR, COLLECTION_NAME, EMBEDDING_MODEL, TOP_K_RESULTS
from utils.models import SingletonEmbeddings

logger = logging.getLogger("mentor-x-ai.vectorstore")


class VectorStoreManager:
    def __init__(self, persist_directory: str = str(CHROMA_DB_DIR), collection_name: str = COLLECTION_NAME):
        self.persist_directory = persist_directory
        self.collection_name = collection_name
        os.makedirs(self.persist_directory, exist_ok=True)

        logger.info("Loading embedding function: %s", EMBEDDING_MODEL)
        self.embedding_fn = SingletonEmbeddings(EMBEDDING_MODEL)

        self.db = Chroma(
            collection_name=self.collection_name,
            embedding_function=self.embedding_fn,
            persist_directory=self.persist_directory,
        )

        count = self._count()
        logger.info(
            "Vector store ready — %s docs in '%s'",
            count,
            self.persist_directory,
        )

        self._check_embedding_version()

    # ── Initial ingestion (first-time setup) ──────────────────
    def add_documents(self, chunks: List[Document], allow_append: bool = False) -> None:
        if self.get_collection_count() > 0 and not allow_append:
            logger.warning(
                "Vector store already has %d docs; skipping ingestion.",
                self.get_collection_count(),
            )
            return

        logger.info("Adding %d chunks to vector store...", len(chunks))
        self.db.add_documents(chunks)
        logger.info("Done. %d docs stored.", self.get_collection_count())

    # ── Dynamic ingestion: always appends ─────────────────────
    def ingest_material(self, chunks: List[Document], material_id: int, lesson_id: int | None = None) -> int:
        """
        Append chunks for a specific lesson material.
        Each chunk gets material_id + lesson_id in metadata.
        Safe to call multiple times — old chunks for same material_id are replaced first.
        """
        # Remove old chunks for this material to avoid duplicates on re-upload
        self.delete_by_material_id(material_id)

        # Stamp metadata
        for chunk in chunks:
            chunk.metadata["material_id"] = material_id
            if lesson_id is not None:
                chunk.metadata["lesson_id"] = lesson_id

        if chunks:
            self.db.add_documents(chunks)
            logger.info(
                "Ingested %d chunks for material_id=%d lesson_id=%s (total in store: %d)",
                len(chunks), material_id, lesson_id, self._count(),
            )

        return len(chunks)

    # ── Delete all chunks belonging to a material ──────────────
    def delete_by_material_id(self, material_id: int) -> int:
        """
        Remove all chunks that have metadata.material_id == material_id.
        Returns the number of chunks deleted.
        """
        try:
            results = self.db._collection.get(
                where={"material_id": {"$eq": material_id}},
                include=[],  # only need IDs
            )
            ids = results.get("ids", [])
            if ids:
                self.db._collection.delete(ids=ids)
                logger.info(
                    "Deleted %d chunks for material_id=%d (remaining: %d)",
                    len(ids), material_id, self._count(),
                )
            return len(ids)
        except Exception:
            logger.exception("Failed to delete chunks for material_id=%d", material_id)
            return 0

    def reset(self) -> None:
        logger.warning("Resetting '%s' vector store", self.collection_name)
        self.db.delete_collection()
        self.db = Chroma(
            collection_name=self.collection_name,
            embedding_function=self.embedding_fn,
            persist_directory=self.persist_directory,
        )
        logger.info("Vector store reset complete")

    def similarity_search(self, query: str, k: int = TOP_K_RESULTS) -> List[Document]:
        self._check_not_empty()
        results = self.db.similarity_search(query, k=k)
        logger.info("Similarity search returned %d results for query sample '%s'",
                    len(results), query[:50])
        return results

    def search_with_score(self, query: str, k: int = TOP_K_RESULTS) -> List[Tuple[Document, float]]:
        self._check_not_empty()
        return self.db.similarity_search_with_relevance_scores(query, k=k)

    def as_retriever(self, k: int = TOP_K_RESULTS):
        return self.db.as_retriever(search_kwargs={"k": k})

    def _count(self) -> int:
        return self.db._collection.count()

    def get_collection_count(self) -> int:
        return self._count()

    def _check_not_empty(self) -> None:
        if self._count() == 0:
            raise RuntimeError("[VectorStore] Collection is empty! Run ingestion first.")

    def info(self) -> dict:
        return {
            "collection": self.collection_name,
            "docs_count": self._count(),
            "persist_directory": self.persist_directory,
            "embedding_model": EMBEDDING_MODEL,
        }

    def _check_embedding_version(self) -> None:
        try:
            meta = self.db._collection.metadata or {}
            stored_model = meta.get("embedding_model")

            if stored_model is None:
                self.db._collection.modify(metadata={"embedding_model": EMBEDDING_MODEL})
                logger.info("Embedding model version stored: %s", EMBEDDING_MODEL)
            elif stored_model != EMBEDDING_MODEL:
                logger.error(
                    "EMBEDDING MODEL MISMATCH! stored='%s' current='%s'. Re-index required.",
                    stored_model, EMBEDDING_MODEL,
                )
                raise RuntimeError(
                    f"Embedding model mismatch: stored='{stored_model}', "
                    f"current='{EMBEDDING_MODEL}'. Re-index required."
                )
            else:
                logger.info("Embedding model version verified: %s", EMBEDDING_MODEL)

        except RuntimeError:
            raise
        except Exception:
            logger.warning("Could not verify embedding model version", exc_info=True)
