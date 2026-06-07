package com.educore.studentcard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentCardResponse {

    private Long   id;
    private Long   studentId;
    private String studentName;
    private String studentCode;

    /** الكود المطبوع على الكارنيه */
    private String cardCode;

    /**
     * التوكن المشفّر في الـ QR.
     * يُعرض فقط للطالب نفسه وللإدارة — لا يظهر في قوائم عامة.
     */
    private String qrToken;

    private boolean       active;
    private LocalDateTime issuedAt;
    private String        issuedBy;
}
