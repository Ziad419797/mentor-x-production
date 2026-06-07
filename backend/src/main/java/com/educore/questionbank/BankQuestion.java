package com.educore.questionbank;

import com.educore.lesson.Week;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * سؤال في بنك الأسئلة — مستقل عن أي كويز بعينه.
 *
 * conceptTag: اسم "الفكرة" — أسئلة بنفس الـ conceptTag هي نسخ (variants)
 *   من نفس الفكرة بأرقام مختلفة.
 *   مثال: conceptTag = "velocity_calculation"
 *     - سؤال: قطار يسير بسرعة 60 كم/س لمسافة 120 كم. كم يستغرق؟
 *     - سؤال: قطار يسير بسرعة 90 كم/س لمسافة 180 كم. كم يستغرق؟
 *
 * الـ ExamGenerator بياخد واحد فقط من كل conceptTag — يضمن تنوع الأسئلة.
 */
@Entity
@Table(
    name = "bank_questions",
    indexes = {
        @Index(name = "idx_bq_topic",      columnList = "topic_id"),
        @Index(name = "idx_bq_week",       columnList = "week_id"),
        @Index(name = "idx_bq_concept",    columnList = "concept_tag"),
        @Index(name = "idx_bq_difficulty", columnList = "difficulty")
    }
)
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class BankQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ── الجزئية اللي السؤال بيخصها ───────────────────────
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "topic_id", nullable = false)
    private QuestionTopic topic;

    // ── الدرس (denormalized للـ query السريع) — اختياري الآن لأن
    //    الجزئيات بقت مرتبطة بالمحاضرة (Session) مش بالدرس (Week) ──
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "week_id", nullable = true)
    private Week week;

    // ── اسم الفكرة — يجمع الـ variants ───────────────────
    // أسئلة بنفس الـ conceptTag → نسخ من نفس الفكرة
    // الـ Generator بياخد واحد فقط من كل مجموعة
    @Column(name = "concept_tag", length = 150)
    private String conceptTag;

    // ── محتوى السؤال ──────────────────────────────────────
    @Column(name = "image_url")
    private String imageUrl;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private Integer mark;

    @ElementCollection
    @CollectionTable(
        name = "bank_question_options",
        joinColumns = @JoinColumn(name = "bank_question_id")
    )
    @Column(name = "option_value")
    @OrderColumn(name = "option_order")
    @Builder.Default
    private List<String> options = new ArrayList<>();

    @Column(name = "correct_answer", nullable = false)
    private String correctAnswer;

    // ── المستوى ───────────────────────────────────────────
    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(nullable = false, length = 10)
    private DifficultyLevel difficulty = DifficultyLevel.MEDIUM;

    @Builder.Default
    @Column(nullable = false)
    private boolean active = true;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    // ── Helper ─────────────────────────────────────────────
    /** مجموعة الـ variant اللي ينتمي ليها السؤال — إما conceptTag أو id فردي */
    public String getVariantGroup() {
        return (conceptTag != null && !conceptTag.isBlank())
                ? conceptTag
                : "solo_" + id;
    }
}
