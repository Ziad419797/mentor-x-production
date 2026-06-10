package com.educore.parent.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChildSummaryCard {

    private Long   studentId;
    private String studentName;
    private String studentCode;
    private String grade;
    private String studyType;       // "أونلاين" / "حضور في مركز"
    private String centerName;
    private String phone;
    private String profileImageUrl;

    private long activeEnrollments;
    private long completedLessons;
    private long totalAttendance;
    private long unreadNotifications; // إشعارات ولي الأمر المتعلقة بهذا الابن
}
