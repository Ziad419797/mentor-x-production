package com.educore.attendance.group.dto.request;

import lombok.Getter;
import lombok.Setter;

/**
 * إضافة طالب للجروب — بأي من الطرق الثلاثة.
 */
@Getter @Setter
public class AddStudentRequest {
    private Long   studentId;    // إدخال ID مباشر
    private String studentCode;  // كود الطالب (مطبوع على الكارنيه)
    private String qrToken;      // قراءة QR الطالب
}
