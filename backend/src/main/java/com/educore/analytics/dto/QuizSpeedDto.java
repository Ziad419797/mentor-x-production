package com.educore.analytics.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
public class QuizSpeedDto {
    private Long quizId;
    private String quizTitle;
    private Integer score;
    private long durationSeconds;
    private LocalDateTime submittedAt;
}
