package com.educore.lesson;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * يسجّل أول مرة يدخل فيها الطالب على حصة أسبوعية معينة.
 * يُستخدم لحساب قفل الحصة من نوع AFTER_DURATION:
 *   تُقفل إذا: now > firstAccessAt + lockAfterDays
 *
 * يُنشأ هذا السجل أول مرة يصل فيها الطالب للـ Week (مشاهدة مادة / تسليم كويز / إلخ).
 */
@Entity
@Table(
        name = "student_week_access",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_student_week",
                columnNames = {"student_id", "week_id"}
        ),
        indexes = {
                @Index(name = "idx_swa_student", columnList = "student_id"),
                @Index(name = "idx_swa_week",    columnList = "week_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentWeekAccess {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    @Column(name = "week_id", nullable = false)
    private Long weekId;

    /** أول وصول للطالب لهذه الحصة */
    @CreationTimestamp
    @Column(name = "first_access_at", nullable = false, updatable = false)
    private LocalDateTime firstAccessAt;
}
