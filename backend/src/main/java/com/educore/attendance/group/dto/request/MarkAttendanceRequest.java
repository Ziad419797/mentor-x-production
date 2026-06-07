package com.educore.attendance.group.dto.request;

import lombok.Getter;
import lombok.Setter;

/**
 * طلب تسجيل حضور طالب.
 * يُعبّئ إما qrToken (من scan) أو studentId (إدخال يدوي).
 * واحد منهم على الأقل مطلوب — يتحقق في الـ service.
 */
@Getter @Setter
public class MarkAttendanceRequest {

    /** التوكن من داخل QR Code الطالب */
    private String qrToken;

    /** ID الطالب (إدخال يدوي) */
    private Long studentId;

    /** تعليق اختياري وقت الـ scan */
    private String comment;
}
