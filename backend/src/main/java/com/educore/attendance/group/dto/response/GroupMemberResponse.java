package com.educore.attendance.group.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter @Builder
public class GroupMemberResponse {
    private Long   studentId;
    private String studentName;
    private String studentCode;
    private String phone;
    private String centerName;   // سنتر الطالب (للمقارنة مع سنتر الجروب)
    private boolean online;      // هل الطالب أونلاين أصلاً
    private LocalDateTime joinedAt;
}
