package com.educore.analytics.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** بيانات الطالب الجغرافية — تُستخدم في خريطة الطلاب */
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class StudentLocationDto {

    private Long   studentId;
    private String studentName;
    private String studentCode;
    private String phone;
    private String governorate;
    private String area;
    private String mapAddress;
    private Double latitude;
    private Double longitude;
    private String grade;
    private Boolean online;
    private String  centerName;
}
