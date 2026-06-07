package com.educore.coupon;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** سجل كل مرة استخدم فيها طالب كوبون */
@Entity
@Table(
    name = "coupon_redemptions",
    indexes = {
        @Index(name = "idx_cr_coupon",  columnList = "coupon_id"),
        @Index(name = "idx_cr_student", columnList = "student_id"),
        @Index(name = "idx_cr_order",   columnList = "order_id")
    }
)
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class CouponRedemption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "coupon_id", nullable = false)
    private Coupon coupon;

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    /** معرف الطلب اللي استُخدم فيه الكوبون */
    @Column(name = "order_id")
    private Long orderId;

    /** مبلغ الخصم الفعلي اللي اتطبق */
    @Column(name = "discount_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal discountAmount;

    /** السعر الأصلي قبل الخصم */
    @Column(name = "original_amount", precision = 10, scale = 2)
    private BigDecimal originalAmount;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime redeemedAt;
}
