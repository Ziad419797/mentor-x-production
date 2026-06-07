package com.educore.dtopayment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderDto {
    private Long   id;
    private String orderNumber;
    private Long   studentId;
    private String studentName;
    private String status;
    private BigDecimal subtotal;
    private BigDecimal discount;
    private BigDecimal tax;
    private BigDecimal total;
    private String formattedTotal;
    private String paymentMethod;
    private List<OrderItemDto> items;
    private PaymentDto payment;
    private LocalDateTime createdAt;
    private LocalDateTime paidAt;
}
