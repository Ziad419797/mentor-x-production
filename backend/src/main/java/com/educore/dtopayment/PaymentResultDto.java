package com.educore.dtopayment;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

/**
 * النتيجة النهائية لأي عملية دفع
 */
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class PaymentResultDto {
    private boolean success;
    private boolean pending;       // كاش / تحويل بنكي بينتظر موافقة
    private String  message;
    private String  orderNumber;
    private String  transactionId;
    private BigDecimal amountPaid;
    private int enrollmentsCreated;      // ← ده اسم الحقل الأصلي
    private String  redirectUrl;   // لو البوابة محتاجة redirect
    private BigDecimal originalAmount;   // ← أضف
    private BigDecimal finalAmount;      // ← أضف
    private BigDecimal discountAmount;   // ← أضف
    public static PaymentResultDto success(
            String orderNumber, String txnId, BigDecimal amount, int enrollments) {
        return PaymentResultDto.builder()
                .success(true).pending(false)
                .message("تم الدفع بنجاح")
                .orderNumber(orderNumber)
                .transactionId(txnId)
                .amountPaid(amount)
                .enrollmentsCreated(enrollments)
                .build();
    }
    // Factory methods
    public static PaymentResultDto successWithDiscount(String orderNumber, String txnId,
                                                       BigDecimal original, BigDecimal finalAmount,
                                                       BigDecimal discount, int enrollments) {
        return PaymentResultDto.builder()
                .success(true)
                .orderNumber(orderNumber)
                .transactionId(txnId)
                .originalAmount(original)
                .finalAmount(finalAmount)
                .discountAmount(discount)
                .enrollmentsCreated(enrollments)  // ← هنا enrollmentsCreated
                .build();
    }

    public static PaymentResultDto pending(String orderNumber, String message) {
        return PaymentResultDto.builder()
                .success(false).pending(true)
                .message(message)
                .orderNumber(orderNumber)
                .build();
    }

    public static PaymentResultDto failed(String message, String orderNumber) {
        return PaymentResultDto.builder()
                .success(false).pending(false)
                .message(message)
                .orderNumber(orderNumber)
                .build();
    }
}