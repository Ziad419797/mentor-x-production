package com.educore.ai;

import com.educore.ai.dto.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.Map;

/**
 * HTTP client بيتصل بالـ Python/FastAPI AI service.
 *
 * Base URL: ${ai.service.url} (افتراضي: http://localhost:8000)
 *
 * Endpoints:
 *   GET  /health
 *   POST /chat
 *   POST /quiz
 *   POST /summarize
 */
@Slf4j
@Component
public class AiClient {

    private final RestClient restClient;

    public AiClient(
            @Value("${ai.service.url:http://localhost:8000}") String aiBaseUrl,
            @Value("${ai.service.timeout-seconds:120}") int timeoutSeconds,
            @Value("${ai.service.api-key:}") String apiKey
    ) {
        // Force HTTP/1.1 — Uvicorn doesn't support HTTP/2 upgrade (h2c)
        // Default Java HttpClient tries h2c upgrade → Uvicorn returns 400 "Invalid HTTP request"
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(10));
        factory.setReadTimeout(Duration.ofSeconds(timeoutSeconds));

        RestClient.Builder builder = RestClient.builder()
                .requestFactory(factory)
                .baseUrl(aiBaseUrl)
                .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader("Accept",       MediaType.APPLICATION_JSON_VALUE);

        if (apiKey != null && !apiKey.isBlank()) {
            builder.defaultHeader("X-API-Key", apiKey);
        }

        this.restClient = builder.build();

        log.info("AiClient initialized → baseUrl={} timeout={}s apiKey={}",
                aiBaseUrl, timeoutSeconds, apiKey.isBlank() ? "none (dev mode)" : "set");
    }

    // ─────────────────────────────────────────────────────────────
    // Ingest — رفع مادة للـ vector store
    // ─────────────────────────────────────────────────────────────

    /**
     * يبعت ملف مادة دراسية للـ Python عشان تعمل ingestion.
     * بيُستخدم بعد رفع المدرس PDF/DOC/PPT.
     */
    public Map<String, Object> ingestMaterial(String fileUrl, String fileName, Long materialId, Long lessonId) {
        log.info("AI ingest: materialId={} fileName={}", materialId, fileName);
        try {
            var payload = new java.util.HashMap<String, Object>();
            payload.put("file_url",    fileUrl);
            payload.put("file_name",   fileName);
            payload.put("material_id", materialId);
            if (lessonId != null) payload.put("lesson_id", lessonId);

            return restClient.post()
                    .uri("/ingest")
                    .body(payload)
                    .retrieve()
                    .body(Map.class);
        } catch (Exception e) {
            log.warn("AI ingest failed for materialId={}: {}", materialId, e.getMessage());
            return Map.of("error", e.getMessage());
        }
    }

    /**
     * يحذف chunks الـ material من الـ vector store عند حذف المادة.
     */
    public void deleteIngested(Long materialId) {
        log.info("AI delete ingested: materialId={}", materialId);
        try {
            restClient.delete()
                    .uri("/ingest/{id}", materialId)
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception e) {
            log.warn("AI delete ingested failed for materialId={}: {}", materialId, e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────
    // Health Check
    // ─────────────────────────────────────────────────────────────

    /**
     * بيتحقق إن الـ AI service شغال ومستعد.
     * @return true لو الـ service جاهز، false في أي حالة تانية
     */
    public boolean isHealthy() {
        try {
            Map<?, ?> response = restClient.get()
                    .uri("/health")
                    .retrieve()
                    .body(Map.class);

            boolean ready = Boolean.TRUE.equals(response != null ? response.get("ready") : false);
            log.debug("AI health check: ready={}", ready);
            return ready;
        } catch (Exception e) {
            log.warn("AI health check failed: {}", e.getMessage());
            return false;
        }
    }

    // ─────────────────────────────────────────────────────────────
    // Chat (RAG Q&A)
    // ─────────────────────────────────────────────────────────────

    /**
     * بيبعت سؤال للـ AI ويجيب الإجابة مع المصادر.
     *
     * @param payload يحتوي على: question, student_id (اختياري), course_id (اختياري)
     * @return AiChatResponse فيها answer + sources
     * @throws AiServiceException لو الـ service مش متاح أو رجع error
     */
    public AiChatResponse chat(Map<String, Object> payload) {
        log.debug("AI chat request: question={}", payload.get("question"));
        try {
            return restClient.post()
                    .uri("/chat")
                    .body(payload)
                    .retrieve()
                    .body(AiChatResponse.class);
        } catch (ResourceAccessException e) {
            log.error("AI service unreachable for /chat: {}", e.getMessage());
            throw new AiServiceException("خدمة الـ AI غير متاحة حالياً، يرجى المحاولة لاحقاً");
        } catch (Exception e) {
            log.error("AI /chat error: {}", e.getMessage());
            throw new AiServiceException("حدث خطأ في معالجة سؤالك");
        }
    }

    // ─────────────────────────────────────────────────────────────
    // Quiz Generation
    // ─────────────────────────────────────────────────────────────

    /**
     * بيولد أسئلة كويز من محتوى نصي.
     *
     * @param payload يحتوي على: content, num_questions, difficulty, lesson_id
     * @return AiQuizResponse فيها قائمة الأسئلة MCQ
     */
    public AiQuizResponse generateQuiz(Map<String, Object> payload) {
        log.debug("AI quiz request: lessonId={} numQuestions={}",
                payload.get("lesson_id"), payload.get("num_questions"));
        try {
            return restClient.post()
                    .uri("/quiz")
                    .body(payload)
                    .retrieve()
                    .body(AiQuizResponse.class);
        } catch (ResourceAccessException e) {
            log.error("AI service unreachable for /quiz: {}", e.getMessage());
            throw new AiServiceException("خدمة الـ AI غير متاحة حالياً، يرجى المحاولة لاحقاً");
        } catch (Exception e) {
            log.error("AI /quiz error: {}", e.getMessage());
            throw new AiServiceException("حدث خطأ في توليد أسئلة الكويز");
        }
    }

    // ─────────────────────────────────────────────────────────────
    // Summarize
    // ─────────────────────────────────────────────────────────────

    /**
     * بيلخص محتوى درس.
     *
     * @param payload يحتوي على: content, language, lesson_id
     * @return AiSummarizeResponse فيها الملخص
     */
    public AiSummarizeResponse summarize(Map<String, Object> payload) {
        log.debug("AI summarize request: lessonId={} language={}",
                payload.get("lesson_id"), payload.get("language"));
        try {
            return restClient.post()
                    .uri("/summarize")
                    .body(payload)
                    .retrieve()
                    .body(AiSummarizeResponse.class);
        } catch (ResourceAccessException e) {
            log.error("AI service unreachable for /summarize: {}", e.getMessage());
            throw new AiServiceException("خدمة الـ AI غير متاحة حالياً، يرجى المحاولة لاحقاً");
        } catch (Exception e) {
            log.error("AI /summarize error: {}", e.getMessage());
            throw new AiServiceException("حدث خطأ في تلخيص المحتوى");
        }
    }
}
