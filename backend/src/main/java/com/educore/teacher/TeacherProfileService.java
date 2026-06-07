package com.educore.teacher;

import com.educore.course.CourseRepository;
import com.educore.dto.request.UpdateTeacherProfileRequest;
import com.educore.dto.response.TeacherProfileResponse;
import com.educore.enrollment.EnrollmentRepository;
import com.educore.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
@Slf4j
public class TeacherProfileService {

    private final TeacherRepository    teacherRepository;
    private final CourseRepository      courseRepository;
    private final EnrollmentRepository  enrollmentRepository;

    // ─────────────────────────────────────────────────────────────
    // Get Profile
    // ─────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public TeacherProfileResponse getProfile(Long teacherId) {
        Teacher teacher = findTeacher(teacherId);
        return toResponse(teacher);
    }

    // ─────────────────────────────────────────────────────────────
    // Update Profile
    // ─────────────────────────────────────────────────────────────

    @Transactional
    public TeacherProfileResponse updateProfile(Long teacherId, UpdateTeacherProfileRequest request) {

        Teacher teacher = findTeacher(teacherId);

        // Only update fields that are present in the request (PATCH-style logic)
        if (StringUtils.hasText(request.getName())) {
            teacher.setName(request.getName());
        }
        if (StringUtils.hasText(request.getSubject())) {
            teacher.setSubject(request.getSubject());
        }
        if (request.getBio() != null) {           // allow clearing bio with ""
            teacher.setBio(request.getBio());
        }
        if (request.getQuote() != null) {
            teacher.setQuote(request.getQuote());
        }
        if (request.getHomeCardImageUrl() != null) {
            teacher.setHomeCardImageUrl(request.getHomeCardImageUrl());
        }
        if (request.getLogoUrl() != null) {
            teacher.setLogoUrl(request.getLogoUrl());
        }
        if (request.getDarkLogoUrl() != null) {
            teacher.setDarkLogoUrl(request.getDarkLogoUrl());
        }
        if (request.getTeacherCardUrl() != null) {
            teacher.setTeacherCardUrl(request.getTeacherCardUrl());
        }
        if (request.getTeacherCardDarkUrl() != null) {
            teacher.setTeacherCardDarkUrl(request.getTeacherCardDarkUrl());
        }
        if (request.getFacebookUrl()  != null) teacher.setFacebookUrl(request.getFacebookUrl());
        if (request.getYoutubeUrl()   != null) teacher.setYoutubeUrl(request.getYoutubeUrl());
        if (request.getInstagramUrl() != null) teacher.setInstagramUrl(request.getInstagramUrl());
        if (request.getTiktokUrl()    != null) teacher.setTiktokUrl(request.getTiktokUrl());
        if (request.getWhatsappNumber() != null) teacher.setWhatsappNumber(request.getWhatsappNumber());
        if (request.getTelegramUrl()  != null) teacher.setTelegramUrl(request.getTelegramUrl());
        if (request.getHomeLayoutConfig() != null) teacher.setHomeLayoutConfig(request.getHomeLayoutConfig());
        if (StringUtils.hasText(request.getEmail())) {
            // Reject duplicate email (unless it belongs to this same teacher)
            if (teacherRepository.existsByEmail(request.getEmail())
                    && !request.getEmail().equalsIgnoreCase(teacher.getEmail())) {
                throw new IllegalArgumentException("البريد الإلكتروني مستخدم بالفعل من حساب آخر");
            }
            teacher.setEmail(request.getEmail());
        }

        teacherRepository.save(teacher);
        log.info("Profile updated for teacherId: {}", teacherId);

        return toResponse(teacher);
    }

    // ─────────────────────────────────────────────────────────────
    // Update Profile Image
    // ─────────────────────────────────────────────────────────────

    @Transactional
    public TeacherProfileResponse updateProfileImage(Long teacherId, String imageUrl) {
        Teacher teacher = findTeacher(teacherId);
        teacher.setProfileImageUrl(imageUrl);
        teacherRepository.save(teacher);
        log.info("Profile image updated for teacherId: {}", teacherId);
        return toResponse(teacher);
    }

    // ─────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────

    private Teacher findTeacher(Long id) {
        return teacherRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("المعلم غير موجود"));
    }

    public Teacher findTeacherById(Long id) {
        return findTeacher(id);
    }

    private TeacherProfileResponse toResponse(Teacher teacher) {
        return TeacherProfileResponse.builder()
                .id(teacher.getId())
                .name(teacher.getName())
                .phone(teacher.getPhone())
                .email(teacher.getEmail())
                .subject(teacher.getSubject())
                .bio(teacher.getBio())
                .quote(teacher.getQuote())
                .profileImageUrl(teacher.getProfileImageUrl())
                .homeCardImageUrl(teacher.getHomeCardImageUrl())
                .logoUrl(teacher.getLogoUrl())
                .darkLogoUrl(teacher.getDarkLogoUrl())
                .teacherCardUrl(teacher.getTeacherCardUrl())
                .teacherCardDarkUrl(teacher.getTeacherCardDarkUrl())
                .facebookUrl(teacher.getFacebookUrl())
                .youtubeUrl(teacher.getYoutubeUrl())
                .instagramUrl(teacher.getInstagramUrl())
                .tiktokUrl(teacher.getTiktokUrl())
                .whatsappNumber(teacher.getWhatsappNumber())
                .telegramUrl(teacher.getTelegramUrl())
                .homeLayoutConfig(teacher.getHomeLayoutConfig())
                .enabled(teacher.isEnabled())
                .createdAt(teacher.getCreatedAt())
                .build();
    }

    @Transactional(readOnly = true)
    public TeacherProfileResponse getPublicProfile() {
        long courseCount  = courseRepository.count();
        long studentCount = enrollmentRepository.countByActiveTrue();
        return teacherRepository.findAll().stream().findFirst()
                .map(t -> {
                    TeacherProfileResponse r = toResponse(t);
                    r.setCourseCount(courseCount);
                    r.setStudentCount(studentCount);
                    return r;
                })
                .orElseGet(() -> TeacherProfileResponse.builder().build());
    }
}
