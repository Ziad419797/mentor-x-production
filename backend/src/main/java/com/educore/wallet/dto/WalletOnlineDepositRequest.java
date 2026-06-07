package com.educore.wallet.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class WalletOnlineDepositRequest {

    @NotNull(message = "المبلغ مطلوب")
    @DecimalMin(value = "10.00", message = "الحد الأدنى للشحن 10 جنيه")
    @DecimalMax(value = "50000.00", message = "الحد الأقصى للشحن 50000 جنيه")
    private BigDecimal amount;

    @NotNull(message = "طريقة الدفع مطلوبة")
    private Integer paymentMethodId;
}
