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
public class PaymentDto {
    private Long   id;
    private Long   orderId;
    private String orderNumber;
    private String transactionId;
    private String paymentMethod;
    private String status;
    private BigDecimal amount;
    private String currency;
    private String approvedBy;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;
}
