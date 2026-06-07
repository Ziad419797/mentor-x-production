package com.educore.ai;

import com.educore.ai.dto.*;
import com.educore.common.GlobalResponse;
import com.educore.security.JwtUserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * AI endpoints — بتتصل بالـ Python AI service.
 *
 * Endpoints:
 *   GET  /api/ai/health          — حالة الـ AI service
 *   POST /api/ai/chat            — الطالب يسأل AI (يتطلب enrollment)
 *   POST /api/ai/quiz            — المدرس/الأدمن يولد أسئلة كويز
 *   POST /api/ai/summarize       — طلب ملخص (طالب مشترك أو مدرس/أدمن)
 */
@Slf4j
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
@Tag(name = "AI", description = "مساعد الذكاء الاصطناعي — RAG Q&A + Quiz Generation + Summarization")
public class AiController {

    private final AiService aiService;

    // ─────────────────────────────────────────────────────────────
    // Health
    // ─────────────────────────────────────────────────────────────

    @Operation(summary = "حالة الـ AI service", description = "بيتحقق إن الـ AI service شغال ومستعد")
    @GetMapping("/health")
    public ResponseEntity<GlobalResponse<Map<String, Object>>> health() {
        boolean ready = aiService.isAiHealthy();
        Map<String, Object> data = Map.of(
                "ready",   ready,
                "service", "Mentor-X AI",
                "status",  ready ? "online" : "offline"
        );
        return ResponseEntity.ok(GlobalResponse.success(
                ready ? "الـ AI service جاهز" : "الـ AI service غير متاح حالياً",
                data
        ));
    }

    // ─────────────────────────────────────────────────────────────
    // Chat — الطالب يسأل سؤال
    // ─────────────────────────────────────────────────────────────

    @Operation(
        summary     = "[STUDENT] سؤال للـ AI",
        description = "الطالب يسأل سؤال والـ AI يجيب من محتوى الكورسات (RAG). يتطلب enrollment نشط."
    )
    @PostMapping("/chat")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<GlobalResponse<AiChatResponse>> chat(
            @Valid @RequestBody AiChatRequest request,
            @AuthenticationPrincipal JwtUserPrincipal principal,
            @RequestHeader("X-Device-Id") String deviceId

    ) {
        AiChatResponse response = aiService.chat(principal.getUserId(), request);
        return ResponseEntity.ok(GlobalResponse.success("تم الرد بنجاح", response));
    }

    // ─────────────────────────────────────────────────────────────
    // Quiz Generation — المدرس / الأدمن
    // ─────────────────────────────────────────────────────────────

    @Operation(
        summary     = "[TEACHER/ADMIN] توليد أسئلة كويز بالـ AI",
        description = "بيبعت محتوى نصي للـ AI ويولد أسئلة MCQ جاهزة. للمدرسين والأدمن فقط."
    )
    @PostMapping("/quiz")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN', 'STAFF')")
    public ResponseEntity<GlobalResponse<AiQuizResponse>> generateQuiz(
            @Valid @RequestBody AiQuizRequest request,
            @AuthenticationPrincipal JwtUserPrincipal principal
    ) {
        log.info("Quiz generation requested by userId={} role={}", principal.getUserId(), principal.getRole());
        AiQuizResponse response = aiService.generateQuiz(request);
        return ResponseEntity.ok(GlobalResponse.success(
                "تم توليد " + response.getNumQuestions() + " سؤال بنجاح",
                response
        ));
    }

    // ─────────────────────────────────────────────────────────────
    // Summarize
    // ─────────────────────────────────────────────────────────────

    @Operation(
        summary     = "[STUDENT] ملخص AI للدرس",
        description = "الطالب المشترك يطلب ملخص لمحتوى درس. يتطلب enrollment نشط."
    )
    @PostMapping("/summarize/student")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<GlobalResponse<AiSummarizeResponse>> summarizeForStudent(
            @Valid @RequestBody AiSummarizeRequest request,
            @AuthenticationPrincipal JwtUserPrincipal principal,
            @RequestHeader("X-Device-Id") String deviceId

    ) {
        AiSummarizeResponse response = aiService.summarize(principal.getUserId(), request);
        return ResponseEntity.ok(GlobalResponse.success("تم إنشاء الملخص بنجاح", response));
    }

    @Operation(
        summary     = "[TEACHER/ADMIN] ملخص AI لمحتوى",
        description = "المدرس أو الأدمن يطلب ملخص لمحتوى أي درس بدون قيد enrollment."
    )
    @PostMapping("/summarize/staff")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN', 'STAFF')")
    public ResponseEntity<GlobalResponse<AiSummarizeResponse>> summarizeForStaff(
            @Valid @RequestBody AiSummarizeRequest request,
            @AuthenticationPrincipal JwtUserPrincipal principal
    ) {
        log.info("Summarize requested by staff userId={} role={}", principal.getUserId(), principal.getRole());
        // null عشان مش طالب — مش بيتحقق من enrollment
        AiSummarizeResponse response = aiService.summarize(null, request);
        return ResponseEntity.ok(GlobalResponse.success("تم إنشاء الملخص بنجاح", response));
    }

    // ─────────────────────────────────────────────────────────────
    // Error Handler
    // ─────────────────────────────────────────────────────────────

    @ExceptionHandler(AiServiceException.class)
    public ResponseEntity<GlobalResponse<Void>> handleAiError(AiServiceException e) {
        log.warn("AI service error: {}", e.getMessage());
        return ResponseEntity.status(503).body(
                GlobalResponse.<Void>builder()
                        .success(false)
                        .message(e.getMessage())
                        .build()
        );
    }
}
