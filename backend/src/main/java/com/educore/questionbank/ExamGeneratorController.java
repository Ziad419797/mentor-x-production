package com.educore.questionbank;

import com.educore.common.GlobalResponse;
import com.educore.questionbank.dto.GenerateExamRequest;
import com.educore.questionbank.dto.GeneratedExamSummary;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Exam Generator — يولّد كويز جديد تلقائياً من بنك الأسئلة.
 *
 * POST /api/question-bank/generate
 *
 * المدرس يحدد:
 *   - weekId        : الدرس
 *   - topicIds      : الجزئيات (اختياري — الكل لو مش محدد)
 *   - difficulty    : مستوى الصعوبة (ALL / EASY / MEDIUM / HARD)
 *   - shuffleQuestions : خلط ترتيب الأسئلة بين الطلبة
 *   - shuffleOptions   : خلط الاختيارات (آمن لأن correctAnswer نص مش index)
 *
 * السيستم:
 *   من كل جزئية → يجمع الأسئلة بالـ conceptTag
 *   من كل مجموعة → يختار سؤال واحد عشوائي
 *   النتيجة → تنوع كامل بدون تكرار للفكرة الواحدة
 */
@RestController
@RequestMapping("/api/question-bank")
@RequiredArgsConstructor
public class ExamGeneratorController {

    private final ExamGeneratorService generatorService;

    @PostMapping("/generate")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public ResponseEntity<GlobalResponse<GeneratedExamSummary>> generate(
            @Valid @RequestBody GenerateExamRequest request) {
        GeneratedExamSummary summary = generatorService.generate(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(GlobalResponse.success(
                        "تم توليد الامتحان بنجاح — " + summary.getQuestionCount() + " سؤال",
                        summary));
    }
}
