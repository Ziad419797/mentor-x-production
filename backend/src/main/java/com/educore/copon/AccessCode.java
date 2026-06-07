package com.educore.copon;

import com.educore.category.Category;
import com.educore.unit.Session;
import java.math.BigDecimal;
import com.educore.course.Course;
import jakarta.persistence.*;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.*;
import org.hibernate.annotations.*;
import org.hibernate.annotations.Cache;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "access_codes",
        indexes = {
                @Index(name = "idx_ac_code",       columnList = "code",        unique = true),
                @Index(name = "idx_ac_teacher",    columnList = "created_by_id"),
                @Index(name = "idx_ac_category",   columnList = "category_id"),
                @Index(name = "idx_ac_course",     columnList = "course_id"),
                @Index(name = "idx_ac_active",     columnList = "active"),
                @Index(name = "idx_ac_expires",    columnList = "expires_at"),
                @Index(name = "idx_ac_batch", columnList = "batch_label"),
                @Index(name = "idx_ac_session",    columnList = "session_id")
        }
)
@Cacheable
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE, region = "accessCodeCache")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class AccessCode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /* ── الكود الفريد ── */
    @Column(nullable = false, unique = true, length = 20)
    private String code;

    /* ── نوع الكود: باقة أو كورس واحد ── */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CodeTargetType targetType; // CATEGORY | COURSE

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id",
            foreignKey = @ForeignKey(name = "fk_ac_category"))
    private Category category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id",
            foreignKey = @ForeignKey(name = "fk_ac_course"))
    private Course course;

    /**
     * الحصة المستهدفة — يُستخدم مع targetType = SESSION فقط.
     * الطالب يتسجل في الكورسات المرتبطة بهذه الحصة.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id",
            foreignKey = @ForeignKey(name = "fk_ac_session"))
    private Session session;

    /**
     * سعر الكود (اختياري) — لو الكود مش مجاني.
     * null = مجاني، > 0 = الطالب يدفع هذا المبلغ عند تفعيل الكود.
     */
    @Column(name = "price", precision = 10, scale = 2)
    private BigDecimal price;

    /* ── المدرس الذي أنشأ الكود ── */
    @Column(name = "created_by_id", nullable = false)
    private Long createdById;

    @Column(name = "created_by_name", nullable = false, length = 100)
    private String createdByName;

    /* ── الحدود ── */
    /** عدد مرات الاستخدام المسموح بها (null = غير محدود) */
    @Column(name = "max_uses")
    private Integer maxUses;

    @Column(name = "used_count", nullable = false)
    @Builder.Default
    private Integer usedCount = 0;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    /* ── الحالة ── */
    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    @Column(name = "batch_label", length = 100)
    private String batchLabel; // وصف الدفعة (مثلاً "طلاب يناير 2025")

    /* ── التوقيتات ── */
    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    /* ══════════════ Business Methods ══════════════ */

    public boolean isValid() {
        if (!Boolean.TRUE.equals(active)) return false;
        if (expiresAt != null && expiresAt.isBefore(LocalDateTime.now())) return false;
        if (maxUses != null && usedCount >= maxUses) return false;
        return true;
    }

    public void incrementUsage() {
        this.usedCount++;
        // أوقف الكود تلقائياً لو وصل الحد
        if (maxUses != null && usedCount >= maxUses) {
            this.active = false;
        }
    }

    public int getRemainingUses() {
        if (maxUses == null) return Integer.MAX_VALUE;
        return Math.max(0, maxUses - usedCount);
    }
}
