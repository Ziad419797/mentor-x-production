package com.educore.analytics;

import com.educore.analytics.dto.*;
import com.educore.assignment.assignmentQuestion.StudentAssignmentAnswerRepository;
import com.educore.enrollment.EnrollmentRepository;
import com.educore.payment.payment.PaymentRepository;
import com.educore.quiz.StudentAnswerRepository;
import com.educore.quiz.StudentQuizAttemptRepository;
import com.educore.session.UserSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class TeacherAnalyticsService {

    private final PaymentRepository paymentRepository;
    private final UserSessionRepository userSessionRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final StudentQuizAttemptRepository attemptRepository;
    private final StudentAnswerRepository quizAnswerRepository;
    private final StudentAssignmentAnswerRepository assignmentAnswerRepository;

    /** Heatmap of purchases by dayOfWeek x hour */
    @Transactional(readOnly = true)
    public List<HeatmapCellDto> getPurchaseHeatmap() {
        return paymentRepository.getPurchaseHeatmap().stream()
                .map(r -> HeatmapCellDto.builder()
                        .dayOfWeek(((Number) r[0]).intValue())
                        .hour(((Number) r[1]).intValue())
                        .count(((Number) r[2]).longValue())
                        .build())
                .toList();
    }

    /** Sales breakdown by enrollment type (COURSE vs CATEGORY) */
    @Transactional(readOnly = true)
    public List<SalesTypeDto> getSalesByType() {
        List<Object[]> rows = enrollmentRepository.countByEnrollmentType();
        long total = rows.stream().mapToLong(r -> ((Number) r[1]).longValue()).sum();
        return rows.stream()
                .map(r -> SalesTypeDto.builder()
                        .enrollmentType(r[0] != null ? r[0].toString() : "UNKNOWN")
                        .count(((Number) r[1]).longValue())
                        .percentage(total > 0 ? Math.round(((Number) r[1]).doubleValue() / total * 1000.0) / 10.0 : 0.0)
                        .build())
                .toList();
    }

    /** Heatmap of student logins by dayOfWeek x hour */
    @Transactional(readOnly = true)
    public List<HeatmapCellDto> getLoginHeatmap() {
        return userSessionRepository.getStudentLoginHeatmap().stream()
                .map(r -> HeatmapCellDto.builder()
                        .dayOfWeek(((Number) r[0]).intValue())
                        .hour(((Number) r[1]).intValue())
                        .count(((Number) r[2]).longValue())
                        .build())
                .toList();
    }

    /** Average quiz scores per course */
    @Transactional(readOnly = true)
    public List<GradesByCourseDto> getGradesByCourse() {
        return enrollmentRepository.getAvgQuizScoreByCourse().stream()
                .map(r -> GradesByCourseDto.builder()
                        .courseId(((Number) r[0]).longValue())
                        .courseTitle(r[1] != null ? r[1].toString() : "")
                        .avgQuizScore(r[2] != null ? Math.round(((Number) r[2]).doubleValue() * 10.0) / 10.0 : 0.0)
                        .enrollmentCount(r[3] != null ? ((Number) r[3]).longValue() : 0L)
                        .build())
                .toList();
    }

    /** Average quiz scores per center */
    @Transactional(readOnly = true)
    public List<GradesByCenterDto> getGradesByCenter() {
        return enrollmentRepository.getAvgQuizScoreByCenter().stream()
                .map(r -> GradesByCenterDto.builder()
                        .centerName(r[0] != null ? r[0].toString() : "أونلاين")
                        .avgQuizScore(r[1] != null ? Math.round(((Number) r[1]).doubleValue() * 10.0) / 10.0 : 0.0)
                        .studentCount(r[2] != null ? ((Number) r[2]).longValue() : 0L)
                        .build())
                .toList();
    }

    /** Hardest quizzes (lowest pass rate, minimum 3 attempts) */
    @Transactional(readOnly = true)
    public List<QuizPassRateDto> getHardestQuizzes(int limit) {
        return attemptRepository.getQuizPassRates(PageRequest.of(0, limit)).stream()
                .map(r -> QuizPassRateDto.builder()
                        .quizId(((Number) r[0]).longValue())
                        .quizTitle(r[1] != null ? r[1].toString() : "")
                        .totalAttempts(((Number) r[2]).longValue())
                        .passedAttempts(r[3] != null ? ((Number) r[3]).longValue() : 0L)
                        .passRate(r[4] != null ? Math.round(((Number) r[4]).doubleValue() * 10.0) / 10.0 : 0.0)
                        .build())
                .toList();
    }

    /** Average student session duration on platform */
    @Transactional(readOnly = true)
    public PlatformTimeDto getPlatformTimeStats() {
        Object[] row = userSessionRepository.getStudentSessionDurationStats();
        if (row == null || row[0] == null) {
            return PlatformTimeDto.builder()
                    .avgSessionMinutes(0)
                    .maxSessionMinutes(0)
                    .totalActiveSessions(0)
                    .build();
        }
        return PlatformTimeDto.builder()
                .avgSessionMinutes(Math.round(((Number) row[0]).doubleValue() * 10.0) / 10.0)
                .maxSessionMinutes(Math.round(((Number) row[1]).doubleValue() * 10.0) / 10.0)
                .totalActiveSessions(((Number) row[2]).longValue())
                .build();
    }

    /**
     * إحصائيات ضعف الطلاب بالـ topic — يجمع بيانات الكويزات والواجبات
     * يرجع: [topicId, topicName, totalAnswers, wrongAnswers, wrongPct]
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getTopicWeakness() {
        // اجمع من الكويزات
        Map<Long, Map<String, Object>> merged = new HashMap<>();

        for (Object[] r : quizAnswerRepository.getTopicErrorStats()) {
            long topicId  = ((Number) r[0]).longValue();
            String name   = r[1] != null ? r[1].toString() : "";
            long total    = ((Number) r[2]).longValue();
            long wrong    = r[3] != null ? ((Number) r[3]).longValue() : 0L;
            merged.put(topicId, new HashMap<>(Map.of(
                    "topicId", topicId, "topicName", name,
                    "totalAnswers", total, "wrongAnswers", wrong)));
        }

        // ادمج من الواجبات
        for (Object[] r : assignmentAnswerRepository.getTopicErrorStats()) {
            long topicId = ((Number) r[0]).longValue();
            String name  = r[1] != null ? r[1].toString() : "";
            long total   = ((Number) r[2]).longValue();
            long wrong   = r[3] != null ? ((Number) r[3]).longValue() : 0L;
            merged.merge(topicId, new HashMap<>(Map.of(
                    "topicId", topicId, "topicName", name,
                    "totalAnswers", total, "wrongAnswers", wrong)),
                    (existing, incoming) -> {
                        existing.put("totalAnswers",
                                (Long) existing.get("totalAnswers") + (Long) incoming.get("totalAnswers"));
                        existing.put("wrongAnswers",
                                (Long) existing.get("wrongAnswers") + (Long) incoming.get("wrongAnswers"));
                        return existing;
                    });
        }

        // احسب النسبة وارتب تنازلياً
        List<Map<String, Object>> result = new ArrayList<>(merged.values());
        result.forEach(m -> {
            long total = (Long) m.get("totalAnswers");
            long wrong = (Long) m.get("wrongAnswers");
            double pct = total > 0 ? Math.round(wrong * 1000.0 / total) / 10.0 : 0.0;
            m.put("wrongPct", pct);
        });
        result.sort((a, b) -> Double.compare((Double) b.get("wrongPct"), (Double) a.get("wrongPct")));
        return result;
    }
}
