package com.educore.dtocopon;

import com.educore.copon.CodeTargetType;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GenerateCodesRequest {

    @NotNull(message = "نوع الكود مطلوب")
    private CodeTargetType targetType;

    @Positive(message = "معرف الباقة يجب أن يكون موجباً")
    private Long categoryId;

    @Positive(message = "معرف الكورس يجب أن يكون موجباً")
    private Long courseId;

    @Positive(message = "معرف الحصة يجب أن يكون موجباً")
    private Long sessionId;

    /**
     * سعر الكود (اختياري) — null = مجاني
     * لو محدد، الطالب لازم يدفع هذا المبلغ عند تفعيل الكود.
     */
    @DecimalMin(value = "0.0", inclusive = false, message = "السعر يجب أن يكون أكبر من صفر")
    private BigDecimal price;

    @NotNull(message = "عدد الأكواد مطلوب")
    @Min(value = 1, message = "الحد الأدنى كود واحد")
    @Max(value = 500, message = "الحد الأقصى 500 كود في طلب واحد")
    private Integer count;

    @Min(value = 1, message = "الحد الأدنى استخدام واحد")
    private Integer maxUsesPerCode;

    @Future(message = "تاريخ الانتهاء يجب أن يكون في المستقبل")
    private LocalDateTime expiresAt;

    @Size(max = 100, message = "وصف الدفعة لا يتجاوز 100 حرف")
    private String batchLabel;

    @AssertTrue(message = "يجب تحديد المعرف المناسب حسب نوع الكود (باقة / كورس / حصة / محفظة)")
    public boolean isValidTarget() {
        if (targetType == null) return false;
        return switch (targetType) {
            case CATEGORY -> categoryId != null && courseId == null && sessionId == null;
            case COURSE   -> courseId != null && categoryId == null && sessionId == null;
            case SESSION  -> sessionId != null && categoryId == null && courseId == null;
            case WALLET   -> categoryId == null && courseId == null && sessionId == null;
        };
    }
}
