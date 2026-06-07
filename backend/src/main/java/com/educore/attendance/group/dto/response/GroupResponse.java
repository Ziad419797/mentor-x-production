package com.educore.attendance.group.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter @Builder
public class GroupResponse {
    private Long    id;
    private String  title;
    private String  dayOfWeek;
    private String  meetingTime;
    private String  description;
    private Long    centerId;
    private String  centerName;
    private Long    levelId;
    private long    membersCount;
    private Integer maxCapacity;   // null = بدون حد
    private boolean isFull;        // membersCount >= maxCapacity
    private Long    openSessionId;
    private String  openSessionTitle;
    private boolean active;
    private LocalDateTime createdAt;
}
