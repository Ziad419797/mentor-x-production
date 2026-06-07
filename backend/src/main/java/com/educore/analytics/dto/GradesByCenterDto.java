package com.educore.analytics.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class GradesByCenterDto {
    private String centerName;
    private double avgQuizScore;
    private long studentCount;
}
