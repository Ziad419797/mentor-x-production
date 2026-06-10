package com.educore.analytics;

import com.educore.enrollment.Enrollment;
import com.educore.enrollment.EnrollmentRepository;
import com.educore.enrollment.EnrollmentStatus;
import com.educore.payment.order.Order;
import com.educore.payment.order.OrderItem;
import com.educore.quiz.StudentQuizAttempt;
import com.educore.session.UserSession;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class AnalyticsRepository {

    @PersistenceContext
    private EntityManager em;

    // ─────────────────────────────────────────────────────────────
    // TEACHER ANALYTICS
    // ─────────────────────────────────────────────────────────────

    /** Governorate → student count */
    public List<Object[]> studentCountByGovernorate() {
        return em.createQuery(
            "SELECT s.governorate, COUNT(s) FROM Student s " +
            "WHERE s.governorate IS NOT NULL AND s.status = 'ACTIVE' " +
            "GROUP BY s.governorate ORDER BY COUNT(s) DESC", Object[].class)
            .getResultList();
    }

    /** Hour of day (0-23) → order count */
    @SuppressWarnings("unchecked")
    public List<Object[]> purchaseCountByHour() {
        return em.createNativeQuery(
            "SELECT EXTRACT(HOUR FROM created_at)::int AS hr, COUNT(*) FROM orders " +
            "WHERE status='COMPLETED' GROUP BY hr ORDER BY hr")
            .getResultList();
    }

    /** Day of week (0=Sun…6=Sat in PG DOW) → order count */
    @SuppressWarnings("unchecked")
    public List<Object[]> purchaseCountByDayOfWeek() {
        return em.createNativeQuery(
            "SELECT EXTRACT(DOW FROM created_at)::int AS dow, COUNT(*) FROM orders " +
            "WHERE status='COMPLETED' GROUP BY dow ORDER BY dow")
            .getResultList();
    }

    /** productType → count */
    public List<Object[]> salesByProductType() {
        return em.createQuery(
            "SELECT oi.productType, COUNT(oi) FROM OrderItem oi " +
            "JOIN oi.order o WHERE o.status = 'COMPLETED' " +
            "GROUP BY oi.productType ORDER BY COUNT(oi) DESC", Object[].class)
            .getResultList();
    }

    /** [hour, dayOfWeek, count] for student logins */
    @SuppressWarnings("unchecked")
    public List<Object[]> studentLoginHeatmap() {
        return em.createNativeQuery(
            "SELECT EXTRACT(HOUR FROM created_at)::int AS hr, EXTRACT(DOW FROM created_at)::int AS dow, COUNT(*) " +
            "FROM user_sessions WHERE user_type='STUDENT' " +
            "GROUP BY hr, dow")
            .getResultList();
    }

    /** courseTitle → avgQuizScore */
    public List<Object[]> avgScoreByCourse() {
        return em.createQuery(
            "SELECT e.course.title, AVG(e.averageQuizScore) FROM Enrollment e " +
            "WHERE e.status = 'ACTIVE' AND e.course IS NOT NULL AND e.averageQuizScore > 0 " +
            "GROUP BY e.course.id, e.course.title ORDER BY AVG(e.averageQuizScore) DESC", Object[].class)
            .getResultList();
    }

    /** centerName → avgQuizScore */
    public List<Object[]> avgScoreByCenter() {
        return em.createQuery(
            "SELECT s.centerName, AVG(e.averageQuizScore) FROM Enrollment e " +
            "JOIN e.student s WHERE e.status = 'ACTIVE' AND s.centerName IS NOT NULL AND e.averageQuizScore > 0 " +
            "GROUP BY s.centerName ORDER BY AVG(e.averageQuizScore) DESC", Object[].class)
            .getResultList();
    }

    /** [topicName, totalAnswers, wrongAnswers] — hardest topics */
    public List<Object[]> hardestTopics() {
        return em.createQuery(
            "SELECT qt.name, COUNT(sa), " +
            "SUM(CASE WHEN sa.selectedAnswer <> q.correctAnswer THEN 1 ELSE 0 END) " +
            "FROM StudentAnswer sa " +
            "JOIN sa.question q JOIN q.topic qt " +
            "WHERE qt IS NOT NULL " +
            "GROUP BY qt.id, qt.name " +
            "HAVING COUNT(sa) >= 10 " +
            "ORDER BY (SUM(CASE WHEN sa.selectedAnswer <> q.correctAnswer THEN 1 ELSE 0 END) * 1.0 / COUNT(sa)) DESC",
            Object[].class)
            .setMaxResults(10)
            .getResultList();
    }

    /** Average total watch time in seconds across all active enrollments */
    public Double avgPlatformTimeSeconds() {
        Double result = em.createQuery(
            "SELECT AVG(e.totalWatchTimeSeconds) FROM Enrollment e WHERE e.status = 'ACTIVE'",
            Double.class).getSingleResult();
        return result != null ? result : 0.0;
    }

    /** Average attempt number when student finally passed */
    public Double avgAttemptsToPass() {
        Double result = em.createQuery(
            "SELECT AVG(a.attemptNumber) FROM StudentQuizAttempt a WHERE a.passed = true",
            Double.class).getSingleResult();
        return result != null ? result : 1.0;
    }

    // ─────────────────────────────────────────────────────────────
    // STUDENT ANALYTICS (by studentId)
    // ─────────────────────────────────────────────────────────────

    /** [courseTitle, myAvg, centerAvg, allAvg] */
    public List<Object[]> studentVsAvgByCourse(Long studentId) {
        // My scores per course
        return em.createQuery(
            "SELECT e.course.title, e.averageQuizScore, " +
            "(SELECT AVG(e2.averageQuizScore) FROM Enrollment e2 " +
            "   JOIN e2.student s2 WHERE e2.course.id = e.course.id " +
            "   AND s2.centerName = (SELECT s3.centerName FROM Student s3 WHERE s3.id = :sid) " +
            "   AND e2.averageQuizScore > 0), " +
            "(SELECT AVG(e3.averageQuizScore) FROM Enrollment e3 WHERE e3.course.id = e.course.id AND e3.averageQuizScore > 0) " +
            "FROM Enrollment e WHERE e.student.id = :sid AND e.course IS NOT NULL AND e.averageQuizScore > 0",
            Object[].class)
            .setParameter("sid", studentId)
            .getResultList();
    }

    /** [yearMonth, avgScore] — progress over time (quiz scores) */
    @SuppressWarnings("unchecked")
    public List<Object[]> quizScoreProgressOverTime(Long studentId) {
        return em.createNativeQuery(
            "SELECT EXTRACT(YEAR FROM submitted_at)::int AS yr, EXTRACT(MONTH FROM submitted_at)::int AS mo, AVG(score) " +
            "FROM student_quiz_attempts WHERE student_id=:sid AND submitted=true " +
            "GROUP BY yr, mo ORDER BY yr, mo")
            .setParameter("sid", studentId)
            .getResultList();
    }

    /** [hour] → login count for student */
    @SuppressWarnings("unchecked")
    public List<Object[]> studentLoginHours(Long studentId) {
        return em.createNativeQuery(
            "SELECT EXTRACT(HOUR FROM created_at)::int AS hr, COUNT(*) FROM user_sessions " +
            "WHERE user_id=:sid AND user_type='STUDENT' " +
            "GROUP BY hr ORDER BY hr")
            .setParameter("sid", studentId)
            .getResultList();
    }

    /** [dayOfWeek, count] for student logins */
    @SuppressWarnings("unchecked")
    public List<Object[]> studentLoginDays(Long studentId) {
        return em.createNativeQuery(
            "SELECT EXTRACT(DOW FROM created_at)::int AS dow, COUNT(*) FROM user_sessions " +
            "WHERE user_id=:sid AND user_type='STUDENT' " +
            "GROUP BY dow ORDER BY dow")
            .setParameter("sid", studentId)
            .getResultList();
    }

    /** Total watch time in seconds for student */
    public Long studentTotalWatchSeconds(Long studentId) {
        Long r = em.createQuery(
            "SELECT SUM(e.totalWatchTimeSeconds) FROM Enrollment e WHERE e.student.id = :sid",
            Long.class).setParameter("sid", studentId).getSingleResult();
        return r != null ? r : 0L;
    }

    /** Completed lessons, passed quizzes, total enrollments */
    public Object[] studentMilestones(Long studentId) {
        return em.createQuery(
            "SELECT SUM(e.completedLessonsCount), SUM(e.quizzesPassed), COUNT(e) " +
            "FROM Enrollment e WHERE e.student.id = :sid AND e.status = 'ACTIVE'",
            Object[].class)
            .setParameter("sid", studentId)
            .getSingleResult();
    }

    /** Count distinct active days in last 60 days (for streak base) */
    @SuppressWarnings("unchecked")
    public List<Object[]> studentActiveDates(Long studentId) {
        return em.createNativeQuery(
            "SELECT created_at::date AS dt FROM user_sessions " +
            "WHERE user_id=:sid AND user_type='STUDENT' " +
            "GROUP BY dt ORDER BY dt DESC")
            .setParameter("sid", studentId)
            .setMaxResults(90)
            .getResultList();
    }

    /** [quizTitle, avgSeconds] — quiz solve speed */
    @SuppressWarnings("unchecked")
    public List<Object[]> studentQuizSpeed(Long studentId) {
        return em.createNativeQuery(
            "SELECT q.title, AVG(EXTRACT(EPOCH FROM (a.submitted_at - a.started_at))) AS avg_secs " +
            "FROM student_quiz_attempts a JOIN quizzes q ON q.id = a.quiz_id " +
            "WHERE a.student_id=:sid AND a.submitted=true AND a.started_at IS NOT NULL " +
            "GROUP BY q.id, q.title ORDER BY avg_secs")
            .setParameter("sid", studentId)
            .getResultList();
    }

    /** Quiz attempts until passed per quiz */
    public List<Object[]> studentQuizAttemptsToPass(Long studentId) {
        return em.createQuery(
            "SELECT a.quiz.title, MAX(a.attemptNumber) FROM StudentQuizAttempt a " +
            "WHERE a.student.id = :sid AND a.passed = true " +
            "GROUP BY a.quiz.id, a.quiz.title ORDER BY MAX(a.attemptNumber) DESC",
            Object[].class)
            .setParameter("sid", studentId)
            .getResultList();
    }
}
