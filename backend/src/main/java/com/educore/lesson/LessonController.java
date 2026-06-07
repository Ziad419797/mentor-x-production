package com.educore.lesson;

import com.educore.common.GlobalResponse;
import com.educore.dtocourse.request.LessonUpdateRequest;
import com.educore.dtocourse.request.WeekCreateRequest;
import com.educore.dtocourse.response.WeekResponse;
import com.educore.dtocourse.response.WeekSummaryResponse;
import com.educore.enrollment.AccessService;
import com.educore.lessongate.LessonGateService;
import com.educore.lessongate.LessonProgressStatus;
import com.educore.lessongate.StudentLessonProgress;
import com.educore.security.JwtUserPrincipal;
import com.educore.lesson.ReorderRequest;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/weeks")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Weeks", description = "إدارة الأسابيع داخل السيشنات")
public class LessonController {

    private final LessonService     lessonService;
    private final LessonGateService lessonGateService;
    private final AccessService     accessService;

    // ================= CREATE =================

    @Operation(summary = "إضافة أسبوع")
    @PostMapping
    public ResponseEntity<WeekResponse> createWeek(
            @Validated @RequestBody WeekCreateRequest request) {

        log.info("API [POST] /api/weeks");

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(lessonService.createLesson(request));
    }

    // ================= UPDATE =================

    @PutMapping("/{id}")
    public ResponseEntity<WeekResponse> updateWeek(
            @PathVariable Long id,
            @Validated @RequestBody LessonUpdateRequest request) {

        log.info("API [PUT] /api/weeks/{}", id);

        return ResponseEntity.ok(
                lessonService.updateLesson(id, request));
    }

    // ================= DELETE =================

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteWeek(@PathVariable Long id) {

        log.info("API [DELETE] /api/weeks/{}", id);

        lessonService.deleteLesson(id);
        return ResponseEntity.noContent().build();
    }

    // ================= GET BY ID =================

    @GetMapping("/{id}")
    public ResponseEntity<WeekResponse> getWeekById(@PathVariable Long id) {

        log.info("API [GET] /api/weeks/{}", id);

        return ResponseEntity.ok(
                lessonService.getLessonById(id));
    }

    // ================= GET ALL =================

    @GetMapping
    public ResponseEntity<Page<WeekResponse>> getAllWeeks(
            Pageable pageable) {

        log.info("API [GET] /api/weeks");

        return ResponseEntity.ok(
                lessonService.getAllLessons(pageable));
    }

    // ================= GET BY SESSION =================

    /**
     * للطالب: يتحقق إن الطالب مشترك في الكورس قبل ما يجيب الـ weeks.
     * للمدرس/الأدمن: يرجع كل الـ weeks بدون قيود.
     */
    @GetMapping("/session/{sessionId}")
    public ResponseEntity<Page<WeekSummaryResponse>> getWeeksBySession(
            @PathVariable Long sessionId,
            Pageable pageable,
            @AuthenticationPrincipal JwtUserPrincipal principal
    ) {

        log.info("API [GET] /api/weeks/session/{}", sessionId);

        return ResponseEntity.ok(
                lessonService.getLessonsBySession(sessionId, pageable));
    }
    // LessonController.java

    @Operation(summary = "Toggle lesson activation status", description = "إظهار أو إخفاء الدرس للطلاب")
    @PatchMapping("/{id}/toggle-status")
    public ResponseEntity<GlobalResponse<Void>> toggleStatus(@PathVariable Long id) {
        log.info("PATCH /api/lessons/{}/toggle-status", id);
        lessonService.toggleLessonStatus(id);
        return ResponseEntity.ok(GlobalResponse.<Void>builder()
                .success(true)
                .message("تم تغيير حالة الدرس بنجاح")
                .build());
    }

    // ─────────────────────────────────────────────────────────────
    // Phase 1: Lesson Gate endpoints
    // ─────────────────────────────────────────────────────────────

    @Operation(
        summary     = "[STUDENT] هل أقدر أدخل هذه الحصة؟",
        description = "يرجع حالة الحصة للطالب — LOCKED / UNLOCKED / IN_PROGRESS / COMPLETED"
    )
    @GetMapping("/{id}/status")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<GlobalResponse<Map<String, Object>>> getLessonStatus(
            @PathVariable Long id,
            @AuthenticationPrincipal JwtUserPrincipal principal) {

        LessonProgressStatus status = lessonGateService.getLessonStatus(principal.getUserId(), id);
        boolean canAccess = lessonGateService.canAccessLesson(principal.getUserId(), id);

        return ResponseEntity.ok(GlobalResponse.<Map<String, Object>>builder()
                .success(true)
                .message("تم جلب حالة الحصة")
                .data(Map.of(
                        "weekId",    id,
                        "status",    status.name(),
                        "canAccess", canAccess
                ))
                .build());
    }

    @Operation(
        summary     = "[STUDENT] إكمال الحصة بعد الكويز",
        description = "يُستدعى بعد تسليم الكويز — يسجل الإكمال ويفتح الحصة التالية لو النتيجة مرضية"
    )
    @PostMapping("/{id}/complete")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<GlobalResponse<Map<String, Object>>> completeLesson(
            @PathVariable Long id,
            @RequestParam double quizScore,
            @RequestParam(defaultValue = "50.0") double passingScore,
            @AuthenticationPrincipal JwtUserPrincipal principal) {

        boolean passed = quizScore >= passingScore;
        lessonGateService.completeLesson(principal.getUserId(), id, quizScore, passed);

        return ResponseEntity.ok(GlobalResponse.<Map<String, Object>>builder()
                .success(true)
                .message(passed ? "أحسنت! تم فتح الحصة التالية" : "درجتك أقل من المطلوب — راجع الحصة وحاول تاني")
                .data(Map.of(
                        "weekId",     id,
                        "quizScore",  quizScore,
                        "passed",     passed,
                        "nextUnlocked", passed
                ))
                .build());
    }

    // ─────────────────────────────────────────────────────────────
    // Reorder endpoints — Drag & Drop
    // ─────────────────────────────────────────────────────────────

    @Operation(
        summary     = "[TEACHER] إعادة ترتيب الفيديوهات والملفات داخل حصة",
        description = "يُستدعى بعد Drag & Drop — يحفظ الترتيب الجديد للمواد"
    )
    @PutMapping("/{weekId}/reorder-materials")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public ResponseEntity<GlobalResponse<Void>> reorderMaterials(
            @PathVariable Long weekId,
            @Valid @RequestBody ReorderRequest request) {
        lessonService.reorderMaterials(weekId, request);
        return ResponseEntity.ok(GlobalResponse.<Void>builder()
                .success(true).message("تم حفظ ترتيب المواد بنجاح").build());
    }

    @Operation(
        summary     = "[TEACHER] إعادة ترتيب الكويزات داخل حصة",
        description = "يُستدعى بعد Drag & Drop — يحفظ الترتيب الجديد للكويزات"
    )
    @PutMapping("/{weekId}/reorder-quizzes")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public ResponseEntity<GlobalResponse<Void>> reorderQuizzes(
            @PathVariable Long weekId,
            @Valid @RequestBody ReorderRequest request) {
        lessonService.reorderQuizzes(weekId, request);
        return ResponseEntity.ok(GlobalResponse.<Void>builder()
                .success(true).message("تم حفظ ترتيب الكويزات بنجاح").build());
    }

    @Operation(
        summary     = "[TEACHER] إعادة ترتيب الواجبات داخل حصة",
        description = "يُستدعى بعد Drag & Drop — يحفظ الترتيب الجديد للواجبات"
    )
    @PutMapping("/{weekId}/reorder-assignments")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public ResponseEntity<GlobalResponse<Void>> reorderAssignments(
            @PathVariable Long weekId,
            @Valid @RequestBody ReorderRequest request) {
        lessonService.reorderAssignments(weekId, request);
        return ResponseEntity.ok(GlobalResponse.<Void>builder()
                .success(true).message("تم حفظ ترتيب الواجبات بنجاح").build());
    }

    @Operation(
        summary     = "[TEACHER] تفعيل/إلغاء الوصول بالترتيب لحصة",
        description = "لو مفعّل، الطالب لازم يخلص كل عنصر قبل ما يفتح اللي بعده"
    )
    @PatchMapping("/{weekId}/toggle-sequential")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public ResponseEntity<GlobalResponse<Void>> toggleSequentialAccess(
            @PathVariable Long weekId) {
        lessonService.toggleSequentialAccess(weekId);
        return ResponseEntity.ok(GlobalResponse.<Void>builder()
                .success(true).message("تم تغيير إعداد الترتيب بنجاح").build());
    }

    @Operation(summary = "[STUDENT] تقدمي في Session معينة")
    @GetMapping("/session/{sessionId}/my-progress")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<GlobalResponse<List<StudentLessonProgress>>> getMyProgress(
            @PathVariable Long sessionId,
            @AuthenticationPrincipal JwtUserPrincipal principal) {

        List<StudentLessonProgress> progress =
                lessonGateService.getStudentProgressInSession(principal.getUserId(), sessionId);

        return ResponseEntity.ok(GlobalResponse.<List<StudentLessonProgress>>builder()
                .success(true)
                .message("تم جلب التقدم")
                .data(progress)
                .build());
    }
}
