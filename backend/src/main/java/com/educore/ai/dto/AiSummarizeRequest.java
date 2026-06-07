package com.educore.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * طلب تلخيص محتوى درس.
 */
@Data
public class AiSummarizeRequest {

    @NotBlank(message = "المحتوى مطلوب")
    @Size(min = 10, max = 8000, message = "المحتوى بين 10 و 8000 حرف")
    private String content;

    /** ar = عربي (افتراضي) | en = إنجليزي */
    @Pattern(regexp = "^(ar|en)$", message = "اللغة: ar أو en")
    private String language = "ar";

    /** الدرس المرتبط — اختياري */
    private Long lessonId;
}
