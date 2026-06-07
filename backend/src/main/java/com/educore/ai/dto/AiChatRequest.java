package com.educore.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * طلب Chat من الطالب إلى الـ AI service.
 */
@Data
public class AiChatRequest {

    @NotBlank(message = "السؤال مطلوب")
    @Size(min = 2, max = 1200, message = "السؤال بين 2 و 1200 حرف")
    private String question;

    /** الكورس اللي السؤال مرتبط بيه — اختياري للـ logging */
    private Long courseId;
}
