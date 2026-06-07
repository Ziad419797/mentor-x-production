package com.educore.quiz;

import com.educore.student.Student;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "student_quiz_attempts",
        indexes = {
                @Index(name = "idx_attempt_quiz",         columnList = "quiz_id"),
                @Index(name = "idx_attempt_student",      columnList = "student_id"),
                @Index(name = "idx_attempt_quiz_student", columnList = "quiz_id, student_id")
        })
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentQuizAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    @JsonIgnore
    private Student student;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quiz_id", nullable = false)
    private Quiz quiz;

    private Integer score;

    private Boolean submitted;

    /** رقم المحاولة (1, 2, 3...) — يُحسب في الـ Service قبل الحفظ */
    @Column(name = "attempt_number", nullable = false)
    @Builder.Default
    private Integer attemptNumber = 1;

    /** هل نجح الطالب في هذه المحاولة؟ — يُحسب من passingScore وقت التصحيح */
    @Column(name = "passed")
    private Boolean passed;

    @CreationTimestamp
    private LocalDateTime submittedAt;

    private LocalDateTime startedAt;

    private LocalDateTime expiresAt;

    /** عدد إجابات الطالب الصح — يُحسب وقت التصحيح */
    @Column(name = "correct_answers")
    private Integer correctAnswers;

}
