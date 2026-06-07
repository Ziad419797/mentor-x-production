package com.educore.questionbank.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class QuestionTopicRequest {

    @NotBlank(message = "اسم الجزئية مطلوب")
    private String name;

    private String description;

    @NotNull(message = "رقم المحاضرة مطلوب")
    private Long sessionId;

    /** null = جزئية رئيسية، رقم = جزئية فرعية */
    private Long parentTopicId;

    private Integer orderNumber;
}
