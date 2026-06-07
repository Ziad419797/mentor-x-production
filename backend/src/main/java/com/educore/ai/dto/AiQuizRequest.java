package com.educore.ai.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

/**
 * طلب توليد أسئلة كويز من محتوى نصي.
 * بيُستخدم من المدرس أو الأدمن لتوليد أسئلة تلقائياً.
 */
@Data
public class AiQuizRequest {

    @NotBlank(message = "المحتوى مطلوب")
    @Size(min = 10, max = 8000, message = "المحتوى بين 10 و 8000 حرف")
    private String content;

    @Min(value = 1, message = "عدد الأسئلة لا يقل عن 1")
    @Max(value = 20, message = "عدد الأسئلة لا يزيد عن 20")
    private int numQuestions = 5;

    /** easy | medium | hard */
    @Pattern(regexp = "^(easy|medium|hard)$", message = "الصعوبة: easy أو medium أو hard")
    private String difficulty = "medium";

    /** الدرس المرتبط بالكويز — اختياري */
    private Long lessonId;
}
