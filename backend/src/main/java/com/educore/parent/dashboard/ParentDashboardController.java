package com.educore.parent.dashboard;

import com.educore.attendance.AttendanceResponse;
import com.educore.common.GlobalResponse;
import com.educore.security.JwtUserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/parent/dashboard")
@RequiredArgsConstructor
@PreAuthorize("hasRole('PARENT')")
@Tag(name = "Parent Dashboard", description = "لوحة تحكم ولي الأمر — متابعة أبنائه")
public class ParentDashboardController {

    private final ParentDashboardService dashboardService;

    // ─────────────────────────────────────────────────────────────
    // الملخص الرئيسي — كل الأبناء + بادج الإشعارات
    // ─────────────────────────────────────────────────────────────

    @Operation(summary = "ملخص Dashboard ولي الأمر — قائمة أبنائه وإجماليات سريعة")
    @GetMapping("/summary")
    public ResponseEntity<GlobalResponse<ParentDashboardSummary>> getSummary(
            @AuthenticationPrincipal JwtUserPrincipal principal) {

        ParentDashboardSummary summary = dashboardService.getSummary(principal.getUserId());

        return ResponseEntity.ok(GlobalResponse.<ParentDashboardSummary>builder()
                .success(true)
                .message("تم جلب ملخص الـ Dashboard")
                .data(summary)
                .build());
    }

    // ─────────────────────────────────────────────────────────────
    // نظرة عامة تفصيلية على ابن محدد
    // ─────────────────────────────────────────────────────────────

    @Operation(summary = "نظرة عامة تفصيلية على ابن محدد (حضور + تقدم + اشتراكات)")
    @GetMapping("/child/{studentId}/overview")
    public ResponseEntity<GlobalResponse<ChildOverview>> getChildOverview(
            @PathVariable Long studentId,
            @AuthenticationPrincipal JwtUserPrincipal principal) {

        ChildOverview overview = dashboardService.getChildOverview(
                principal.getUserId(), studentId);

        return ResponseEntity.ok(GlobalResponse.<ChildOverview>builder()
                .success(true)
                .message("تم جلب بيانات الطالب")
                .data(overview)
                .build());
    }

    // ─────────────────────────────────────────────────────────────
    // سجل الحضور
    // ─────────────────────────────────────────────────────────────

    @Operation(summary = "سجل حضور ابن محدد (مرقّم صفحات)")
    @GetMapping("/child/{studentId}/attendance")
    public ResponseEntity<GlobalResponse<Page<AttendanceResponse>>> getChildAttendance(
            @PathVariable Long studentId,
            @AuthenticationPrincipal JwtUserPrincipal principal,
            @PageableDefault(size = 20, sort = "attendedAt", direction = Sort.Direction.DESC)
            Pageable pageable) {

        Page<AttendanceResponse> page = dashboardService.getChildAttendance(
                principal.getUserId(), studentId, pageable);

        return ResponseEntity.ok(GlobalResponse.<Page<AttendanceResponse>>builder()
                .success(true)
                .message("تم جلب سجل الحضور")
                .data(page)
                .build());
    }

    // ─────────────────────────────────────────────────────────────
    // الكورسات المشترك فيها
    // ─────────────────────────────────────────────────────────────

    @Operation(summary = "كورسات (اشتراكات) ابن محدد")
    @GetMapping("/child/{studentId}/enrollments")
    public ResponseEntity<GlobalResponse<List<ChildEnrollmentInfo>>> getChildEnrollments(
            @PathVariable Long studentId,
            @AuthenticationPrincipal JwtUserPrincipal principal) {

        List<ChildEnrollmentInfo> enrollments = dashboardService.getChildEnrollments(
                principal.getUserId(), studentId);

        return ResponseEntity.ok(GlobalResponse.<List<ChildEnrollmentInfo>>builder()
                .success(true)
                .message("تم جلب الاشتراكات")
                .data(enrollments)
                .build());
    }

    // ─────────────────────────────────────────────────────────────
    // تقدم الحصص في سيشن معين
    // ─────────────────────────────────────────────────────────────

    @Operation(summary = "تقدم ابن محدد في سيشن معين (حصة حصة)")
    @GetMapping("/child/{studentId}/progress/{sessionId}")
    public ResponseEntity<GlobalResponse<List<ChildProgressInfo>>> getChildProgress(
            @PathVariable Long studentId,
            @PathVariable Long sessionId,
            @AuthenticationPrincipal JwtUserPrincipal principal) {

        List<ChildProgressInfo> progress = dashboardService.getChildProgress(
                principal.getUserId(), studentId, sessionId);

        return ResponseEntity.ok(GlobalResponse.<List<ChildProgressInfo>>builder()
                .success(true)
                .message("تم جلب تقدم الطالب")
                .data(progress)
                .build());
    }
}
