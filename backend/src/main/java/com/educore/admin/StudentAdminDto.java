package com.educore.admin;

import com.educore.student.Student;
import com.educore.student.StudentStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * StudentAdminDto — بيانات الطالب للـ Admin API
 * يُخفي: password, activeDeviceId, activeSessionId, lastDeviceFingerprint
 */
@Getter
@Builder
public class StudentAdminDto {

    private Long          id;
    private String        studentCode;
    private String        phone;
    private String        firstName;
    private String        secondName;
    private String        thirdName;
    private String        fourthName;
    private String        grade;
    private String        governorate;
    private String        area;
    private String        schoolName;
    private String        educationDepartment;
    private Boolean       online;
    private String        centerName;
    private String        profileImageUrl;
    private StudentStatus status;
    private boolean       enabled;
    private Integer       devicesCount;
    private Integer       loginCount;
    private Integer       logoutCount;
    private String        parentPhone;
    private LocalDateTime createdAt;
    private LocalDateTime lastActivityAt;

    public static StudentAdminDto from(Student s) {
        String parentPhone = (s.getParent() != null) ? s.getParent().getPhone() : null;
        return StudentAdminDto.builder()
                .id(s.getId())
                .studentCode(s.getStudentCode())
                .phone(s.getPhone())
                .firstName(s.getFirstName())
                .secondName(s.getSecondName())
                .thirdName(s.getThirdName())
                .fourthName(s.getFourthName())
                .grade(s.getGrade())
                .governorate(s.getGovernorate())
                .area(s.getArea())
                .schoolName(s.getSchoolName())
                .educationDepartment(s.getEducationDepartment())
                .online(s.getOnline())
                .centerName(s.getCenterName())
                .profileImageUrl(s.getProfileImageUrl())
                .status(s.getStatus())
                .enabled(s.isEnabled())
                .devicesCount(s.getDevicesCount())
                .loginCount(s.getLoginCount())
                .logoutCount(s.getLogoutCount())
                .parentPhone(parentPhone)
                .createdAt(s.getCreatedAt())
                .lastActivityAt(s.getLastActivityAt())
                .build();
    }
}
