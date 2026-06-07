package com.educore.analytics.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
public class ProgressSummaryDto {
    private Long courseId;
    private String courseTitle;
    private double progress;
    private LocalDateTime enrolledAt;
    private LocalDateTime lastAccessedAt;
    private double avgQuizScore;
    private int quizzesTaken;
    private int quizzesPassed;
    private EnrollmentStatus status;

    public enum EnrollmentStatus { ACTIVE, COMPLETED, CANCELLED }
}
