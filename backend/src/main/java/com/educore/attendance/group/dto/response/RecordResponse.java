package com.educore.attendance.group.dto.response;

import com.educore.attendance.group.AttendanceStatus;
import com.educore.attendance.group.GroupAlertType;
import com.educore.attendance.group.ScanMethod;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter @Builder
public class RecordResponse {
    private Long            id;
    private Long            studentId;
    private String          studentName;
    private String          studentCode;
    private String          studentPhone;
    private String          studentCenter;  // سنتر الطالب المسجّل فيه

    private AttendanceStatus status;
    private ScanMethod       scanMethod;

    // ─── Alert ──────────────────────────────────────────────
    private boolean         hasAlert;
    private GroupAlertType  alertType;
    private String          alertMessage;

    // ─── تعليق ──────────────────────────────────────────────
    private String          teacherComment;

    private LocalDateTime   scannedAt;
}
