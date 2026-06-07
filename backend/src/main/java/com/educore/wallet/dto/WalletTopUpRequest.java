package com.educore.wallet.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** الأدمن/المدرس يشحن محفظة طالب */
@Data
public class WalletTopUpRequest {

    @NotNull(message = "رقم الطالب مطلوب")
    private Long studentId;

    @NotNull(message = "المبلغ مطلوب")
    @DecimalMin(value = "1.00", message = "أقل مبلغ هو 1 جنيه")
    @DecimalMax(value = "100000.00", message = "أقصى مبلغ هو 100,000 جنيه")
    private BigDecimal amount;

    /** وصف اختياري — يظهر في كشف الحساب */
    @Size(max = 300, message = "الوصف لا يتجاوز 300 حرف")
    private String description;

    /**
     * تاريخ انتهاء الصلاحية — اختياري.
     * لو محدد → الرصيد ده بينتهي في هذا التاريخ.
     * null = رصيد دائم (لا ينتهي).
     */
    private LocalDateTime expiresAt;

    /**
     * بديل للـ expiresAt: عدد أيام الصلاحية من الآن.
     * لو الاثنين محددين → expiresAt بيكون الأولوية.
     */
    @Min(value = 1, message = "عدد الأيام لا يقل عن 1")
    private Integer validDays;
}
