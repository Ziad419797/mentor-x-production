package com.educore.parent;

import com.educore.dto.request.UpdateParentProfileRequest;
import com.educore.dto.response.ParentProfileResponse;
import com.educore.exception.ResourceNotFoundException;
import com.educore.parent.dto.ChildSummaryDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ParentProfileService {

    private final ParentRepository parentRepository;

    // ─────────────────────────────────────────────────────────────
    // GET Profile
    // ─────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public ParentProfileResponse getProfile(Long parentId) {
        Parent parent = findParent(parentId);
        return toResponse(parent);
    }

    // ─────────────────────────────────────────────────────────────
    // UPDATE Profile (PATCH-style)
    // ─────────────────────────────────────────────────────────────

    @Transactional
    public ParentProfileResponse updateProfile(Long parentId, UpdateParentProfileRequest request) {
        Parent parent = findParent(parentId);

        if (StringUtils.hasText(request.getName())) {
            parent.setName(request.getName());
        }

        Parent saved = parentRepository.save(parent);
        log.info("Parent {} profile updated", parentId);
        return toResponse(saved);
    }

    // ─────────────────────────────────────────────────────────────
    // UPDATE Profile Image
    // ─────────────────────────────────────────────────────────────

    @Transactional
    public ParentProfileResponse updateProfileImage(Long parentId, String imageUrl) {
        Parent parent = findParent(parentId);
        parent.setProfileImageUrl(imageUrl);
        Parent saved = parentRepository.save(parent);
        log.info("Parent {} profile image updated", parentId);
        return toResponse(saved);
    }

    // ─────────────────────────────────────────────────────────────
    // GET Children list
    // ─────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<ChildSummaryDto> getChildren(Long parentId) {
        Parent parent = findParent(parentId);
        return parent.getStudents().stream()
                .map(s -> ChildSummaryDto.builder()
                        .id(s.getId())
                        .fullName(s.getFullName())
                        .studentCode(s.getStudentCode())
                        .grade(s.getGrade())
                        .governorate(s.getGovernorate())
                        .area(s.getArea())
                        .profileImageUrl(s.getProfileImageUrl())
                        .status(s.getStatus())
                        .online(s.getOnline())
                        .centerName(s.getCenterName())
                        .build())
                .collect(Collectors.toList());
    }

    // ─────────────────────────────────────────────────────────────
    // Private Helpers
    // ─────────────────────────────────────────────────────────────

    private Parent findParent(Long id) {
        return parentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ولي الأمر غير موجود: " + id));
    }

    private ParentProfileResponse toResponse(Parent parent) {
        List<ParentProfileResponse.StudentSummary> students = parent.getStudents().stream()
                .map(s -> ParentProfileResponse.StudentSummary.builder()
                        .id(s.getId())
                        .fullName(s.getFullName())
                        .studentCode(s.getStudentCode())
                        .grade(s.getGrade())
                        .build())
                .collect(Collectors.toList());

        return ParentProfileResponse.builder()
                .id(parent.getId())
                .phone(parent.getPhone())
                .name(parent.getName())
                .profileImageUrl(parent.getProfileImageUrl())
                .students(students)
                .createdAt(parent.getCreatedAt())
                .build();
    }
}
