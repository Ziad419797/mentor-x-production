package com.educore.dtocopon;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RedeemCodeRequest {

    @NotBlank(message = "الكود مطلوب")
    @Size(min = 11, max = 11, message = "الكود يجب أن يكون 11 حرفاً (مثال: ABCDE-FGHIJ)")
    @Pattern(regexp = "^[A-Z0-9]{5}-[A-Z0-9]{5}$",
            message = "صيغة الكود غير صحيحة (مثال: ABCDE-FGHIJ)")
    private String code;

    /** معرف الكورس اللي الطالب بيحاول يشتريه — مطلوب للتحقق من تطابق الكود */
    private Long courseId;
}