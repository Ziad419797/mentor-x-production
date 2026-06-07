package com.educore.payment.order;

import com.educore.payment.payment.Payment;
import com.educore.payment.payment.PaymentMethod;
import com.educore.student.Student;
import jakarta.persistence.*;
import jakarta.persistence.CascadeType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.*;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;


/* ══════════════════════════════════════════════════════════════
   Order — الطلب الرئيسي
══════════════════════════════════════════════════════════════ */
@Entity
@Table(
        name = "orders",
        indexes = {
                @Index(name = "idx_order_student",  columnList = "student_id"),
                @Index(name = "idx_order_status",   columnList = "status"),
                @Index(name = "idx_order_number",   columnList = "order_number"),
                @Index(name = "idx_order_created",  columnList = "created_at")
        }
)
@Cacheable
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE, region = "orderCache")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String orderNumber;
    private BigDecimal discountAmount;   // ← أضف
    private BigDecimal finalAmount;      // ← أضف
    private String appliedCouponCode;    // ← أضف

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_order_student"))
    private Student student;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 25)
    @Builder.Default
    private OrderStatus status =OrderStatus.PENDING;

    /* ─────────────── الأسعار ─────────────── */
    @Column(nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal subtotal = BigDecimal.ZERO;

    @Column(precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal discount = BigDecimal.ZERO;

    @Column(precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal tax = BigDecimal.ZERO;

    @Column(nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal total = BigDecimal.ZERO;

    /* ─────────────── طريقة الدفع ─────────────── */
    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private PaymentMethod paymentMethod;

    /* ─────────────── ملاحظات ─────────────── */
    @Column(length = 1000)
    private String notes;

    /* ─────────────── العلاقات ─────────────── */
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<OrderItem> items = new ArrayList<>();

    @OneToOne(mappedBy = "order", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Payment payment;

    /* ─────────────── التوقيتات ─────────────── */
    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    private LocalDateTime paidAt;

    /* ─────────────── Optimistic Lock ─────────────── */
    @Version
    private Long version;

    /* ══════════════════════════════════════════════════════════════
       Business Methods
    ══════════════════════════════════════════════════════════════ */

    public void addItem(OrderItem item) {
        items.add(item);
        item.setOrder(this);
        recalculate();
    }

    public void recalculate() {
        this.subtotal = items.stream()
                .map(OrderItem::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        this.total = this.subtotal
                .subtract(this.discount)
                .add(this.tax)
                .max(BigDecimal.ZERO);  // لا يمكن أن يكون سالب
    }

    public void markAsPaid(PaymentMethod method) {
        this.status        = OrderStatus.PAID;
        this.paymentMethod = method;
        this.paidAt        = LocalDateTime.now();
    }

    public void markAsAwaitingApproval(PaymentMethod method) {
        this.status        = OrderStatus.AWAITING_APPROVAL;
        this.paymentMethod = method;
    }

    public void markAsFailed(String reason) {
        this.status = OrderStatus.FAILED;
        this.notes  = reason;
    }

    public void cancel() {
        if (this.status == OrderStatus.PAID) {
            throw new IllegalStateException("Cannot cancel a paid order. Use refund instead.");
        }
        this.status = OrderStatus.CANCELLED;
    }

    public void markAsRefunded() {
        this.status = OrderStatus.REFUNDED;
    }

    public boolean isPending()  { return this.status == OrderStatus.PENDING; }
    public boolean isPaid()     { return this.status == OrderStatus.PAID; }
    public boolean isCancelled(){ return this.status == OrderStatus.CANCELLED; }
}