package com.educore.student;

import com.educore.common.GlobalResponse;
import com.educore.common.FileUploadService;
import com.educore.dto.request.UpdateStudentProfileRequest;
import com.educore.dto.response.StudentProfileResponse;
import com.educore.security.JwtUserPrincipal;
import com.educore.student.dto.UpdateLocationRequest;
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

@Slf4j
@RestController
@RequestMapping("/api/student/profile")
@RequiredArgsConstructor
@PreAuthorize("hasRole('STUDENT')")
@Tag(name = "Student Profile", description = "بروفايل الطالب — عرض وتعديل")
public class StudentProfileController {

    private final StudentProfileService studentProfileService;
    private final FileUploadService     fileUploadService;

    // ─────────────────────────────────────────────────────────────
    // GET /api/student/profile
    // ─────────────────────────────────────────────────────────────

    @Operation(summary = "عرض بروفايل الطالب")
    @GetMapping
    public ResponseEntity<GlobalResponse<StudentProfileResponse>> getProfile(
            @AuthenticationPrincipal JwtUserPrincipal principal,
            @RequestHeader("X-Device-Id") String deviceId
    ) {
        log.debug("GET /api/student/profile — studentId: {}", principal.getUserId());

        StudentProfileResponse profile = studentProfileService.getProfile(principal.getUserId());

        return ResponseEntity.ok(GlobalResponse.<StudentProfileResponse>builder()
                .success(true)
                .message("تم جلب البروفايل بنجاح")
                .data(profile)
                .build());
    }

    // ─────────────────────────────────────────────────────────────
    // PUT /api/student/profile
    // ─────────────────────────────────────────────────────────────

    @Operation(summary = "تعديل بروفايل الطالب")
    @PutMapping
    public ResponseEntity<GlobalResponse<StudentProfileResponse>> updateProfile(
            @AuthenticationPrincipal JwtUserPrincipal principal,
            @RequestHeader("X-Device-Id") String deviceId,
            @Valid @RequestBody UpdateStudentProfileRequest request
    ) {
        log.debug("PUT /api/student/profile — studentId: {}", principal.getUserId());

        StudentProfileResponse updated = studentProfileService.updateProfile(
                principal.getUserId(), request);

        return ResponseEntity.ok(GlobalResponse.<StudentProfileResponse>builder()
                .success(true)
                .message("تم تحديث البروفايل بنجاح")
                .data(updated)
                .build());
    }

    // ─────────────────────────────────────────────────────────────
    // PATCH /api/student/profile/location
    // ─────────────────────────────────────────────────────────────

    @Operation(summary = "تحديث الموقع الجغرافي للطالب")
    @PatchMapping("/location")
    public ResponseEntity<GlobalResponse<StudentProfileResponse>> updateLocation(
            @AuthenticationPrincipal JwtUserPrincipal principal,
            @Valid @RequestBody UpdateLocationRequest request
    ) {
        StudentProfileResponse updated = studentProfileService
                .updateLocation(principal.getUserId(), request);
        return ResponseEntity.ok(GlobalResponse.<StudentProfileResponse>builder()
                .success(true).message("تم تحديث الموقع بنجاح").data(updated).build());
    }

    // ─────────────────────────────────────────────────────────────
    // POST /api/student/profile/image
    // ─────────────────────────────────────────────────────────────

    @Operation(summary = "رفع صورة البروفايل")
    @PostMapping(value = "/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<GlobalResponse<StudentProfileResponse>> uploadProfileImage(
            @AuthenticationPrincipal JwtUserPrincipal principal,
            @RequestHeader("X-Device-Id") String deviceId,
            @RequestPart("image") MultipartFile image
    ) {
        log.debug("POST /api/student/profile/image — studentId: {}", principal.getUserId());

        String imageUrl = fileUploadService.uploadProfilePicture(image);
        StudentProfileResponse updated = studentProfileService.updateProfileImage(
                principal.getUserId(), imageUrl);

        return ResponseEntity.ok(GlobalResponse.<StudentProfileResponse>builder()
                .success(true)
                .message("تم رفع الصورة بنجاح")
                .data(updated)
                .build());
    }

    // ─────────────────────────────────────────────────────────────
    // PATCH /api/student/profile/fcm-token
    // ─────────────────────────────────────────────────────────────

    /**
     * الموبايل يبعت الـ FCM token بعد كل login أو لما يتغير الـ token.
     * يُستدعى عند:
     *   - أول مرة تشغيل الـ app
     *   - عند تجديد الـ FCM token من Firebase
     *   - بعد تسجيل الدخول مباشرة
     */
    @Operation(summary = "تحديث FCM Token للإشعارات")
    @PatchMapping("/fcm-token")
    public ResponseEntity<GlobalResponse<Void>> updateFcmToken(
            @AuthenticationPrincipal JwtUserPrincipal principal,
            @RequestBody java.util.Map<String, String> body
    ) {
        String fcmToken = body.get("fcmToken");
        if (fcmToken == null || fcmToken.isBlank()) {
            return ResponseEntity.badRequest().body(
                    GlobalResponse.<Void>builder()
                            .success(false)
                            .message("fcmToken مطلوب")
                            .build());
        }

        studentProfileService.updateFcmToken(principal.getUserId(), fcmToken);

        log.info("FCM token updated for studentId={}", principal.getUserId());

        return ResponseEntity.ok(GlobalResponse.<Void>builder()
                .success(true)
                .message("تم تحديث FCM Token بنجاح")
                .build());
    }
}
