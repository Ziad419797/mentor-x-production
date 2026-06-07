package com.educore.coupon;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * كوبون خصم — يمكن إنشاؤه من الأدمن أو المدرس.
 *
 * نوعان:
 *   PERCENTAGE   → خصم % من سعر الجلسة
 *   FIXED_AMOUNT → خصم مبلغ ثابت (جنيه)
 *
 * قيود الاستخدام:
 *   maxUses     → null = غير محدود
 *   usedCount   → عداد تلقائي
 *   expiresAt   → null = لا ينتهي
 *   minAmount   → الحد الأدنى للسعر الذي يُطبق عليه الكوبون
 */
@Entity
@Table(
    name = "coupons",
    indexes = {
        @Index(name = "idx_coupon_code",   columnList = "code", unique = true),
        @Index(name = "idx_coupon_active", columnList = "active"),
        @Index(name = "idx_coupon_exp",    columnList = "expires_at")
    }
)
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class Coupon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** كود الكوبون — حروف وأرقام، case-insensitive */
    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Column(length = 300)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CouponType type;

    /**
     * قيمة الخصم:
     *   PERCENTAGE   → من 1 لـ 100 (مثال: 20 = 20%)
     *   FIXED_AMOUNT → المبلغ بالجنيه (مثال: 50)
     */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal value;

    /** الحد الأقصى لعدد مرات الاستخدام — null = غير محدود */
    @Column(name = "max_uses")
    private Integer maxUses;

    @Column(name = "used_count", nullable = false)
    @Builder.Default
    private int usedCount = 0;

    /** الحد الأدنى لسعر الجلسة لتطبيق الكوبون — null = لا يوجد حد */
    @Column(name = "min_amount", precision = 10, scale = 2)
    private BigDecimal minAmount;

    /** أقصى قيمة خصم (مفيد مع الـ PERCENTAGE) — null = لا يوجد حد */
    @Column(name = "max_discount", precision = 10, scale = 2)
    private BigDecimal maxDiscount;

    @Builder.Default
    @Column(nullable = false)
    private boolean active = true;

    /** تاريخ انتهاء الصلاحية — null = لا ينتهي */
    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    /** من أنشأ الكوبون */
    @Column(name = "created_by", length = 100)
    private String createdBy;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    // ─── Business Methods ─────────────────────────────────────

    /** هل الكوبون صالح الآن؟ */
    public boolean isValid() {
        if (!active) return false;
        if (expiresAt != null && LocalDateTime.now().isAfter(expiresAt)) return false;
        if (maxUses != null && usedCount >= maxUses) return false;
        return true;
    }

    /**
     * يحسب مبلغ الخصم على سعر معين.
     * بيحترم maxDiscount لو محدد.
     */
    public BigDecimal calculateDiscount(BigDecimal originalPrice) {
        if (type == CouponType.PERCENTAGE) {
            BigDecimal discount = originalPrice.multiply(value)
                    .divide(BigDecimal.valueOf(100));
            if (maxDiscount != null && discount.compareTo(maxDiscount) > 0) {
                return maxDiscount;
            }
            return discount;
        } else {
            // FIXED_AMOUNT — لا يتجاوز السعر الأصلي
            return value.min(originalPrice);
        }
    }

    /** يزيد العداد — لازم يتعمل داخل @Transactional */
    public void incrementUsage() {
        this.usedCount++;
    }
}
