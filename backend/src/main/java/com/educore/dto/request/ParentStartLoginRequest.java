package com.educore.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * Request body for Step 1 of parent login — sending OTP.
 * Used by POST /api/parent/start-login
 */
@Data
public class ParentStartLoginRequest {

    @NotBlank(message = "رقم ولي الأمر مطلوب")
    @Pattern(regexp = "^01[0-9]{9}$", message = "رقم الهاتف غير صحيح")
    private String parentPhone;  // fixed: was "ParentPhone" (capital P — Java naming violation)
}
