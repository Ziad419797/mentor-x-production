package com.educore.analytics;

import com.educore.analytics.dto.AdminDashboardStats;
import com.educore.analytics.dto.StudentLocationDto;
import com.educore.analytics.dto.TeacherDashboardStats;
import com.educore.coupon.CouponRepository;
import com.educore.enrollment.EnrollmentRepository;
import com.educore.payment.payment.PaymentRepository;
import com.educore.assignment.StudentAssignmentAttemptRepository;
import com.educore.quiz.StudentQuizAttemptRepository;
import com.educore.student.Student;
import com.educore.student.StudentRepository;
import com.educore.student.StudentStatus;
import com.educore.wallet.WalletTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final StudentRepository                studentRepository;
    private final EnrollmentRepository             enrollmentRepository;
    private final PaymentRepository                paymentRepository;
    private final StudentQuizAttemptRepository     attemptRepository;
    private final StudentAssignmentAttemptRepository assignmentAttemptRepository;
    private final WalletTransactionRepository      walletTxRepository;
    private final CouponRepository                 couponRepository;

    // ─── نطاقات الوقت ────────────────────────────────────────────

    public enum Period {
        TODAY, WEEK, MONTH, YEAR;

        public LocalDateTime getFrom() {
            return switch (this) {
                case TODAY -> LocalDateTime.now().toLocalDate().atStartOfDay();
                case WEEK  -> LocalDateTime.now().minusWeeks(1);
                case MONTH -> LocalDateTime.now().minusMonths(1);
                case YEAR  -> LocalDateTime.now().minusYears(1);
            };
        }
    }

    // ─── Admin Dashboard ─────────────────────────────────────────

    @Transactional(readOnly = true)
    public AdminDashboardStats getAdminDashboard(Period period) {
        LocalDateTime from = period.getFrom();
        LocalDateTime to   = LocalDateTime.now();

        // ── الطلاب ──────────────────────────────────────────────
        long totalStudents   = studentRepository.count();
        long activeStudents  = studentRepository.countByStatus(StudentStatus.ACTIVE);
        long pendingStudents = studentRepository.countByStatus(StudentStatus.PENDING);
        long newStudents     = studentRepository.countNewStudents(from, to);

        // ── الإيرادات ────────────────────────────────────────────
        BigDecimal totalRevenue    = paymentRepository.getTotalRevenue();
        BigDecimal revenueInPeriod = paymentRepository.getRevenueInPeriod(from, to);
        Map<String, BigDecimal> revenueByMethod = buildRevenueByMethod(from, to);

        // ── الاشتراكات ───────────────────────────────────────────
        long totalEnrollments  = enrollmentRepository.count();
        long activeEnrollments = enrollmentRepository.countByActiveTrue();
        List<AdminDashboardStats.CourseStatDto> topCourses = buildTopCourses();

        // ── الكويزات ─────────────────────────────────────────────
        long totalAttempts = attemptRepository.count();
        double passRate    = computeGlobalPassRate();

        // ── المحافظ ──────────────────────────────────────────────
        BigDecimal totalDeposits     = walletTxRepository.sumDepositsInPeriod(
                LocalDateTime.of(2000, 1, 1, 0, 0), to);
        BigDecimal depositsInPeriod  = walletTxRepository.sumDepositsInPeriod(from, to);

        // ── الكوبونات ────────────────────────────────────────────
        long totalCoupons  = couponRepository.count();
        long activeCoupons = couponRepository.findValidCoupons().size();
        long couponRedemptions = couponRepository.countRedemptionsInPeriod(from, to);

        // ── الجغرافيا ────────────────────────────────────────────
        List<AdminDashboardStats.GeoStatDto> geoStats = buildGeoStats();

        // ── خريطة الطلاب ─────────────────────────────────────────
        List<StudentLocationDto> map = buildStudentMap();

        return AdminDashboardStats.builder()
                .totalStudents(totalStudents)
                .activeStudents(activeStudents)
                .pendingStudents(pendingStudents)
                .newStudentsInPeriod(newStudents)
                .totalRevenue(totalRevenue)
                .revenueInPeriod(revenueInPeriod)
                .revenueByMethod(revenueByMethod)
                .totalEnrollments(totalEnrollments)
                .activeEnrollments(activeEnrollments)
                .topCourses(topCourses)
                .totalQuizAttempts(totalAttempts)
                .globalPassRate(passRate)
                .totalWalletDeposits(totalDeposits)
                .walletDepositsInPeriod(depositsInPeriod)
                .totalCoupons(totalCoupons)
                .activeCoupons(activeCoupons)
                .couponRedemptionsInPeriod(couponRedemptions)
                .studentsByGovernorate(geoStats)
                .studentsMap(map)
                .build();
    }

    // ─── Teacher Dashboard ───────────────────────────────────────

    @Transactional(readOnly = true)
    public TeacherDashboardStats getTeacherDashboard() {
        LocalDateTime weekAgo = LocalDateTime.now().minusWeeks(1);
        LocalDateTime now     = LocalDateTime.now();

        // ── الطلاب ────────────────────────────────────────────
        long totalActive    = studentRepository.countByStatus(StudentStatus.ACTIVE);
        long pending        = studentRepository.countByStatus(StudentStatus.PENDING);
        long newThisWeek    = studentRepository.countNewStudents(weekAgo, now);

        // ── الكويزات ──────────────────────────────────────────
        long totalAttempts     = attemptRepository.count();
        long submittedAttempts = attemptRepository.countBySubmittedTrue();
        double passRate        = computeGlobalPassRate();

        // ── الواجبات ──────────────────────────────────────────
        long totalAssignmentSubmissions = assignmentAttemptRepository.countBySubmittedTrue();

        // ── الاشتراكات ────────────────────────────────────────
        long totalEnrollments    = enrollmentRepository.count();
        long activeEnrollments   = enrollmentRepository.countByActiveTrue();
        long newEnrollmentsThisWeek = enrollmentRepository.countNewEnrollmentsInPeriod(weekAgo, now);
        // Count completed enrollments globally from course completion stats
        long completedCount = enrollmentRepository.getCompletedCountByCourse().stream()
                .mapToLong(row -> ((Number) row[2]).longValue()).sum();

        // ── أفضل الكورسات ─────────────────────────────────────
        var topCourses = enrollmentRepository.findTopCoursesByEnrollment(
                org.springframework.data.domain.PageRequest.of(0, 5)).stream()
                .map(row -> AdminDashboardStats.CourseStatDto.builder()
                        .courseId(((Number) row[0]).longValue())
                        .courseTitle(row[1].toString())
                        .enrollments(((Number) row[2]).longValue())
                        .avgProgress(row[3] != null ? ((Number) row[3]).doubleValue() : 0.0)
                        .build())
                .toList();

        List<TeacherDashboardStats.TopStudentDto> topStudents = buildTopStudents();
        List<AdminDashboardStats.GeoStatDto> geoStats = buildGeoStats();
        List<StudentLocationDto> map = buildStudentMap();

        return TeacherDashboardStats.builder()
                .totalActiveStudents(totalActive)
                .pendingStudents(pending)
                .newStudentsThisWeek(newThisWeek)
                .totalQuizAttempts(totalAttempts)
                .submittedAttempts(submittedAttempts)
                .globalPassRate(passRate)
                .totalAssignmentSubmissions(totalAssignmentSubmissions)
                .totalEnrollments(totalEnrollments)
                .activeEnrollments(activeEnrollments)
                .newEnrollmentsThisWeek(newEnrollmentsThisWeek)
                .completedEnrollments(completedCount)
                .topCourses(topCourses)
                .topStudents(topStudents)
                .studentsByGovernorate(geoStats)
                .studentsMap(map)
                .build();
    }

    // ─── خريطة الطلاب فقط ────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<StudentLocationDto> getStudentMap() {
        return buildStudentMap();
    }

    // ─── Helpers ─────────────────────────────────────────────────

    private Map<String, BigDecimal> buildRevenueByMethod(LocalDateTime from, LocalDateTime to) {
        Map<String, BigDecimal> map = new HashMap<>();
        paymentRepository.getRevenueStatsByMethod(from, to).forEach(row -> {
            String method = row[0].toString();
            BigDecimal sum = row[2] != null ? (BigDecimal) row[2] : BigDecimal.ZERO;
            map.put(method, sum);
        });
        return map;
    }

    private List<AdminDashboardStats.CourseStatDto> buildTopCourses() {
        return enrollmentRepository
                .findTopCoursesByEnrollment(PageRequest.of(0, 5))
                .stream()
                .map(row -> AdminDashboardStats.CourseStatDto.builder()
                        .courseId(((Number) row[0]).longValue())
                        .courseTitle(row[1].toString())
                        .enrollments(((Number) row[2]).longValue())
                        .avgProgress(row[3] != null ? ((Number) row[3]).doubleValue() : 0.0)
                        .build())
                .toList();
    }

    private double computeGlobalPassRate() {
        long submitted = attemptRepository.countBySubmittedTrue();
        if (submitted == 0) return 0.0;
        // pass = score > 0 (a proxy — full pass logic needs threshold per quiz)
        long passed = attemptRepository.countBySubmittedTrueAndScoreGreaterThan(0);
        return Math.round((double) passed / submitted * 1000.0) / 10.0; // 1 decimal
    }

    private List<AdminDashboardStats.GeoStatDto> buildGeoStats() {
        return studentRepository.countActiveByGovernorate().stream()
                .map(row -> AdminDashboardStats.GeoStatDto.builder()
                        .governorate(row[0] != null ? row[0].toString() : "غير محدد")
                        .studentCount(((Number) row[1]).longValue())
                        .build())
                .toList();
    }

    private List<StudentLocationDto> buildStudentMap() {
        return studentRepository.findAllActiveForMap().stream()
                .map(this::toLocationDto)
                .toList();
    }

    private List<TeacherDashboardStats.TopStudentDto> buildTopStudents() {
        return attemptRepository.findTopStudentsGlobally(PageRequest.of(0, 10)).stream()
                .map(row -> {
                    Long   sid        = ((Number) row[0]).longValue();
                    long   totalScore = ((Number) row[1]).longValue();
                    double avgPct     = row[2] != null ? ((Number) row[2]).doubleValue() : 0.0;
                    long   attempts   = ((Number) row[3]).longValue();

                    // جيب اسم الطالب
                    String name = studentRepository.findById(sid)
                            .map(Student::getFullName)
                            .orElse("Unknown");
                    String code = studentRepository.findById(sid)
                            .map(Student::getStudentCode)
                            .orElse("-");

                    return TeacherDashboardStats.TopStudentDto.builder()
                            .studentId(sid)
                            .studentName(name)
                            .studentCode(code)
                            .avgPercentage(Math.round(avgPct * 10.0) / 10.0)
                            .totalAttempts(attempts)
                            .totalScore(totalScore)
                            .build();
                })
                .toList();
    }

    public List<Object[]> getEnrollmentTrend(LocalDateTime from, LocalDateTime to) {
        return enrollmentRepository.getDailyEnrollmentTrend(from, to);
    }

    public List<Object[]> getAvgProgressByCourse() {
        return enrollmentRepository.getAvgProgressByCourse();
    }

    private StudentLocationDto toLocationDto(Student s) {
        return StudentLocationDto.builder()
                .studentId(s.getId())
                .studentName(s.getFullName())
                .studentCode(s.getStudentCode())
                .phone(s.getPhone())
                .governorate(s.getGovernorate())
                .area(s.getArea())
                .mapAddress(s.getMapAddress())
                .latitude(s.getLatitude())
                .longitude(s.getLongitude())
                .grade(s.getGrade())
                .online(s.getOnline())
                .centerName(s.getCenterName())
                .build();
    }
}
