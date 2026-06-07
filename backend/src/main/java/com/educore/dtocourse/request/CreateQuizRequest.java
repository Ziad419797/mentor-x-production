package com.educore.dtocourse.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class CreateQuizRequest {

    @NotNull
    private Long weekId;

    @NotBlank
    private String title;

    @NotNull
    @Min(value = 1)
    private Integer durationMinutes;

    @Min(value = 0) @Max(value = 100)
    private Integer passingScore = 50;

    @Min(value = 1)
    private Integer attemptsAllowed = 1;

    private String quizType = "SESSION_QUIZ";
    private String questionOrder = "FIXED";
    private Integer points = 0;
    private String prizeName;
    private Integer prizeScore;
    private Boolean improvable = false;
    private java.time.LocalDateTime startDate;
    private java.time.LocalDateTime endDate;

    // Optional — questions can be added later via POST /api/questions/quiz/{quizId}
    private List<CreateQuestionRequest> questions;
}
