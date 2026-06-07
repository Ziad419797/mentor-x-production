package com.educore.questionbank;

import com.educore.common.GlobalResponse;
import com.educore.questionbank.dto.QuestionTopicRequest;
import com.educore.questionbank.dto.QuestionTopicResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * CRUD للجزئيات (QuestionTopic) — شجرة المحتوى داخل الدرس.
 *
 * GET  /api/question-bank/topics/session/{sessionId}       → شجرة الجزئيات
 * GET  /api/question-bank/topics/session/{sessionId}/flat  → قائمة مسطّحة
 * POST /api/question-bank/topics                     → إضافة جزئية
 * PUT  /api/question-bank/topics/{id}                → تعديل جزئية
 * DELETE /api/question-bank/topics/{id}              → حذف ناعم
 */
@RestController
@RequestMapping("/api/question-bank/topics")
@RequiredArgsConstructor
public class QuestionTopicController {

    private final QuestionTopicService topicService;

    // ─── قراءة ───────────────────────────────────────────────────

    /** شجرة الجزئيات مع الجزئيات الفرعية nested */
    @GetMapping("/session/{sessionId}")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN','STUDENT')")
    public ResponseEntity<GlobalResponse<List<QuestionTopicResponse>>> getTree(
            @PathVariable Long sessionId) {
        return ResponseEntity.ok(
                GlobalResponse.success("شجرة الجزئيات", topicService.getTopicTree(sessionId)));
    }

    /** قائمة مسطّحة — مفيدة في الـ dropdowns */
    @GetMapping("/session/{sessionId}/flat")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN','STUDENT')")
    public ResponseEntity<GlobalResponse<List<QuestionTopicResponse>>> getFlat(
            @PathVariable Long sessionId) {
        return ResponseEntity.ok(
                GlobalResponse.success("الجزئيات", topicService.getFlatTopics(sessionId)));
    }

    // ─── كتابة ───────────────────────────────────────────────────

    @PostMapping
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public ResponseEntity<GlobalResponse<QuestionTopicResponse>> create(
            @Valid @RequestBody QuestionTopicRequest request) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(GlobalResponse.success("تم إنشاء الجزئية", topicService.create(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public ResponseEntity<GlobalResponse<QuestionTopicResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody QuestionTopicRequest request) {
        return ResponseEntity.ok(
                GlobalResponse.success("تم تعديل الجزئية", topicService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public ResponseEntity<GlobalResponse<Void>> delete(@PathVariable Long id) {
        topicService.delete(id);
        return ResponseEntity.ok(GlobalResponse.success("تم حذف الجزئية", null));
    }

    /** All topics flat list — for admin dropdowns (no sessionId required) */
    @GetMapping("/all")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public ResponseEntity<GlobalResponse<List<QuestionTopicResponse>>> getAll() {
        return ResponseEntity.ok(
                GlobalResponse.success("topics", topicService.getAllTopics()));
    }

}
