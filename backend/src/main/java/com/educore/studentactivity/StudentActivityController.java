package com.educore.studentactivity;

import com.educore.common.GlobalResponse;
import com.educore.parent.ParentRepository;
import com.educore.security.JwtUserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@Tag(name = "Student Activity Log", description = "سجل نشاط الطالب")
public class StudentActivityController {

    private final StudentActivityLogService activityService;
    private final ParentRepository          parentRepository;

    // ─────────────────────────────────────────────────────────────
    // Teacher / Admin — GET /api/teacher/students/{id}/activity
    // ─────────────────────────────────────────────────────────────

    @Operation(summary = "سجل نشاط طالب — للمدرس والأدمن")
    @GetMapping("/api/teacher/students/{studentId}/activity")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN','STAFF')")
    public ResponseEntity<GlobalResponse<Page<StudentActivityLog>>> getForTeacher(
            @PathVariable Long studentId,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {

        Page<StudentActivityLog> logs = activityService.getByStudent(
                studentId, PageRequest.of(page, size, Sort.by("createdAt").descending()));
        return ResponseEntity.ok(GlobalResponse.success("تم جلب سجل النشاط", logs));
    }

    // ─────────────────────────────────────────────────────────────
    // Parent — GET /api/parent/children/{id}/activity
    // ─────────────────────────────────────────────────────────────

    @Operation(summary = "سجل نشاط الابن — لولي الأمر")
    @GetMapping("/api/parent/children/{studentId}/activity")
    @PreAuthorize("hasRole('PARENT')")
    public ResponseEntity<GlobalResponse<Page<StudentActivityLog>>> getForParent(
            @PathVariable Long studentId,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal JwtUserPrincipal principal) {

        // تحقق إن الطالب ده ابن ولي الأمر ده
        boolean isChild = parentRepository.findById(principal.getUserId())
                .map(p -> p.getStudents().stream().anyMatch(s -> s.getId().equals(studentId)))
                .orElse(false);

        if (!isChild) {
            return ResponseEntity.status(403).body(
                    GlobalResponse.<Page<StudentActivityLog>>builder()
                            .success(false).message("هذا الطالب غير مرتبط بحسابك").build());
        }

        Page<StudentActivityLog> logs = activityService.getByStudent(
                studentId, PageRequest.of(page, size, Sort.by("createdAt").descending()));
        return ResponseEntity.ok(GlobalResponse.success("تم جلب سجل النشاط", logs));
    }
}
