package com.educore.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Request body for Step 2 of parent login — OTP verification.
 * Used by POST /api/parent/complete-login
 */
@Data
public class ParentCompleteLoginRequest {

    @NotBlank(message = "رقم ولي الأمر مطلوب")
    @Pattern(regexp = "^01[0-9]{9}$", message = "رقم الهاتف غير صحيح")
    private String parentPhone;  // fixed: was "ParentPhone" (capital P — Java naming violation)

    @NotNull(message = "كود التحقق مطلوب")
    @Pattern(regexp = "^[0-9]{6}$", message = "كود التحقق يجب أن يكون 6 أرقام بالضبط")
    private String otp;
}
