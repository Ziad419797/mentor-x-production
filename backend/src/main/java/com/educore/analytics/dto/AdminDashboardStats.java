package com.educore.analytics.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * إحصائيات لوحة تحكم الأدمن الشاملة.
 * يمكن طلب نطاق زمني: TODAY / WEEK / MONTH / YEAR / ALL
 */
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class AdminDashboardStats {

    // ── إحصائيات الطلاب ─────────────────────────────
    private long totalStudents;
    private long activeStudents;
    private long pendingStudents;
    private long newStudentsInPeriod;

    // ── الإيرادات ────────────────────────────────────
    private BigDecimal totalRevenue;
    private BigDecimal revenueInPeriod;
    /** الإيرادات بكل طريقة دفع: ONLINE_CARD, CASH, WALLET... */
    private Map<String, BigDecimal> revenueByMethod;

    // ── الاشتراكات ───────────────────────────────────
    private long totalEnrollments;
    private long activeEnrollments;
    /** أكثر 5 كورسات اشتراكاً */
    private List<CourseStatDto> topCourses;

    // ── الكويزات ─────────────────────────────────────
    private long totalQuizAttempts;
    private double globalPassRate;       // نسبة النجاح كلها

    // ── المحافظ ──────────────────────────────────────
    private BigDecimal totalWalletDeposits;
    private BigDecimal walletDepositsInPeriod;

    // ── الكوبونات ────────────────────────────────────
    private long totalCoupons;
    private long activeCoupons;
    private long couponRedemptionsInPeriod;

    // ── التوزيع الجغرافي ─────────────────────────────
    private List<GeoStatDto> studentsByGovernorate;

    // ── خريطة الطلاب ─────────────────────────────────
    private List<StudentLocationDto> studentsMap;

    // ─── Inner DTOs ───────────────────────────────────

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class CourseStatDto {
        private Long   courseId;
        private String courseTitle;
        private long   enrollments;
        private double avgProgress;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class GeoStatDto {
        private String governorate;
        private long   studentCount;
    }
}
