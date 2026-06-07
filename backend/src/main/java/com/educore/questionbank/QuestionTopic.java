package com.educore.questionbank;

import com.educore.unit.Session;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * موضوع/جزئية داخل درس — هياركي غير محدود العمق.
 *
 * مثال:
 *   الدرس: قوانين نيوتن
 *   ├── الموضوع: القانون الأول
 *   │   ├── فكرة: حالات السكون
 *   │   └── فكرة: حالات الحركة المنتظمة
 *   └── الموضوع: القانون الثاني
 *       ├── فكرة: حساب القوة بمعادلة F=ma
 *       └── فكرة: حساب التسارع
 *
 * الـ shuffle بيشتغل على مستوى الـ leaf topics — بياخد سؤال واحد من كل جزئية.
 */
@Entity
@Table(
    name = "question_topics",
    indexes = {
        @Index(name = "idx_qtopic_session", columnList = "session_id"),
        @Index(name = "idx_qtopic_parent", columnList = "parent_topic_id")
    }
)
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class QuestionTopic {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ── اسم الجزئية ───────────────────────────────────────
    @Column(nullable = false, length = 200)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "order_number")
    private Integer orderNumber;

    // ── المحاضرة اللي الجزئية دي بتخصها ──────────────────
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private Session session;

    // ── الجزئية الأب (null = جزئية رئيسية) ───────────────
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_topic_id")
    private QuestionTopic parentTopic;

    // ── الجزئيات الفرعية ──────────────────────────────────
    @OneToMany(mappedBy = "parentTopic", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orderNumber ASC")
    @Builder.Default
    private List<QuestionTopic> subTopics = new ArrayList<>();

    @Builder.Default
    @Column(nullable = false)
    private boolean active = true;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    // ── Helper: هل الجزئية دي leaf (ما فيهاش جزئيات فرعية)? ─
    public boolean isLeaf() {
        return subTopics == null || subTopics.isEmpty();
    }
}
