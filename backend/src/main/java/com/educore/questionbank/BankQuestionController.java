package com.educore.questionbank;

import com.educore.common.GlobalResponse;
import com.educore.questionbank.dto.BankQuestionRequest;
import com.educore.questionbank.dto.BankQuestionResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * CRUD لأسئلة بنك الأسئلة.
 *
 * GET  /api/question-bank/questions/{id}             → سؤال واحد
 * GET  /api/question-bank/questions/week/{weekId}    → أسئلة درس (paginated)
 * GET  /api/question-bank/questions/topic/{topicId}  → أسئلة جزئية (list)
 * POST /api/question-bank/questions                  → إضافة سؤال
 * PUT  /api/question-bank/questions/{id}             → تعديل سؤال
 * DELETE /api/question-bank/questions/{id}           → حذف ناعم
 */
@RestController
@RequestMapping("/api/question-bank/questions")
@RequiredArgsConstructor
public class BankQuestionController {

    private final BankQuestionService bankService;

    // ─── قراءة ───────────────────────────────────────────────────

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public ResponseEntity<GlobalResponse<BankQuestionResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(GlobalResponse.success(bankService.getById(id)));
    }

    /** أسئلة درس بأكمله — مع دعم الـ pagination */
    @GetMapping("/week/{weekId}")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public ResponseEntity<GlobalResponse<Page<BankQuestionResponse>>> getByWeek(
            @PathVariable Long weekId,
            @PageableDefault(size = 20, sort = "id") Pageable pageable) {
        return ResponseEntity.ok(
                GlobalResponse.success(bankService.getByWeek(weekId, pageable)));
    }

    /** كل أسئلة جزئية معينة — بدون pagination (للاستخدام الداخلي والمراجعة) */
    @GetMapping("/topic/{topicId}")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public ResponseEntity<GlobalResponse<List<BankQuestionResponse>>> getByTopic(
            @PathVariable Long topicId) {
        return ResponseEntity.ok(
                GlobalResponse.success(bankService.getByTopic(topicId)));
    }

    // ─── كتابة ───────────────────────────────────────────────────

    @PostMapping
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public ResponseEntity<GlobalResponse<BankQuestionResponse>> create(
            @Valid @RequestBody BankQuestionRequest request) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(GlobalResponse.success("تم إضافة السؤال للبنك", bankService.create(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public ResponseEntity<GlobalResponse<BankQuestionResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody BankQuestionRequest request) {
        return ResponseEntity.ok(
                GlobalResponse.success("تم تعديل السؤال", bankService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public ResponseEntity<GlobalResponse<Void>> delete(@PathVariable Long id) {
        bankService.delete(id);
        return ResponseEntity.ok(GlobalResponse.success("تم حذف السؤال من البنك", null));
    }
}
