package com.educore.coupon.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/** الطالب يطبق كوبون عند الشراء */
@Data
public class ApplyCouponRequest {

    @NotBlank(message = "كود الكوبون مطلوب")
    private String code;

    @NotNull(message = "سعر الجلسة مطلوب لحساب الخصم")
    @DecimalMin(value = "0.01", message = "السعر لا يقل عن 0.01")
    private BigDecimal originalPrice;
}
