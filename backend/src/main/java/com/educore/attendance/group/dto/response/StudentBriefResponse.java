package com.educore.attendance.group.dto.response;

import com.educore.attendance.group.AttendanceStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * ملخص الطالب في جروب معين — للطالب ولولي الأمر.
 */
@Getter @Builder
public class StudentBriefResponse {

    private Long   groupId;
    private String groupTitle;
    private String centerName;

    // ─── إحصاء سريع ──────────────────────────────────────────
    private int totalSessions;
    private int presentCount;
    private int absentCount;
    private int lateCount;
    private int excusedCount;
    private double attendancePercentage;  // % الحضور

    // ─── تاريخ الحصص التفصيلي ────────────────────────────────
    private List<SessionBrief> sessions;

    @Getter @Builder
    public static class SessionBrief {
        private Long            sessionId;
        private LocalDate       sessionDate;
        private Integer         sessionNumber;
        private String          sessionTitle;
        private AttendanceStatus status;          // حضر / غاب / ...
        private String          teacherComment;  // تعليق المدرس (لو موجود)
        private LocalDateTime   scannedAt;
    }
}
