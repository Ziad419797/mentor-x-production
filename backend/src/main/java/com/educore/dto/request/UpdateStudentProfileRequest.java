package com.educore.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateStudentProfileRequest {

    @Size(min = 2, max = 50, message = "الاسم الأول بين 2 و 50 حرف")
    private String firstName;

    @Size(max = 50)
    private String secondName;

    @Size(max = 50)
    private String thirdName;

    @Size(max = 50)
    private String fourthName;

    @Size(max = 50)
    private String grade;

    @Size(max = 50)
    private String governorate;

    @Size(max = 100)
    private String area;

    @Size(max = 150)
    private String schoolName;

    @Size(max = 100)
    private String educationDepartment;

    @Size(max = 100)
    private String centerName;

    private Boolean online;
}
