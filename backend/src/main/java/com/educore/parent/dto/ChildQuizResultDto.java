package com.educore.parent.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class ChildQuizResultDto {
    private Long   attemptId;
    private Long   quizId;
    private String quizTitle;
    private Integer score;
    private Integer totalMarks;
    private Boolean passed;
    private Integer correctAnswers;
    private Integer attemptNumber;
    private LocalDateTime submittedAt;
}
