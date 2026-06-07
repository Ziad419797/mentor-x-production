package com.educore.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class StartRegisterRequest {

    /** رقم هاتف الطالب فقط — باقي البيانات تُملأ في خطوة الإكمال */
    @NotBlank(message = "رقم هاتف الطالب مطلوب")
    @Pattern(regexp = "^01[0-9]{9}$", message = "رقم الهاتف غير صحيح")
    private String phone;

}
