package com.educore.analytics;

import com.educore.analytics.dto.*;
import com.educore.enrollment.Enrollment;
import com.educore.enrollment.EnrollmentStatus;
import com.educore.enrollment.EnrollmentRepository;
import com.educore.exception.ResourceNotFoundException;
import com.educore.parent.Parent;
import com.educore.parent.ParentRepository;
import com.educore.assignment.StudentAssignmentAttemptRepository;
import com.educore.quiz.StudentQuizAttemptRepository;
import com.educore.session.UserSessionRepository;
import com.educore.student.Student;
import com.educore.student.StudentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class StudentAnalyticsService {

    private final EnrollmentRepository enrollmentRepository;
    private final StudentQuizAttemptRepository attemptRepository;
    private final StudentAssignmentAttemptRepository assignmentAttemptRepository;
    private final UserSessionRepository userSessionRepository;
    private final StudentRepository studentRepository;
    private final ParentRepository parentRepository;

    /** Grade comparison: my avg % vs all students */
    @Transactional(readOnly = true)
    public StudentGradeComparisonDto getGradeComparison(Long studentId) {
        List<Object[]> allAvgs = attemptRepository.getAllStudentAvgPercentages();

        double myAvg = allAvgs.stream()
                .filter(r -> ((Number) r[0]).longValue() == studentId)
                .mapToDouble(r -> r[1] != null ? ((Number) r[1]).doubleValue() : 0.0)
                .findFirst()
                .orElse(0.0);

        double classAvg = allAvgs.stream()
                .filter(r -> r[1] != null)
                .mapToDouble(r -> ((Number) r[1]).doubleValue())
                .average()
                .orElse(0.0);

        long rank = allAvgs.stream()
                .filter(r -> r[1] != null && ((Number) r[1]).doubleValue() > myAvg)
                .count() + 1;

        int myAttempts = (int) attemptRepository.countByStudentIdAndSubmittedTrue(studentId);

        return StudentGradeComparisonDto.builder()
                .studentId(studentId)
                .myAvgPercentage(Math.round(myAvg * 10.0) / 10.0)
                .classAvgPercentage(Math.round(classAvg * 10.0) / 10.0)
                .myRank(rank)
                .totalStudents((long) allAvgs.size())
                .totalAttempts(myAttempts)
                .build();
    }

    /** Course progress list for a student */
    @Transactional(readOnly = true)
    public List<ProgressSummaryDto> getCourseProgress(Long studentId) {
        return enrollmentRepository.findByStudentIdOrderByEnrolledAt(studentId).stream()
                .map(e -> ProgressSummaryDto.builder()
                        .courseId(e.getCourse() != null ? e.getCourse().getId() : null)
                        .courseTitle(e.getCourse() != null ? e.getCourse().getTitle() : "")
                        .progress(e.getProgress() != null ? e.getProgress() : 0.0)
                        .enrolledAt(e.getEnrolledAt())
                        .lastAccessedAt(e.getLastAccessedAt())
                        .avgQuizScore(e.getAverageQuizScore() != null ? e.getAverageQuizScore() : 0.0)
                        .quizzesTaken(e.getQuizzesTaken() != null ? e.getQuizzesTaken() : 0)
                        .quizzesPassed(e.getQuizzesPassed() != null ? e.getQuizzesPassed() : 0)
                        .status(toProgressStatus(e.getStatus()))
                        .build())
                .toList();
    }

    /** Most active hours for a student */
    @Transactional(readOnly = true)
    public List<ActiveHourDto> getActiveHours(Long studentId) {
        return userSessionRepository.getStudentActiveHours(studentId).stream()
                .map(r -> ActiveHourDto.builder()
                        .hour(((Number) r[0]).intValue())
                        .count(((Number) r[1]).longValue())
                        .build())
                .toList();
    }

    /** Achievements based on student milestones — fully dynamic from real student data */
    @Transactional(readOnly = true)
    public AchievementsDto getAchievements(Long studentId) {
        // ── جلب الإحصاءات الحقيقية من الداتابيز ──────────────────
        long totalQuizAttempts  = attemptRepository.countByStudentIdAndSubmittedTrue(studentId);
        // passedAttempts: درجة > 0 كـ proxy (حتى تتوفر threshold لكل كويز)
        long passedQuizAttempts = attemptRepository.countByStudentIdAndSubmittedTrueAndScoreGreaterThan(studentId, 0);
        long enrollments        = enrollmentRepository.countActiveEnrollmentsByStudent(studentId);
        long completedCourses   = enrollmentRepository
                .findByStudentIdAndStatusAndActiveTrue(studentId, EnrollmentStatus.COMPLETED).size();
        long activeDaysCount    = userSessionRepository.countDistinctActiveDays(studentId) != null
                                  ? userSessionRepository.countDistinctActiveDays(studentId) : 0L;

        // ── تعريف كل الإنجازات ──────────────────────────────────
        record AchievementDef(String id, String title, String description, String icon, int threshold, long progress) {}

        List<AchievementDef> defs = List.of(
            // كويزات - محاولات
            new AchievementDef("first_quiz",   "أول كويز",        "حللت أول كويز",           "target",         1,  totalQuizAttempts),
            new AchievementDef("quiz_5",        "5 كويزات",        "حللت 5 كويزات",            "edit",           5,  totalQuizAttempts),
            new AchievementDef("quiz_10",       "10 كويزات",       "حللت 10 كويزات",           "fire",           10, totalQuizAttempts),
            new AchievementDef("quiz_25",       "25 كويز",         "حللت 25 كويز",             "trophy",         25, totalQuizAttempts),
            new AchievementDef("quiz_50",       "50 كويز",         "حللت 50 كويز",             "star",           50, totalQuizAttempts),
            // كويزات - نجاح
            new AchievementDef("first_pass",    "أول نجاح",        "نجحت في أول كويز",         "check",          1,  passedQuizAttempts),
            new AchievementDef("pass_5",        "5 نجاحات",        "نجحت في 5 كويزات",         "star",           5,  passedQuizAttempts),
            new AchievementDef("pass_10",       "10 نجاحات",       "نجحت في 10 كويزات",        "medal",          10, passedQuizAttempts),
            new AchievementDef("pass_25",       "25 نجاح",         "نجحت في 25 كويز",          "crown",          25, passedQuizAttempts),
            // اشتراكات
            new AchievementDef("enrolled_1",    "أول اشتراك",      "اشتركت في أول كورس",        "book",           1,  enrollments),
            new AchievementDef("enrolled_3",    "3 كورسات",        "اشتركت في 3 كورسات",        "graduation-cap", 3,  enrollments),
            new AchievementDef("enrolled_5",    "5 كورسات",        "اشتركت في 5 كورسات",        "library",        5,  enrollments),
            // إتمام الكورسات
            new AchievementDef("completed_1",   "أكملت كورس",      "أكملت أول كورس بالكامل",    "check-circle",   1,  completedCourses),
            new AchievementDef("completed_3",   "3 كورسات مكتملة", "أكملت 3 كورسات",            "award",          3,  completedCourses),
            // الاستمرارية
            new AchievementDef("active_7",      "7 أيام نشاط",     "فتحت التطبيق 7 أيام مختلفة","calendar",       7,  activeDaysCount),
            new AchievementDef("active_30",     "30 يوم نشاط",     "30 يوم نشاط على المنصة",    "calendar-check", 30, activeDaysCount)
        );

        List<AchievementsDto.Achievement> unlocked = new ArrayList<>();
        List<AchievementsDto.Achievement> locked   = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        for (AchievementDef def : defs) {
            boolean isUnlocked = def.progress() >= def.threshold();
            AchievementsDto.Achievement ach = AchievementsDto.Achievement.builder()
                    .id(def.id())
                    .title(def.title())
                    .description(def.description())
                    .icon(def.icon())
                    .unlocked(isUnlocked)
                    .unlockedAt(isUnlocked ? now : null)  // approximate — full timestamp needs activity log
                    .threshold(def.threshold())
                    .currentProgress(def.progress())
                    .build();
            if (isUnlocked) unlocked.add(ach); else locked.add(ach);
        }

        return AchievementsDto.builder().unlocked(unlocked).locked(locked).build();
    }

    /** Study streak based on session activity */
    @Transactional(readOnly = true)
    public StreakDto getStreak(Long studentId) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found: " + studentId));

        Long activeDays = userSessionRepository.countDistinctActiveDays(studentId);
        int totalActiveDays = activeDays != null ? activeDays.intValue() : 0;

        LocalDate lastActivityDate = student.getLastActivityAt() != null
                ? student.getLastActivityAt().toLocalDate()
                : null;

        // Simple streak: days since last activity (naive but works without full activity log)
        int currentStreak = 0;
        if (lastActivityDate != null) {
            long daysSinceLast = Duration.between(
                    lastActivityDate.atStartOfDay(),
                    LocalDate.now().atStartOfDay()).toDays();
            currentStreak = daysSinceLast <= 1 ? Math.min(totalActiveDays, 7) : 0;
        }

        return StreakDto.builder()
                .currentStreakDays(currentStreak)
                .longestStreakDays(0) // would need full activity log
                .lastActivityDate(lastActivityDate)
                .totalActiveDays(totalActiveDays)
                .loginCount(student.getLoginCount() != null ? student.getLoginCount() : 0)
                .build();
    }

    /** Quiz solving speed list */
    @Transactional(readOnly = true)
    public List<QuizSpeedDto> getQuizSpeed(Long studentId) {
        return attemptRepository.findStudentAttemptDetails(studentId).stream()
                .filter(r -> r[3] != null && r[4] != null)  // startedAt + submittedAt not null
                .map(r -> {
                    Long quizId = ((Number) r[0]).longValue();
                    String title = r[1] != null ? r[1].toString() : "";
                    Integer score = r[2] != null ? ((Number) r[2]).intValue() : null;
                    LocalDateTime started = (LocalDateTime) r[3];
                    LocalDateTime submitted = (LocalDateTime) r[4];
                    long durationSecs = Duration.between(started, submitted).getSeconds();

                    return QuizSpeedDto.builder()
                            .quizId(quizId)
                            .quizTitle(title)
                            .score(score)
                            .durationSeconds(durationSecs)
                            .submittedAt(submitted)
                            .build();
                })
                .filter(q -> q.getDurationSeconds() > 0)
                .toList();
    }

    /** Validate that parentId owns studentId */
    public void validateParentOwnsStudent(Long parentId, Long studentId) {
        Parent parent = parentRepository.findById(parentId)
                .orElseThrow(() -> new ResourceNotFoundException("Parent not found: " + parentId));
        boolean owns = parent.getStudents().stream()
                .anyMatch(s -> s.getId().equals(studentId));
        if (!owns) {
            throw new ResourceNotFoundException(
                    "Student " + studentId + " does not belong to parent " + parentId);
        }
    }

    // ── Helpers ──────────────────────────────────────────────────

    private ProgressSummaryDto.EnrollmentStatus toProgressStatus(com.educore.enrollment.EnrollmentStatus s) {
        if (s == null) return ProgressSummaryDto.EnrollmentStatus.ACTIVE;
        return switch (s.name()) {
            case "COMPLETED" -> ProgressSummaryDto.EnrollmentStatus.COMPLETED;
            case "CANCELLED" -> ProgressSummaryDto.EnrollmentStatus.CANCELLED;
            default          -> ProgressSummaryDto.EnrollmentStatus.ACTIVE;
        };
    }

}
