package com.educore.wallet.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class WalletResponse {

    private Long        walletId;
    private Long        studentId;
    private String      studentName;
    private BigDecimal  balance;          // cached balance
    private BigDecimal  effectiveBalance; // real balance (non-expired)
    private BigDecimal  totalDeposited;
    private BigDecimal  totalSpent;
    private LocalDateTime updatedAt;

    private List<WalletTransactionResponse> recentTransactions;

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class WalletTransactionResponse {
        private Long        id;
        private String      transactionNumber;
        private String      type;        // DEPOSIT / PURCHASE / REFUND
        private String      status;      // PENDING / COMPLETED / ...
        private BigDecimal  amount;
        private BigDecimal  balanceAfter;
        private String      description;
        private LocalDateTime createdAt;
        private LocalDateTime expiresAt;  // null = لا تنتهي
        private boolean     expired;      // true لو expiresAt < now
    }
}
