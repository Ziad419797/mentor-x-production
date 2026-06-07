package com.educore.parent.dashboard;

import com.educore.enrollment.EnrollmentStatus;
import com.educore.enrollment.EnrollmentType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChildEnrollmentInfo {

    private Long             enrollmentId;
    private Long             courseId;
    private String           courseTitle;
    private EnrollmentStatus status;
    private EnrollmentType   enrollmentType;
    private double           progress;
    private LocalDateTime    enrolledAt;
    private LocalDateTime    expiresAt;
    private LocalDateTime    completedAt;
}
