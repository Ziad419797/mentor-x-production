package com.educore.analytics.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class QuizPassRateDto {
    private Long quizId;
    private String quizTitle;
    private long totalAttempts;
    private long passedAttempts;
    private double passRate;  // percentage 0-100
}
