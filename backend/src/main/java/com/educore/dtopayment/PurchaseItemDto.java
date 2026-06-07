package com.educore.dtopayment;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
/**
 * عنصر داخل الطلب
 */
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class PurchaseItemDto {

    @NotNull(message = "نوع المنتج مطلوب")
    @Pattern(regexp = "COURSE|CATEGORY", message = "نوع المنتج: COURSE أو CATEGORY فقط")
    private String productType;

    @NotNull(message = "معرف المنتج مطلوب")
    @Positive(message = "معرف المنتج يجب أن يكون موجباً")
    private Long productId;
}