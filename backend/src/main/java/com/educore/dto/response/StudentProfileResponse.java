package com.educore.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentProfileResponse {

    private Long id;
    private String phone;

    // Name
    private String firstName;
    private String secondName;
    private String thirdName;
    private String fourthName;
    private String fullName;

    // Academic
    private String grade;
    private String governorate;
    private String area;
    private String schoolName;
    private String schoolType;
    private String educationDepartment;
    private String centerName;

    // Attendance
    private Boolean online;

    // Media
    private String profileImageUrl;

    // Meta
    private String studentCode;
    private String status;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
}
