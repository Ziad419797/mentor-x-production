package com.educore.attendance.group.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter @Builder
public class SessionResponse {
    private Long      id;
    private Long      groupId;
    private String    groupTitle;
    private LocalDate sessionDate;
    private Integer   sessionNumber;
    private String    title;
    private boolean   open;
    private long      presentCount;
    private long      absentCount;
    private long      lateCount;
    private long      totalMembers;
    private LocalDateTime openedAt;
    private LocalDateTime closedAt;
    private LocalDateTime createdAt;
}
