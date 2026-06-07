package com.educore.enrollment;

import com.educore.category.Category;
import com.educore.course.Course;
import com.educore.student.Student;
import jakarta.persistence.*;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.*;
import org.hibernate.annotations.*;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "enrollments",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_enrollment_student_course",
                        columnNames = {"student_id", "course_id"}
                )
        },
        indexes = {
                @Index(name = "idx_enrollment_student", columnList = "student_id"),
                @Index(name = "idx_enrollment_course", columnList = "course_id"),
                @Index(name = "idx_enrollment_status", columnList = "status"),
                @Index(name = "idx_enrollment_completed", columnList = "completed_at"),
                @Index(name = "idx_enrollment_active", columnList = "active")
        }
)
@SQLDelete(sql = "UPDATE enrollments SET active = false, deleted_at = NOW() WHERE id = ?")
@Where(clause = "active = true")
@Cacheable
@org.hibernate.annotations.Cache(
        usage = CacheConcurrencyStrategy.READ_WRITE,
        region = "enrollment"
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Enrollment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ==================== Relationships ====================

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false, foreignKey = @ForeignKey(name = "fk_enrollment_student"))
    @org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false, foreignKey = @ForeignKey(name = "fk_enrollment_course"))
    @org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    private Course course;


    // في Enrollment.java أضف:
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", foreignKey = @ForeignKey(name = "fk_enrollment_category"))
    private Category category;  // ✅ جديد: لو اشترى باقة

    @Enumerated(EnumType.STRING)
    private EnrollmentType enrollmentType;  // ✅ جديد: CATEGORY أو COURSE
    // ==================== Enrollment Details ====================

    @Builder.Default
    @Column(name = "enrolled_at", nullable = false, updatable = false)
    private LocalDateTime enrolledAt = LocalDateTime.now();

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Builder.Default
    @Column(nullable = false)
    private Double progress = 0.0;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EnrollmentStatus status = EnrollmentStatus.ACTIVE;

    // ==================== Progress Tracking ====================

    @Builder.Default
    @Column(name = "last_accessed_at")
    private LocalDateTime lastAccessedAt = LocalDateTime.now();

    @Builder.Default
    @Column(name = "total_watch_time_seconds", nullable = false)
    private Long totalWatchTimeSeconds = 0L;

    @Builder.Default
    @Column(name = "completed_lessons_count", nullable = false)
    private Integer completedLessonsCount = 0;

    @Builder.Default
    @Column(name = "total_lessons_count", nullable = false)
    private Integer totalLessonsCount = 0;

    // ==================== Quiz & Assignment Stats ====================

    @Builder.Default
    @Column(name = "quizzes_taken", nullable = false)
    private Integer quizzesTaken = 0;

    @Builder.Default
    @Column(name = "quizzes_passed", nullable = false)
    private Integer quizzesPassed = 0;

    @Builder.Default
    @Column(name = "average_quiz_score", nullable = false)
    private Double averageQuizScore = 0.0;

    @Builder.Default
    @Column(name = "assignments_submitted", nullable = false)
    private Integer assignmentsSubmitted = 0;

    @Builder.Default
    @Column(name = "average_assignment_score", nullable = false)
    private Double averageAssignmentScore = 0.0;

    // ==================== Expiry & Access ====================

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Builder.Default
    @Column(name = "access_count", nullable = false)
    private Integer accessCount = 0;

    // ==================== Notes & Metadata ====================

    @Column(name = "notes", length = 1000)
    private String notes;

    @Column(name = "metadata", length = 2000)
    private String metadata; // JSON for additional data

    // ==================== Soft Delete Fields ====================

    @Builder.Default
    @Column(name = "active", nullable = false)
    private Boolean active = true;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "deleted_by", length = 50)
    private String deletedBy;

    // ==================== Audit Fields ====================

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "created_by", length = 50)
    @Builder.Default
    private String createdBy = "SYSTEM";

    @Column(name = "updated_by", length = 50)
    private String updatedBy;

    // ==================== Helper Methods ====================

    /**
     * تحديث التقدم في الكورس
     */
    public void updateProgress(Double newProgress) {
        this.progress = Math.min(100.0, Math.max(0.0, newProgress));
        this.lastAccessedAt = LocalDateTime.now();
        this.accessCount++;

        if (this.progress >= 100.0 && this.status == EnrollmentStatus.ACTIVE) {
            completeEnrollment();
        }
    }

    /**
     * إكمال الكورس
     */
    public void completeEnrollment() {
        this.status = EnrollmentStatus.COMPLETED;
        this.completedAt = LocalDateTime.now();
        this.progress = 100.0;
    }

    /**
     * تحديث إحصائيات الكويزات
     */
    public void updateQuizStats(Double score, Boolean passed) {
        double totalScore = this.averageQuizScore * this.quizzesTaken;
        this.quizzesTaken++;
        totalScore += score;
        this.averageQuizScore = totalScore / this.quizzesTaken;

        if (passed) {
            this.quizzesPassed++;
        }
    }

    /**
     * تحديث إحصائيات الواجبات
     */
    public void updateAssignmentStats(Double score) {
        double totalScore = this.averageAssignmentScore * this.assignmentsSubmitted;
        this.assignmentsSubmitted++;
        totalScore += score;
        this.averageAssignmentScore = totalScore / this.assignmentsSubmitted;
    }

    /**
     * تحديث عدد الدروس المكتملة
     */
    public void updateLessonProgress(Integer completed, Integer total) {
        this.completedLessonsCount = completed;
        this.totalLessonsCount = total;

        if (total > 0) {
            double newProgress = (completed.doubleValue() / total.doubleValue()) * 100.0;
            updateProgress(newProgress);
        }
    }

    /**
     * زيادة وقت المشاهدة
     */
    public void addWatchTime(Long seconds) {
        this.totalWatchTimeSeconds += seconds;
        this.lastAccessedAt = LocalDateTime.now();
    }

    /**
     * تسجيل الدخول إلى الكورس
     */
    public void recordAccess() {
        this.accessCount++;
        this.lastAccessedAt = LocalDateTime.now();
    }

    /**
     * التحقق من صلاحية الوصول
     */
    public boolean isValidAccess() {
        if (!this.active || this.status != EnrollmentStatus.ACTIVE) {
            return false;
        }

        if (this.expiresAt != null && this.expiresAt.isBefore(LocalDateTime.now())) {
            return false;
        }

        return true;
    }

    /**
     * تمديد فترة الوصول
     */
    public void extendExpiry(LocalDateTime newExpiry) {
        if (newExpiry != null && (this.expiresAt == null || newExpiry.isAfter(this.expiresAt))) {
            this.expiresAt = newExpiry;
        }
    }

    /**
     * إلغاء التسجيل (Soft Delete)
     */
    public void cancel(String cancelledBy) {
        this.active = false;
        this.deletedAt = LocalDateTime.now();
        this.deletedBy = cancelledBy;
        this.status = EnrollmentStatus.CANCELLED;
    }

    // ==================== Business Methods ====================

    public boolean isCompleted() {
        return this.status == EnrollmentStatus.COMPLETED || this.progress >= 100.0;
    }

    public boolean isActive() {
        return this.active && this.status == EnrollmentStatus.ACTIVE && isValidAccess();
    }

    public boolean isExpired() {
        return this.expiresAt != null && this.expiresAt.isBefore(LocalDateTime.now());
    }

    public Double getCompletionPercentage() {
        return this.progress;
    }

    public Long getRemainingDays() {
        if (this.expiresAt == null) return null;
        return java.time.Duration.between(LocalDateTime.now(), this.expiresAt).toDays();
    }

    // ==================== toString ====================

    @Override
    public String toString() {
        return "Enrollment{" +
                "id=" + id +
                ", studentId=" + (student != null ? student.getId() : null) +
                ", courseId=" + (course != null ? course.getId() : null) +
                ", progress=" + progress +
                ", status=" + status +
                ", active=" + active +
                '}';
    }
}