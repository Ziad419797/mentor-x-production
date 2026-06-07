package com.educore.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * RejectRequest — سبب رفض الطالب
 * مستخدم في POST /api/admin/students/{id}/reject
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RejectRequest {

    @NotBlank(message = "سبب الرفض مطلوب")
    @Size(min = 5, max = 500, message = "سبب الرفض يجب أن يكون بين 5 و 500 حرف")
    private String reason;
}
