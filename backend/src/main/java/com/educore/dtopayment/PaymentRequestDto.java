package com.educore.dtopayment;

import com.educore.payment.payment.PaymentMethod;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentRequestDto {

    @NotNull(message = "رقم الطلب مطلوب")
    @Positive(message = "رقم الطلب يجب أن يكون موجباً")
    private Long orderId;
    private String couponCode;  // ← أضف هذا الحقل

    @NotNull(message = "طريقة الدفع مطلوبة")
    private PaymentMethod paymentMethod;
    private BigDecimal amount;  // ← أضف (المبلغ بعد الخصم للبوابات الخارجية)

    /* ── بطاقة ائتمان ── */
    private String cardNumber;
    private String cardHolderName;
    private String expiryMonth;
    private String expiryYear;
    private String cvv;

    /* ── فوري / فودافون كاش ── */
    @Pattern(regexp = "^01[0-9]{9}$", message = "رقم الموبايل غير صحيح")
    private String mobileNumber;

    /* ── تحويل بنكي ── */
    private String bankName;
    private String accountNumber;
    private String transferReference;

    /* ── كاش ── */
    private String cashNotes;
}
