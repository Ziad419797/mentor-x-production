package com.educore.parent.dto;

import com.educore.student.StudentStatus;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ChildSummaryDto {
    private Long   id;
    private String fullName;
    private String studentCode;
    private String grade;
    private String governorate;
    private String area;
    private String profileImageUrl;
    private StudentStatus status;
    private Boolean online;
    private String centerName;
}
