package com.educore.parent;

import com.educore.common.GlobalResponse;
import com.educore.common.FileUploadService;
import com.educore.dto.request.UpdateParentProfileRequest;
import com.educore.dto.response.ParentProfileResponse;
import com.educore.parent.dto.ChildSummaryDto;
import com.educore.security.JwtUserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/parent/profile")
@RequiredArgsConstructor
@PreAuthorize("hasRole('PARENT')")
@Tag(name = "Parent Profile", description = "بروفايل ولي الأمر — عرض وتعديل")
public class ParentProfileController {

    private final ParentProfileService parentProfileService;
    private final FileUploadService    fileUploadService;

    // ─────────────────────────────────────────────────────────────
    // GET /api/parent/profile
    // ─────────────────────────────────────────────────────────────

    @Operation(summary = "عرض بروفايل ولي الأمر مع قائمة أبنائه")
    @GetMapping
    public ResponseEntity<GlobalResponse<ParentProfileResponse>> getProfile(
            @AuthenticationPrincipal JwtUserPrincipal principal) {

        log.debug("GET /api/parent/profile — parentId: {}", principal.getUserId());

        ParentProfileResponse profile = parentProfileService.getProfile(principal.getUserId());

        return ResponseEntity.ok(GlobalResponse.<ParentProfileResponse>builder()
                .success(true)
                .message("تم جلب البروفايل بنجاح")
                .data(profile)
                .build());
    }

    // ─────────────────────────────────────────────────────────────
    // PUT /api/parent/profile
    // ─────────────────────────────────────────────────────────────

    @Operation(summary = "تعديل بروفايل ولي الأمر")
    @PutMapping
    public ResponseEntity<GlobalResponse<ParentProfileResponse>> updateProfile(
            @AuthenticationPrincipal JwtUserPrincipal principal,
            @Valid @RequestBody UpdateParentProfileRequest request) {

        log.debug("PUT /api/parent/profile — parentId: {}", principal.getUserId());

        ParentProfileResponse updated = parentProfileService.updateProfile(
                principal.getUserId(), request);

        return ResponseEntity.ok(GlobalResponse.<ParentProfileResponse>builder()
                .success(true)
                .message("تم تحديث البروفايل بنجاح")
                .data(updated)
                .build());
    }

    // ─────────────────────────────────────────────────────────────
    // POST /api/parent/profile/image
    // ─────────────────────────────────────────────────────────────

    @Operation(summary = "رفع صورة البروفايل")
    @PostMapping(value = "/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<GlobalResponse<ParentProfileResponse>> uploadProfileImage(
            @AuthenticationPrincipal JwtUserPrincipal principal,
            @RequestPart("image") MultipartFile image) {

        log.debug("POST /api/parent/profile/image — parentId: {}", principal.getUserId());

        String imageUrl = fileUploadService.uploadProfilePicture(image);
        ParentProfileResponse updated = parentProfileService.updateProfileImage(
                principal.getUserId(), imageUrl);

        return ResponseEntity.ok(GlobalResponse.<ParentProfileResponse>builder()
                .success(true)
                .message("تم رفع الصورة بنجاح")
                .data(updated)
                .build());
    }
}
