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
public class WalletDto {
    private Long    id;
    private Long    studentId;
    private String  studentName;
    private BigDecimal balance;
    private BigDecimal totalDeposited;
    private BigDecimal totalSpent;
    private BigDecimal totalRefunded;
    private Boolean isActive;
    private Boolean isVerified;
    private String  formattedBalance;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}