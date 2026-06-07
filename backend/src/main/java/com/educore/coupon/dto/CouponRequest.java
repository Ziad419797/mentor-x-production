package com.educore.coupon.dto;

import com.educore.coupon.CouponType;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class CouponRequest {

    @NotBlank(message = "كود الكوبون مطلوب")
    @Size(min = 3, max = 50, message = "الكود بين 3 و 50 حرف")
    @Pattern(regexp = "^[A-Za-z0-9_-]+$", message = "الكود يحتوي على حروف وأرقام فقط")
    private String code;

    @Size(max = 300)
    private String description;

    @NotNull(message = "نوع الخصم مطلوب")
    private CouponType type;

    @NotNull(message = "قيمة الخصم مطلوبة")
    @DecimalMin(value = "0.01", message = "قيمة الخصم لا تقل عن 0.01")
    private BigDecimal value;

    /** للـ PERCENTAGE فقط: الحد الأقصى للخصم بالجنيه */
    private BigDecimal maxDiscount;

    /** الحد الأدنى للسعر لتطبيق الكوبون */
    private BigDecimal minAmount;

    /** null = غير محدود */
    @Min(value = 1, message = "عدد مرات الاستخدام لا يقل عن 1")
    private Integer maxUses;

    /** null = لا ينتهي */
    private LocalDateTime expiresAt;
}
