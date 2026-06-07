package com.educore.analytics.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class GradesByCourseDto {
    private Long courseId;
    private String courseTitle;
    private double avgQuizScore;
    private long enrollmentCount;
}
