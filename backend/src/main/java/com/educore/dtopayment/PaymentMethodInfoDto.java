package com.educore.dtopayment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * معلومات طريقة الدفع للـ frontend
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentMethodInfoDto {
    private String  code;
    private String  nameAr;
    private String  icon;
    private BigDecimal minAmount;
    private BigDecimal maxAmount;
    private boolean enabled;
    private boolean needsAdminApproval;
    private boolean instantActivation;
    private String  description;
}