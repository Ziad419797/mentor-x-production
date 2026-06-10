package com.educore.DtoEnroll.response;


import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EnrollmentResponse {

    private Long id;
    private Long studentId;
    private String studentName;
    private String studentPhone;

    private Long courseId;
    private String courseTitle;
    private String courseDescription;
    private String courseImageUrl;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime enrolledAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime completedAt;

    private Double progress;
    private String status;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastAccessedAt;

    private Long totalWatchTimeSeconds;
    private Integer completedLessonsCount;
    private Integer totalLessonsCount;

    private Integer quizzesTaken;
    private Integer quizzesPassed;
    private Double averageQuizScore;

    private Integer assignmentsSubmitted;
    private Double averageAssignmentScore;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime expiresAt;

    private Long remainingDays;
    private Integer accessCount;
    private String notes;

    private Boolean isValidAccess;

    private String enrollmentType;
    private String createdBy;

    // إحصائيات إضافية
    private String completionPercentage;
    private String timeSpentFormatted;
}
