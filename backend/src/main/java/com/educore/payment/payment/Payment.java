package com.educore.payment.payment;

import com.educore.payment.order.Order;
import jakarta.persistence.*;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.*;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/* ══════════════════════════════════════════════════════════════
   Payment — سجل الدفع الفعلي (واحد لكل طلب)
══════════════════════════════════════════════════════════════ */
@Entity
@Table(
        name = "payments",
        indexes = {
                @Index(name = "idx_pay_order",    columnList = "order_id"),
                @Index(name = "idx_pay_txn",      columnList = "transaction_id"),
                @Index(name = "idx_pay_status",   columnList = "status"),
                @Index(name = "idx_pay_method",   columnList = "payment_method"),
                @Index(name = "idx_pay_created",  columnList = "created_at")
        }
)
@Cacheable
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE, region = "paymentCache")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", unique = true,
            foreignKey = @ForeignKey(name = "fk_payment_order"))
    private Order order;
    // ═══════════════════════════════════════
    private BigDecimal discountAmount;   // ← أضف
    private String couponCode;           // ← أضف
    /** المعرف الخارجي من بوابة الدفع */
    @Column(unique = true, length = 100)
    private String transactionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false, length = 30)
    private PaymentMethod paymentMethod;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private PaymentStatus status = PaymentStatus.PENDING;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 10)
    @Builder.Default
    private String currency = "EGP";

    /** رد البوابة الخارجية كاملاً (JSON مثلاً) */
    @Column(length = 2000)
    private String gatewayResponse;

    @Column(length = 500)
    private String failureReason;

    /** للكاش والتحويل البنكي — اسم الأدمن اللي وافق */
    @Column(length = 100)
    private String approvedBy;

    /* ─────────────── التوقيتات ─────────────── */
    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime completedAt;

    /* ══════════════════════════════════════════════════════════════
       State Transitions
    ══════════════════════════════════════════════════════════════ */

    public void complete(String transactionId, String gatewayResponse) {
        this.status          = PaymentStatus.COMPLETED;
        this.transactionId   = transactionId;
        this.gatewayResponse = gatewayResponse;
        this.completedAt     = LocalDateTime.now();
    }

    public void fail(String reason) {
        this.status        = PaymentStatus.FAILED;
        this.failureReason = reason;
    }

    public void refund() {
        this.status = PaymentStatus.REFUNDED;
    }

    public void adminApprove(String adminUsername, String transactionId) {
        this.status      = PaymentStatus.COMPLETED;
        this.approvedBy  = adminUsername;
        this.transactionId = transactionId;
        this.completedAt = LocalDateTime.now();
    }

    /**
     * يُعيّن الحالة إلى PROCESSING بعد إنشاء Fawaterek invoice.
     * الـ transactionId هنا هو الـ invoiceId من فواتيرك.
     * سيتم الانتقال إلى COMPLETED عند استقبال الـ callback.
     */
    public void markAsProcessing(String fawaterekInvoiceId) {
        this.status        = PaymentStatus.PROCESSING;
        this.transactionId = fawaterekInvoiceId;
    }

    public boolean isCompleted()  { return this.status == PaymentStatus.COMPLETED;  }
    public boolean isPending()    { return this.status == PaymentStatus.PENDING;     }
    public boolean isProcessing() { return this.status == PaymentStatus.PROCESSING;  }
}