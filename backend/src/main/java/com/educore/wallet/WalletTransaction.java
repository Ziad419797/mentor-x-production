package com.educore.wallet;

import com.educore.payment.payment.PaymentMethod;
import com.educore.payment.payment.TransactionStatus;
import com.educore.payment.payment.TransactionType;
import jakarta.persistence.*;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.*;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;


@Entity
@Table(
        name = "wallet_transactions",
        indexes = {
                @Index(name = "idx_wtx_wallet",  columnList = "wallet_id"),
                @Index(name = "idx_wtx_type",    columnList = "transaction_type"),
                @Index(name = "idx_wtx_status",  columnList = "status"),
                @Index(name = "idx_wtx_created", columnList = "created_at"),
                @Index(name = "idx_wtx_ref",     columnList = "reference_id")
        }
)
@Cacheable
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE, region = "walletTxCache")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WalletTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /* ─────────────── المحفظة ─────────────── */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wallet_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_wtx_wallet"))
    private Wallet wallet;

    /* ─────────────── التفاصيل ─────────────── */
    @Column(nullable = false, unique = true, length = 50)
    private String transactionNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false, length = 20)
    private TransactionType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private TransactionStatus status = TransactionStatus.PENDING;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    /** الرصيد في المحفظة بعد العملية دي */
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal balanceAfter;

    /** معرف الطلب أو معرف المعاملة الخارجي */
    @Column(name = "reference_id", length = 100)
    private String referenceId;

    @Column(length = 500)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private PaymentMethod paymentMethod;

    /* ─────────────── التوقيتات ─────────────── */
    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime completedAt;

    /**
     * تاريخ انتهاء صلاحية الرصيد (للـ DEPOSIT فقط).
     * null = لا ينتهي أبداً.
     * لو انتهى التاريخ → الرصيد ده مش بيُحسب في الـ effective balance.
     */
    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    /** هل الرصيد ده منتهي الصلاحية؟ */
    public boolean isExpired() {
        return expiresAt != null && LocalDateTime.now().isAfter(expiresAt);
    }

    /* ═══════════════════════════════════════════════
       State Transitions
    ═══════════════════════════════════════════════ */

    public void complete() {
        this.status      = TransactionStatus.COMPLETED;
        this.completedAt = LocalDateTime.now();
    }

    public void fail(String reason) {
        this.status      = TransactionStatus.FAILED;
        this.description = reason;
    }

    public void cancel() {
        this.status = TransactionStatus.CANCELLED;
    }
}