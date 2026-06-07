package com.educore.questionbank.dto;

import com.educore.questionbank.DifficultyLevel;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class BankQuestionRequest {

    @NotNull(message = "topic id is required")
    private Long topicId;

    private String conceptTag;
    private String imageUrl;

    @NotBlank(message = "question text is required")
    private String description;

    // mark is optional — defaults to 1 in the service
    @Min(value = 1)
    private Integer mark;

    @NotNull(message = "options are required")
    @Size(min = 2, max = 6)
    private List<@NotBlank String> options;

    @NotBlank(message = "correct answer is required")
    private String correctAnswer;

    private DifficultyLevel difficulty = DifficultyLevel.MEDIUM;
}
