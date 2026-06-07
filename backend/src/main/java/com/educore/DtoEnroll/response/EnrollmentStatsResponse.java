package com.educore.DtoEnroll.response;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EnrollmentStatsResponse {

    private Long totalEnrollments;
    private Long activeEnrollments;
    private Long completedEnrollments;
    private Long expiredEnrollments;

    private Double averageProgress;
    private Long totalWatchTimeSeconds;

    private Map<String, Long> enrollmentsByCourse;
    private Map<String, Double> progressByCourse;

    private Integer totalQuizzesTaken;
    private Integer totalAssignmentsSubmitted;

    private String mostActiveCourse;
    private String bestPerformingCourse;
}