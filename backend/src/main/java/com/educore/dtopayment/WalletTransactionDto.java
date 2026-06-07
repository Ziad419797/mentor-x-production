package com.educore.dtopayment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WalletTransactionDto {
    private Long   id;
    private Long   walletId;
    private Long   studentId;
    private String studentName;
    private String studentCode;
    private String transactionNumber;
    private String type;
    private String status;
    private BigDecimal amount;
    private BigDecimal balanceAfter;
    private String formattedAmount;
    private String referenceId;
    private String description;
    private String paymentMethod;
    private String formattedDate;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;
}
