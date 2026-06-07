package com.educore.quiz;

import com.educore.lesson.Week;
import com.educore.question.Question;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.*;
import org.hibernate.annotations.*;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "quizzes",
        indexes = {
                @Index(name = "idx_quiz_week", columnList = "week_id"),
                @Index(name = "idx_quiz_deleted", columnList = "deleted"),
                @Index(name = "idx_quiz_active", columnList = "active")
        })
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@SQLDelete(sql = "UPDATE quizzes SET deleted = true WHERE id = ?")
@Where(clause = "deleted = false")
public class Quiz {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Builder.Default
    @Column(nullable = false)
    private Boolean active = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "week_id", nullable = false)
    @JsonIgnore
    private Week week;

    @OneToMany(mappedBy = "quiz", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnoreProperties("quiz")
    @org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    private Set<Question> questions = new HashSet<>();

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @Column(nullable = false)
    private Integer durationMinutes;

    @Builder.Default
    @Column(name = "passing_score", nullable = false)
    private Integer passingScore = 50;

    @Builder.Default
    @Column(name = "attempts_allowed", nullable = false)
    private Integer attemptsAllowed = 1;

    @Builder.Default
    @Column(name = "order_number", nullable = false)
    private Integer orderNumber = 0;

    @Builder.Default
    @Column(nullable = false)
    private Boolean timeRestricted = false;

    @Builder.Default
    @Column(nullable = false)
    private Boolean deleted = false;

    /** نوع الامتحان: SESSION_QUIZ / COMPREHENSIVE / CUMULATIVE */
    @Builder.Default
    @Column(name = "quiz_type", length = 30)
    private String quizType = "SESSION_QUIZ";

    /** ترتيب الأسئلة: FIXED / RANDOM */
    @Builder.Default
    @Column(name = "question_order", length = 10)
    private String questionOrder = "FIXED";

    /** نقاط الطالب عند إكمال الامتحان */
    @Builder.Default
    @Column(name = "points")
    private Integer points = 0;

    /** اسم الجائزة (اختياري) */
    @Column(name = "prize_name")
    private String prizeName;

    /** الدرجة المطلوبة للحصول على الجائزة */
    @Column(name = "prize_score")
    private Integer prizeScore;

    /** هل يمكن تحسين الدرجة؟ */
    @Builder.Default
    @Column(name = "improvable")
    private Boolean improvable = false;

    /** تاريخ بداية الامتحان */
    @Column(name = "start_date")
    private java.time.LocalDateTime startDate;

    /** تاريخ نهاية الامتحان */
    @Column(name = "end_date")
    private java.time.LocalDateTime endDate;
}
