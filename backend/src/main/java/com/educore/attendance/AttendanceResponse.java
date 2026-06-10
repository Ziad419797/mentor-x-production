package com.educore.attendance;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceResponse {

    private Long   id;

    private Long   studentId;
    private String studentName;
    private String studentCode;

    private Long   weekId;
    private String weekTitle;

    private LocalDateTime  attendedAt;
    private AttendanceType type;
    private AttendanceSource source;

    private String scannedBy;
    private String notes;

    /** اسم الجروب (للطالب السنتر) */
    private String groupName;

    /** اسم السنتر */
    private String centerName;
}
