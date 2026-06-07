package com.educore.analytics.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * لوحة تحكم المدرس — يرى إحصائيات الطلاب وأداء الكويزات
 */
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class TeacherDashboardStats {

    // ── الطلاب ───────────────────────────────────────
    private long totalActiveStudents;
    private long pendingStudents;
    private long newStudentsThisWeek;

    // ── الكويزات ─────────────────────────────────────
    private long totalQuizAttempts;
    private long submittedAttempts;
    private double globalPassRate;
    /** أفضل 10 طلاب على مستوى النظام */
    private List<TopStudentDto> topStudents;

    // ── الواجبات ─────────────────────────────────────
    private long totalAssignmentSubmissions;

    // ── الاشتراكات ───────────────────────────────────
    private long totalEnrollments;
    private long activeEnrollments;
    private long completedEnrollments;
    /** اشتراكات جديدة هذا الأسبوع */
    private long newEnrollmentsThisWeek;

    // ── أفضل الكورسات ────────────────────────────────
    /** أكثر 5 كورسات اشتراكاً */
    private List<AdminDashboardStats.CourseStatDto> topCourses;

    // ── التوزيع الجغرافي ─────────────────────────────
    private List<AdminDashboardStats.GeoStatDto> studentsByGovernorate;

    // ── الخريطة الكاملة للطلاب ───────────────────────
    private List<StudentLocationDto> studentsMap;

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class TopStudentDto {
        private Long   studentId;
        private String studentName;
        private String studentCode;
        private double avgPercentage;
        private long   totalAttempts;
        private long   totalScore;
    }
}
