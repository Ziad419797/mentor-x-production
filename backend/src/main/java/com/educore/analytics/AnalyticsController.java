package com.educore.analytics;

import com.educore.analytics.dto.*;
import com.educore.common.GlobalResponse;
import com.educore.security.JwtUserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Analytics & Dashboard API
 *
 * GET /api/analytics/admin/dashboard?period=WEEK    → لوحة الأدمن الشاملة
 * GET /api/analytics/teacher/dashboard              → لوحة المدرس
 * GET /api/analytics/students/map                   → خريطة الطلاب الجغرافية
 */
@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;
    private final TeacherAnalyticsService teacherAnalyticsService;
    private final StudentAnalyticsService studentAnalyticsService;

    // ─── Admin Dashboard ─────────────────────────────────────────

    /**
     * لوحة تحكم الأدمن الشاملة.
     * period: TODAY | WEEK | MONTH | YEAR (default: MONTH)
     */
    @GetMapping("/admin/dashboard")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<GlobalResponse<AdminDashboardStats>> adminDashboard(
            @RequestParam(defaultValue = "MONTH") AnalyticsService.Period period) {
        return ResponseEntity.ok(GlobalResponse.success(
                analyticsService.getAdminDashboard(period)));
    }

    // ─── Teacher Dashboard ───────────────────────────────────────

    @GetMapping("/teacher/dashboard")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public ResponseEntity<GlobalResponse<TeacherDashboardStats>> teacherDashboard() {
        return ResponseEntity.ok(GlobalResponse.success(
                analyticsService.getTeacherDashboard()));
    }

    // ─── خريطة الطلاب ────────────────────────────────────────────

    /**
     * قائمة كل الطلاب النشطين ببيانات موقعهم.
     * مخصصة للـ Frontend لبناء خريطة جغرافية (Google Maps / Leaflet).
     */
    @GetMapping("/students/map")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public ResponseEntity<GlobalResponse<List<StudentLocationDto>>> studentsMap() {
        return ResponseEntity.ok(GlobalResponse.success(
                "خريطة الطلاب", analyticsService.getStudentMap()));
    }

    // ─── Teacher Extended Endpoints ──────────────────────────────

    @GetMapping("/teacher/purchase-heatmap")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public ResponseEntity<GlobalResponse<List<HeatmapCellDto>>> purchaseHeatmap() {
        return ResponseEntity.ok(GlobalResponse.success(teacherAnalyticsService.getPurchaseHeatmap()));
    }

    @GetMapping("/teacher/sales-by-type")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public ResponseEntity<GlobalResponse<List<SalesTypeDto>>> salesByType() {
        return ResponseEntity.ok(GlobalResponse.success(teacherAnalyticsService.getSalesByType()));
    }

    @GetMapping("/teacher/login-heatmap")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public ResponseEntity<GlobalResponse<List<HeatmapCellDto>>> loginHeatmap() {
        return ResponseEntity.ok(GlobalResponse.success(teacherAnalyticsService.getLoginHeatmap()));
    }

    @GetMapping("/teacher/grades-by-course")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public ResponseEntity<GlobalResponse<List<GradesByCourseDto>>> gradesByCourse() {
        return ResponseEntity.ok(GlobalResponse.success(teacherAnalyticsService.getGradesByCourse()));
    }

    @GetMapping("/teacher/grades-by-center")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public ResponseEntity<GlobalResponse<List<GradesByCenterDto>>> gradesByCenter() {
        return ResponseEntity.ok(GlobalResponse.success(teacherAnalyticsService.getGradesByCenter()));
    }

    /** نقاط الضعف لكل topic — مرتبة تنازلياً بنسبة الأخطاء */
    @GetMapping("/teacher/topic-weakness")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public ResponseEntity<GlobalResponse<List<Map<String, Object>>>> topicWeakness() {
        return ResponseEntity.ok(GlobalResponse.success(teacherAnalyticsService.getTopicWeakness()));
    }

    @GetMapping("/teacher/hardest-quizzes")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public ResponseEntity<GlobalResponse<List<QuizPassRateDto>>> hardestQuizzes(
            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(GlobalResponse.success(teacherAnalyticsService.getHardestQuizzes(limit)));
    }

    @GetMapping("/teacher/platform-time")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public ResponseEntity<GlobalResponse<PlatformTimeDto>> platformTime() {
        return ResponseEntity.ok(GlobalResponse.success(teacherAnalyticsService.getPlatformTimeStats()));
    }

    // ─── Student Endpoints ───────────────────────────────────────
    // device validation مش محتاجة هنا — JwtAuthenticationFilter بيعملها
    // لكل STUDENT request قبل ما توصل للـ controller على الإطلاق

    @GetMapping("/student/grade-comparison")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<GlobalResponse<StudentGradeComparisonDto>> studentGradeComparison(
            @AuthenticationPrincipal JwtUserPrincipal principal,
            @RequestHeader(value = "X-Device-Id", required = false) String deviceId) {
        return ResponseEntity.ok(GlobalResponse.success(
                studentAnalyticsService.getGradeComparison(principal.getUserId())));
    }

    @GetMapping("/student/course-progress")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<GlobalResponse<List<ProgressSummaryDto>>> studentCourseProgress(
            @AuthenticationPrincipal JwtUserPrincipal principal,
            @RequestHeader(value = "X-Device-Id", required = false) String deviceId) {
        return ResponseEntity.ok(GlobalResponse.success(
                studentAnalyticsService.getCourseProgress(principal.getUserId())));
    }

    @GetMapping("/student/active-hours")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<GlobalResponse<List<ActiveHourDto>>> studentActiveHours(
            @AuthenticationPrincipal JwtUserPrincipal principal,
            @RequestHeader(value = "X-Device-Id", required = false) String deviceId) {
        return ResponseEntity.ok(GlobalResponse.success(
                studentAnalyticsService.getActiveHours(principal.getUserId())));
    }

    @GetMapping("/student/achievements")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<GlobalResponse<AchievementsDto>> studentAchievements(
            @AuthenticationPrincipal JwtUserPrincipal principal,
            @RequestHeader(value = "X-Device-Id", required = false) String deviceId) {
        return ResponseEntity.ok(GlobalResponse.success(
                studentAnalyticsService.getAchievements(principal.getUserId())));
    }

    @GetMapping("/student/streak")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<GlobalResponse<StreakDto>> studentStreak(
            @AuthenticationPrincipal JwtUserPrincipal principal,
            @RequestHeader(value = "X-Device-Id", required = false) String deviceId) {
        return ResponseEntity.ok(GlobalResponse.success(
                studentAnalyticsService.getStreak(principal.getUserId())));
    }

    @GetMapping("/student/quiz-speed")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<GlobalResponse<List<QuizSpeedDto>>> studentQuizSpeed(
            @AuthenticationPrincipal JwtUserPrincipal principal,
            @RequestHeader(value = "X-Device-Id", required = false) String deviceId) {
        return ResponseEntity.ok(GlobalResponse.success(
                studentAnalyticsService.getQuizSpeed(principal.getUserId())));
    }

    // ─── Teacher Extended — Enrollment Trends ──────────────────

    @GetMapping("/teacher/enrollment-trend")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public ResponseEntity<GlobalResponse<List<Map<String, Object>>>> enrollmentTrend(
            @RequestParam(defaultValue = "MONTH") AnalyticsService.Period period) {
        LocalDateTime from = period.getFrom();
        LocalDateTime to   = LocalDateTime.now();
        List<Object[]> raw = analyticsService.getEnrollmentTrend(from, to);
        List<Map<String, Object>> result = raw.stream()
                .map(r -> Map.<String,Object>of("date", r[0].toString(), "count", ((Number)r[1]).longValue()))
                .toList();
        return ResponseEntity.ok(GlobalResponse.success(result));
    }

    @GetMapping("/teacher/avg-progress-by-course")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public ResponseEntity<GlobalResponse<List<Map<String, Object>>>> avgProgressByCourse() {
        List<Object[]> raw = analyticsService.getAvgProgressByCourse();
        List<Map<String, Object>> result = raw.stream()
                .map(r -> Map.<String,Object>of(
                        "courseId", ((Number)r[0]).longValue(),
                        "courseTitle", r[1].toString(),
                        "avgProgress", r[2] != null ? Math.round(((Number)r[2]).doubleValue()*10.0)/10.0 : 0.0,
                        "enrollmentCount", ((Number)r[3]).longValue()))
                .toList();
        return ResponseEntity.ok(GlobalResponse.success(result));
    }

    // ─── Parent Endpoints ────────────────────────────────────────

    @GetMapping("/parent/child/{studentId}/grade-comparison")
    @PreAuthorize("hasRole('PARENT')")
    public ResponseEntity<GlobalResponse<StudentGradeComparisonDto>> parentChildGradeComparison(
            @AuthenticationPrincipal JwtUserPrincipal principal,
            @PathVariable Long studentId) {
        studentAnalyticsService.validateParentOwnsStudent(principal.getUserId(), studentId);
        return ResponseEntity.ok(GlobalResponse.success(
                studentAnalyticsService.getGradeComparison(studentId)));
    }

    @GetMapping("/parent/child/{studentId}/course-progress")
    @PreAuthorize("hasRole('PARENT')")
    public ResponseEntity<GlobalResponse<List<ProgressSummaryDto>>> parentChildCourseProgress(
            @AuthenticationPrincipal JwtUserPrincipal principal,
            @PathVariable Long studentId) {
        studentAnalyticsService.validateParentOwnsStudent(principal.getUserId(), studentId);
        return ResponseEntity.ok(GlobalResponse.success(
                studentAnalyticsService.getCourseProgress(studentId)));
    }

    @GetMapping("/parent/child/{studentId}/active-hours")
    @PreAuthorize("hasRole('PARENT')")
    public ResponseEntity<GlobalResponse<List<ActiveHourDto>>> parentChildActiveHours(
            @AuthenticationPrincipal JwtUserPrincipal principal,
            @PathVariable Long studentId) {
        studentAnalyticsService.validateParentOwnsStudent(principal.getUserId(), studentId);
        return ResponseEntity.ok(GlobalResponse.success(
                studentAnalyticsService.getActiveHours(studentId)));
    }

    @GetMapping("/parent/child/{studentId}/achievements")
    @PreAuthorize("hasRole('PARENT')")
    public ResponseEntity<GlobalResponse<AchievementsDto>> parentChildAchievements(
            @AuthenticationPrincipal JwtUserPrincipal principal,
            @PathVariable Long studentId) {
        studentAnalyticsService.validateParentOwnsStudent(principal.getUserId(), studentId);
        return ResponseEntity.ok(GlobalResponse.success(
                studentAnalyticsService.getAchievements(studentId)));
    }

    @GetMapping("/parent/child/{studentId}/streak")
    @PreAuthorize("hasRole('PARENT')")
    public ResponseEntity<GlobalResponse<StreakDto>> parentChildStreak(
            @AuthenticationPrincipal JwtUserPrincipal principal,
            @PathVariable Long studentId) {
        studentAnalyticsService.validateParentOwnsStudent(principal.getUserId(), studentId);
        return ResponseEntity.ok(GlobalResponse.success(
                studentAnalyticsService.getStreak(studentId)));
    }

    @GetMapping("/parent/child/{studentId}/quiz-speed")
    @PreAuthorize("hasRole('PARENT')")
    public ResponseEntity<GlobalResponse<List<QuizSpeedDto>>> parentChildQuizSpeed(
            @AuthenticationPrincipal JwtUserPrincipal principal,
            @PathVariable Long studentId) {
        studentAnalyticsService.validateParentOwnsStudent(principal.getUserId(), studentId);
        return ResponseEntity.ok(GlobalResponse.success(
                studentAnalyticsService.getQuizSpeed(studentId)));
    }
}
