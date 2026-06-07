package com.educore.parent.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChildOverview {

    // معلومات الطالب
    private Long   studentId;
    private String studentName;
    private String studentCode;
    private String grade;
    private String governorate;
    private String schoolName;
    private String studyType;
    private String centerName;
    private String profileImageUrl;

    // إحصائيات الحضور
    private long totalAttendance;
    private long centerAttendance;
    private long onlineAttendance;

    // إحصائيات الدراسة
    private long activeEnrollments;
    private long completedLessons;
    private long inProgressLessons;
    private long lockedLessons;
}
