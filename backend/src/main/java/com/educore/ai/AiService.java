package com.educore.ai;

import com.educore.ai.dto.*;
import com.educore.enrollment.EnrollmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * Business logic طبقة بين الـ Controller والـ AiClient.
 *
 * مسؤوليات:
 *  1. التحقق من صلاحية الطالب (مشترك في كورس واحد على الأقل)
 *  2. تجهيز الـ payload وإرساله للـ AI service
 *  3. إرجاع النتيجة للـ Controller
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiService {

    private final AiClient             aiClient;
    private final EnrollmentRepository enrollmentRepository;

    // ─────────────────────────────────────────────────────────────
    // Chat — الطالب يسأل AI سؤال
    // ─────────────────────────────────────────────────────────────

    /**
     * الطالب يسأل سؤال — لازم يكون مشترك في كورس واحد على الأقل.
     *
     * @param studentId معرف الطالب
     * @param request   السؤال + courseId (اختياري)
     * @return إجابة الـ AI + المصادر
     */
    public AiChatResponse chat(Long studentId, AiChatRequest request) {
        // تحقق إن الطالب مشترك في أي كورس
        requireActiveEnrollment(studentId);

        Map<String, Object> payload = new HashMap<>();
        payload.put("question",   request.getQuestion());
        payload.put("student_id", studentId);
        if (request.getCourseId() != null) {
            payload.put("course_id", request.getCourseId());
        }

        log.info("AI chat: studentId={} courseId={} question={}",
                studentId, request.getCourseId(), request.getQuestion().substring(0, Math.min(50, request.getQuestion().length())));

        return aiClient.chat(payload);
    }

    // ─────────────────────────────────────────────────────────────
    // Quiz Generation — المدرس / الأدمن يولد أسئلة
    // ─────────────────────────────────────────────────────────────

    /**
     * يولد أسئلة كويز من محتوى نصي.
     * بيُستخدم من المدرسين/الأدمن (بدون قيد enrollment).
     *
     * @param request المحتوى + عدد الأسئلة + الصعوبة + lessonId
     * @return قائمة أسئلة MCQ جاهزة
     */
    public AiQuizResponse generateQuiz(AiQuizRequest request) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("content",       request.getContent());
        payload.put("num_questions", request.getNumQuestions());
        payload.put("difficulty",    request.getDifficulty());
        if (request.getLessonId() != null) {
            payload.put("lesson_id", request.getLessonId());
        }

        log.info("AI quiz: lessonId={} numQ={} difficulty={}",
                request.getLessonId(), request.getNumQuestions(), request.getDifficulty());

        return aiClient.generateQuiz(payload);
    }

    // ─────────────────────────────────────────────────────────────
    // Summarize — الطالب أو المدرس يطلب ملخص
    // ─────────────────────────────────────────────────────────────

    /**
     * يلخص محتوى درس.
     * للطلاب: يتحقق من الاشتراك أولاً.
     * للمدرسين/الأدمن: بدون قيد.
     *
     * @param studentId null لو الطالب مش صاحب الطلب
     * @param request   المحتوى + اللغة + lessonId
     */
    public AiSummarizeResponse summarize(Long studentId, AiSummarizeRequest request) {
        // الطلاب: لازم يكونوا مشتركين
        if (studentId != null) {
            requireActiveEnrollment(studentId);
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("content",  request.getContent());
        payload.put("language", request.getLanguage());
        if (request.getLessonId() != null) {
            payload.put("lesson_id", request.getLessonId());
        }

        log.info("AI summarize: lessonId={} language={} requestedBy={}",
                request.getLessonId(), request.getLanguage(),
                studentId != null ? "student:" + studentId : "staff");

        return aiClient.summarize(payload);
    }

    // ─────────────────────────────────────────────────────────────
    // Health
    // ─────────────────────────────────────────────────────────────

    /**
     * يتحقق إن الـ AI service شغال.
     */
    public boolean isAiHealthy() {
        return aiClient.isHealthy();
    }

    // ─────────────────────────────────────────────────────────────
    // Private Helpers
    // ─────────────────────────────────────────────────────────────

    /**
     * يتحقق إن الطالب مشترك في كورس واحد على الأقل.
     * لو مش مشترك، يرمي AiServiceException.
     */
    private void requireActiveEnrollment(Long studentId) {
        long count = enrollmentRepository.countActiveEnrollmentsByStudent(studentId);
        if (count == 0) {
            log.warn("Student {} tried to use AI without any active enrollment", studentId);
            throw new AiServiceException(
                    "يجب الاشتراك في كورس واحد على الأقل لاستخدام مساعد الـ AI"
            );
        }
    }
}
