package com.educore.dtopayment;


import com.educore.payment.payment.PaymentMethod;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DepositRequestDto {

    @NotNull(message = "المبلغ مطلوب")
    @DecimalMin(value = "1.00", message = "الحد الأدنى للإيداع هو 1 جنيه")
    @DecimalMax(value = "50000.00", message = "الحد الأقصى للإيداع هو 50000 جنيه")
    private BigDecimal amount;

    @NotNull(message = "طريقة الدفع مطلوبة")
    private PaymentMethod paymentMethod;

    /* تفاصيل الدفع — نفس حقول PaymentRequestDto */
    private String cardNumber;
    private String cardHolderName;
    private String expiryMonth;
    private String expiryYear;
    private String cvv;

    @Pattern(regexp = "^01[0-9]{9}$", message = "رقم الموبايل غير صحيح")
    private String mobileNumber;



    /**
     * Idempotency Key — الفرونت إند يولده مرة واحدة لكل محاولة دفع
     * لو الطلب اتبعت مرتين بنفس الكي، ما يتحاسبش مرتين
     */
    private String idempotencyKey;
}
