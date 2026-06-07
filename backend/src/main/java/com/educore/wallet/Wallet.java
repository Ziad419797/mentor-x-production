package com.educore.wallet;

import com.educore.student.Student;
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
        name = "wallets",
        indexes = {
                @Index(name = "idx_wallet_student", columnList = "student_id"),
                @Index(name = "idx_wallet_active",  columnList = "is_active")
        }
)
@Cacheable
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE, region = "walletCache")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Wallet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /* ─────────────── علاقة الطالب ─────────────── */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", unique = true, nullable = false,
            foreignKey = @ForeignKey(name = "fk_wallet_student"))
    private Student student;

    /* ─────────────── الأرصدة ─────────────── */
    @Column(nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal balance = BigDecimal.ZERO;

    @Column(nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal totalDeposited = BigDecimal.ZERO;

    @Column(nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal totalSpent = BigDecimal.ZERO;

    @Column(nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal totalRefunded = BigDecimal.ZERO;

    /* ─────────────── الحالة ─────────────── */
    @Column(nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(nullable = false)
    @Builder.Default
    private Boolean isVerified = false;

    /* ─────────────── التوقيتات ─────────────── */
    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    /* ─────────────── الحماية بـ Version (Optimistic Lock) ─────────────── */
    @Version
    private Long version;

    /* ═══════════════════════════════════════════════
       Business Methods
    ═══════════════════════════════════════════════ */

    /**
     * إيداع مبلغ في المحفظة — يُستدعى داخل transaction فقط
     */
    public void credit(BigDecimal amount) {
        validatePositive(amount, "Deposit amount");
        this.balance        = this.balance.add(amount);
        this.totalDeposited = this.totalDeposited.add(amount);
    }

    /**
     * خصم مبلغ من المحفظة — يرمي InsufficientBalanceException لو الرصيد ناقص
     */
    public void debit(BigDecimal amount) {
        validatePositive(amount, "Debit amount");
        if (this.balance.compareTo(amount) < 0) {
            throw new InsufficientBalanceException(
                    String.format("Insufficient balance. Required: %.2f, Available: %.2f",
                            amount, this.balance));
        }
        this.balance   = this.balance.subtract(amount);
        this.totalSpent = this.totalSpent.add(amount);
    }

    /**
     * استرداد مبلغ (Refund)
     */
    public void refund(BigDecimal amount) {
        validatePositive(amount, "Refund amount");
        this.balance        = this.balance.add(amount);
        this.totalRefunded  = this.totalRefunded.add(amount);
        // نطرح من totalSpent بس من غير ما نوصل لسالب
        this.totalSpent     = this.totalSpent.subtract(amount).max(BigDecimal.ZERO);
    }

    public boolean hasSufficientBalance(BigDecimal amount) {
        return this.balance.compareTo(amount) >= 0;
    }

    private void validatePositive(BigDecimal amount, String fieldName) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(fieldName + " must be positive");
        }
    }

    /* ─── Custom Exception ─── */
    public static class InsufficientBalanceException extends RuntimeException {
        public InsufficientBalanceException(String message) { super(message); }
    }
}