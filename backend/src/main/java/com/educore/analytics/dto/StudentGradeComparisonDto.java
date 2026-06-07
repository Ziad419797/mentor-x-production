package com.educore.analytics.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class StudentGradeComparisonDto {
    private Long studentId;
    private double myAvgPercentage;
    private double classAvgPercentage;
    private long myRank;
    private long totalStudents;
    private int totalAttempts;
}
