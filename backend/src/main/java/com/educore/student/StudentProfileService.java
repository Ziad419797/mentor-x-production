package com.educore.student;

import com.educore.dto.request.UpdateStudentProfileRequest;
import com.educore.dto.response.StudentProfileResponse;
import com.educore.exception.ResourceNotFoundException;
import com.educore.student.dto.UpdateLocationRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class StudentProfileService {

    private final StudentRepository studentRepository;

    // ─────────────────────────────────────────────────────────────
    // GET Profile
    // ─────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public StudentProfileResponse getProfile(Long studentId) {
        Student student = findStudent(studentId);
        return toResponse(student);
    }

    // ─────────────────────────────────────────────────────────────
    // UPDATE Profile (PATCH-style — only non-null fields)
    // ─────────────────────────────────────────────────────────────

    @Transactional
    public StudentProfileResponse updateProfile(Long studentId, UpdateStudentProfileRequest request) {
        Student student = findStudent(studentId);

        if (StringUtils.hasText(request.getFirstName()))       student.setFirstName(request.getFirstName());
        if (StringUtils.hasText(request.getSecondName()))      student.setSecondName(request.getSecondName());
        if (StringUtils.hasText(request.getThirdName()))       student.setThirdName(request.getThirdName());
        if (StringUtils.hasText(request.getFourthName()))      student.setFourthName(request.getFourthName());
        if (StringUtils.hasText(request.getGrade()))           student.setGrade(request.getGrade());
        if (StringUtils.hasText(request.getGovernorate()))     student.setGovernorate(request.getGovernorate());
        if (StringUtils.hasText(request.getArea()))            student.setArea(request.getArea());
        if (StringUtils.hasText(request.getSchoolName()))      student.setSchoolName(request.getSchoolName());
        if (StringUtils.hasText(request.getEducationDepartment()))
            student.setEducationDepartment(request.getEducationDepartment());
        if (StringUtils.hasText(request.getCenterName()))      student.setCenterName(request.getCenterName());
        if (request.getOnline() != null)                       student.setOnline(request.getOnline());

        Student saved = studentRepository.save(student);
        log.info("Student {} profile updated", studentId);
        return toResponse(saved);
    }

    // ─────────────────────────────────────────────────────────────
    // UPDATE Location
    // ─────────────────────────────────────────────────────────────

    @Transactional
    public StudentProfileResponse updateLocation(Long studentId, UpdateLocationRequest request) {
        Student student = findStudent(studentId);

        if (request.getLatitude()   != null) student.setLatitude(request.getLatitude());
        if (request.getLongitude()  != null) student.setLongitude(request.getLongitude());
        if (StringUtils.hasText(request.getMapAddress())) student.setMapAddress(request.getMapAddress());

        Student saved = studentRepository.save(student);
        log.info("Student {} location updated: lat={}, lng={}", studentId,
                saved.getLatitude(), saved.getLongitude());
        return toResponse(saved);
    }

    // ─────────────────────────────────────────────────────────────
    // UPDATE Profile Image
    // ─────────────────────────────────────────────────────────────

    @Transactional
    public StudentProfileResponse updateProfileImage(Long studentId, String imageUrl) {
        Student student = findStudent(studentId);
        student.setProfileImageUrl(imageUrl);
        Student saved = studentRepository.save(student);
        log.info("Student {} profile image updated", studentId);
        return toResponse(saved);
    }

    // ─────────────────────────────────────────────────────────────
    // Private Helpers
    // ─────────────────────────────────────────────────────────────

    /**
     * بيحدث الـ FCM token للطالب — بيُستدعى من الموبايل عند كل login أو تغيير الـ token.
     */
    @Transactional
    public void updateFcmToken(Long studentId, String fcmToken) {
        Student student = findStudent(studentId);
        student.setFcmToken(fcmToken);
        studentRepository.save(student);
        log.debug("FCM token updated for studentId={}", studentId);
    }

    private Student findStudent(Long id) {
        return studentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("الطالب غير موجود: " + id));
    }

    private StudentProfileResponse toResponse(Student student) {
        return StudentProfileResponse.builder()
                .id(student.getId())
                .phone(student.getPhone())
                .firstName(student.getFirstName())
                .secondName(student.getSecondName())
                .thirdName(student.getThirdName())
                .fourthName(student.getFourthName())
                .fullName(student.getFullName())
                .grade(student.getGrade())
                .governorate(student.getGovernorate())
                .area(student.getArea())
                .schoolName(student.getSchoolName())
                .educationDepartment(student.getEducationDepartment())
                .centerName(student.getCenterName())
                .online(student.getOnline())
                .profileImageUrl(student.getProfileImageUrl())
                .studentCode(student.getStudentCode())
                .status(student.getStatus() != null ? student.getStatus().name() : null)
                .createdAt(student.getCreatedAt())
                .build();
    }
}
