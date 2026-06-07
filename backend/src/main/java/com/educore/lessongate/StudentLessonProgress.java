package com.educore.lessongate;

import com.educore.lesson.Week;
import com.educore.student.Student;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * يتتبع حالة كل حصة لكل طالب.
 *
 * الـ flow:
 *   LOCKED → UNLOCKED (بعد إتمام الحصة السابقة أو عند التسجيل في الأولى)
 *   UNLOCKED → IN_PROGRESS (لما الطالب يفتح الحصة)
 *   IN_PROGRESS → COMPLETED (بعد ما يعدي الكويز أو الواجب)
 *   COMPLETED → (يفتح الحصة الجاية تلقائياً)
 */
@Entity
@Table(
    name = "student_lesson_progress",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uq_student_week",
            columnNames = {"student_id", "week_id"}
        )
    },
    indexes = {
        @Index(name = "idx_slp_student",        columnList = "student_id"),
        @Index(name = "idx_slp_week",           columnList = "week_id"),
        @Index(name = "idx_slp_status",         columnList = "status"),
        @Index(name = "idx_slp_student_status", columnList = "student_id, status")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentLessonProgress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ─── الطالب والحصة ────────────────────────────────────
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "week_id", nullable = false)
    private Week week;

    // ─── الحالة ───────────────────────────────────────────
    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private LessonProgressStatus status = LessonProgressStatus.LOCKED;

    // ─── تواريخ المراحل ───────────────────────────────────
    @Column(name = "unlocked_at")
    private LocalDateTime unlockedAt;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    // ─── نتائج التقييم ────────────────────────────────────

    /** درجة الكويز المطلوبة لفتح الحصة التالية */
    @Column(name = "quiz_score")
    private Double quizScore;

    @Builder.Default
    @Column(name = "quiz_passed", nullable = false)
    private boolean quizPassed = false;

    @Builder.Default
    @Column(name = "assignment_submitted", nullable = false)
    private boolean assignmentSubmitted = false;

    @Column(name = "assignment_score")
    private Double assignmentScore;

    // ─── أوديت ────────────────────────────────────────────
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // ─── Helpers ──────────────────────────────────────────

    public boolean isAccessible() {
        return status == LessonProgressStatus.UNLOCKED
                || status == LessonProgressStatus.IN_PROGRESS
                || status == LessonProgressStatus.COMPLETED;
    }

    public void unlock() {
        this.status    = LessonProgressStatus.UNLOCKED;
        this.unlockedAt = LocalDateTime.now();
    }

    public void markInProgress() {
        if (this.status == LessonProgressStatus.UNLOCKED) {
            this.status    = LessonProgressStatus.IN_PROGRESS;
            this.startedAt = LocalDateTime.now();
        }
    }

    public void complete(double quizScore, boolean passed) {
        this.quizScore    = quizScore;
        this.quizPassed   = passed;
        this.status       = LessonProgressStatus.COMPLETED;
        this.completedAt  = LocalDateTime.now();
    }
}
