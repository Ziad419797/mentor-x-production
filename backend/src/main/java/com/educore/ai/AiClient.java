package com.educore.ai;

import com.educore.ai.dto.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

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
            @Value("${ai.service.timeout-seconds:30}") int timeoutSeconds
    ) {
        this.restClient = RestClient.builder()
                .baseUrl(aiBaseUrl)
                .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader("Accept",       MediaType.APPLICATION_JSON_VALUE)
                .build();

        log.info("AiClient initialized → baseUrl={} timeout={}s", aiBaseUrl, timeoutSeconds);
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
