package com.educore.coupon.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/** نتيجة تطبيق الكوبون — الـ Frontend يعرضها للطالب قبل التأكيد */
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class ApplyCouponResponse {

    private String     couponCode;
    private String     couponDescription;
    private BigDecimal originalPrice;
    private BigDecimal discountAmount;
    private BigDecimal finalPrice;
    private String     discountSummary; // "خصم 20% = 40 جنيه"
}
