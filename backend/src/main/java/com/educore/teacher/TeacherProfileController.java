package com.educore.teacher;

//import com.educore.auth.GlobalResponse;
import com.educore.common.GlobalResponse;

import com.educore.common.FileUploadService;
import com.educore.dto.request.UpdateTeacherProfileRequest;
import com.educore.dto.response.TeacherProfileResponse;
import com.educore.security.JwtUserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/teacher/profile")
@RequiredArgsConstructor
//@PreAuthorize("hasRole('TEACHER')")
@Tag(name = "Teacher - Profile", description = "عرض وتحديث بيانات بروفايل المعلم")
//@SecurityRequirement(name = "bearerAuth")
public class TeacherProfileController {

    private final TeacherProfileService teacherProfileService;
    private final FileUploadService     fileUploadService;

    // ─────────────────────────────────────────────────────────────
    // GET /api/teacher/profile
    // ─────────────────────────────────────────────────────────────



    @Operation(summary = "عرض بروفايل المعلم")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "تم جلب البروفايل بنجاح"),
        @ApiResponse(responseCode = "401", description = "غير مصرح — يجب تسجيل الدخول"),
        @ApiResponse(responseCode = "404", description = "المعلم غير موجود")
    })
    @GetMapping
    public ResponseEntity<GlobalResponse<TeacherProfileResponse>> getProfile(
            @AuthenticationPrincipal JwtUserPrincipal principal) {

        log.info("GET /api/teacher/profile — teacherId: {}", principal.getUserId());

        TeacherProfileResponse profile = teacherProfileService.getProfile(principal.getUserId());

// الترتيب الصحيح في common.GlobalResponse هو: (message, data)
        return ResponseEntity.ok(GlobalResponse.success("تم جلب البروفايل بنجاح", profile));    }

    // ─────────────────────────────────────────────────────────────
    // PUT /api/teacher/profile
    // ─────────────────────────────────────────────────────────────

    @Operation(
        summary     = "تحديث بروفايل المعلم",
        description = "تحديث الاسم والتخصص والنبذة التعريفية والبريد الإلكتروني"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "تم التحديث بنجاح"),
        @ApiResponse(responseCode = "400", description = "بيانات غير صحيحة"),
        @ApiResponse(responseCode = "401", description = "غير مصرح")
    })
    @PutMapping
    public ResponseEntity<GlobalResponse<TeacherProfileResponse>> updateProfile(
            @AuthenticationPrincipal JwtUserPrincipal principal,
            @Valid @RequestBody UpdateTeacherProfileRequest request) {

        log.info("PUT /api/teacher/profile — teacherId: {}", principal.getUserId());

        TeacherProfileResponse updated = teacherProfileService.updateProfile(principal.getUserId(), request);

        return ResponseEntity.ok(GlobalResponse.success("تم تحديث البروفايل بنجاح",updated));
    }

    // ─────────────────────────────────────────────────────────────
    // GET /api/teacher/profile/public  — accessible by STUDENT
    // ─────────────────────────────────────────────────────────────

    @Operation(summary = "بروفايل المعلم العام — للطلاب")
    @GetMapping("/public")
    @PreAuthorize("hasAnyRole('STUDENT', 'TEACHER', 'ADMIN', 'STAFF')")
    public ResponseEntity<GlobalResponse<TeacherProfileResponse>> getPublicProfile() {
        log.info("GET /api/teacher/profile/public");
        return ResponseEntity.ok(GlobalResponse.success("تم جلب البروفايل بنجاح",
                teacherProfileService.getPublicProfile()));
    }

    // ─────────────────────────────────────────────────────────────
    // POST /api/teacher/profile/image
    // ─────────────────────────────────────────────────────────────

    @Operation(summary = "رفع صورة البروفايل")
    @PostMapping(value = "/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<GlobalResponse<TeacherProfileResponse>> uploadProfileImage(
            @AuthenticationPrincipal JwtUserPrincipal principal,
            @RequestPart("image") MultipartFile image) {

        log.info("POST /api/teacher/profile/image — teacherId: {}", principal.getUserId());

        String imageUrl = fileUploadService.uploadProfilePicture(image);
        TeacherProfileResponse updated = teacherProfileService.updateProfileImage(
                principal.getUserId(), imageUrl);

        return ResponseEntity.ok(GlobalResponse.success( "تم رفع الصورة بنجاح " ,updated));
    }

    // ─────────────────────────────────────────────────────────────
    // POST /api/teacher/profile/home-card-image
    // ─────────────────────────────────────────────────────────────

    @Operation(summary = "رفع صورة كارد الهوم")
    @PostMapping(value = "/home-card-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<GlobalResponse<TeacherProfileResponse>> uploadHomeCardImage(
            @AuthenticationPrincipal JwtUserPrincipal principal,
            @RequestPart("image") MultipartFile image) {

        log.info("POST /api/teacher/profile/home-card-image — teacherId: {}", principal.getUserId());
        String imageUrl = fileUploadService.uploadProfilePicture(image);

        UpdateTeacherProfileRequest req = new UpdateTeacherProfileRequest();
        req.setHomeCardImageUrl(imageUrl);
        TeacherProfileResponse updated = teacherProfileService.updateProfile(principal.getUserId(), req);

        return ResponseEntity.ok(GlobalResponse.success("تم رفع صورة الكارد بنجاح", updated));
    }

    // ─────────────────────────────────────────────────────────────
    // POST /api/teacher/profile/logo
    // ─────────────────────────────────────────────────────────────

    @Operation(summary = "رفع اللوجو")
    @PostMapping(value = "/logo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<GlobalResponse<TeacherProfileResponse>> uploadLogo(
            @AuthenticationPrincipal JwtUserPrincipal principal,
            @RequestPart("image") MultipartFile image) {

        log.info("POST /api/teacher/profile/logo — teacherId: {}", principal.getUserId());
        String imageUrl = fileUploadService.uploadProfilePicture(image);

        UpdateTeacherProfileRequest req = new UpdateTeacherProfileRequest();
        req.setLogoUrl(imageUrl);
        TeacherProfileResponse updated = teacherProfileService.updateProfile(principal.getUserId(), req);

        return ResponseEntity.ok(GlobalResponse.success("تم رفع اللوجو بنجاح", updated));
    }

    // ─────────────────────────────────────────────────────────────
    // POST /api/teacher/profile/dark-logo
    // ─────────────────────────────────────────────────────────────

    @Operation(summary = "رفع اللوجو للدارك مود")
    @PostMapping(value = "/dark-logo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<GlobalResponse<TeacherProfileResponse>> uploadDarkLogo(
            @AuthenticationPrincipal JwtUserPrincipal principal,
            @RequestPart("image") MultipartFile image) {

        log.info("POST /api/teacher/profile/dark-logo — teacherId: {}", principal.getUserId());
        String imageUrl = fileUploadService.uploadProfilePicture(image);

        UpdateTeacherProfileRequest req = new UpdateTeacherProfileRequest();
        req.setDarkLogoUrl(imageUrl);
        TeacherProfileResponse updated = teacherProfileService.updateProfile(principal.getUserId(), req);

        return ResponseEntity.ok(GlobalResponse.success("تم رفع لوجو الدارك مود بنجاح", updated));
    }

    // ─────────────────────────────────────────────────────────────
    // POST /api/teacher/profile/teacher-card
    // ─────────────────────────────────────────────────────────────

    @Operation(summary = "رفع كارد المدرس (لايت)")
    @PostMapping(value = "/teacher-card", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<GlobalResponse<TeacherProfileResponse>> uploadTeacherCard(
            @AuthenticationPrincipal JwtUserPrincipal principal,
            @RequestPart("image") MultipartFile image) {
        String imageUrl = fileUploadService.uploadProfilePicture(image);
        UpdateTeacherProfileRequest req = new UpdateTeacherProfileRequest();
        req.setTeacherCardUrl(imageUrl);
        TeacherProfileResponse updated = teacherProfileService.updateProfile(principal.getUserId(), req);
        return ResponseEntity.ok(GlobalResponse.success("تم رفع الكارد بنجاح", updated));
    }

    // ─────────────────────────────────────────────────────────────
    // POST /api/teacher/profile/teacher-card-dark
    // ─────────────────────────────────────────────────────────────

    @Operation(summary = "رفع كارد المدرس (دارك مود)")
    @PostMapping(value = "/teacher-card-dark", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<GlobalResponse<TeacherProfileResponse>> uploadTeacherCardDark(
            @AuthenticationPrincipal JwtUserPrincipal principal,
            @RequestPart("image") MultipartFile image) {
        String imageUrl = fileUploadService.uploadProfilePicture(image);
        UpdateTeacherProfileRequest req = new UpdateTeacherProfileRequest();
        req.setTeacherCardDarkUrl(imageUrl);
        TeacherProfileResponse updated = teacherProfileService.updateProfile(principal.getUserId(), req);
        return ResponseEntity.ok(GlobalResponse.success("تم رفع كارد الدارك مود بنجاح", updated));
    }

    // ─────────────────────────────────────────────────────────────
    // GET /api/teacher/profile/home-layout
    // ─────────────────────────────────────────────────────────────

    @Operation(summary = "جلب إعدادات ويدجتز الهوم")
    @GetMapping("/home-layout")
    public ResponseEntity<GlobalResponse<String>> getHomeLayout(
            @AuthenticationPrincipal JwtUserPrincipal principal) {

        Teacher teacher = teacherProfileService.findTeacherById(principal.getUserId());
        String config = teacher.getHomeLayoutConfig();
        return ResponseEntity.ok(GlobalResponse.success("تم جلب الإعدادات", config));
    }

    // ─────────────────────────────────────────────────────────────
    // POST /api/teacher/profile/home-layout
    // ─────────────────────────────────────────────────────────────

    @Operation(summary = "حفظ إعدادات ويدجتز الهوم")
    @PostMapping("/home-layout")
    public ResponseEntity<GlobalResponse<TeacherProfileResponse>> saveHomeLayout(
            @AuthenticationPrincipal JwtUserPrincipal principal,
            @RequestBody Map<String, Object> body) {

        String config = body.getOrDefault("config", "").toString();
        UpdateTeacherProfileRequest req = new UpdateTeacherProfileRequest();
        req.setHomeLayoutConfig(config);
        TeacherProfileResponse updated = teacherProfileService.updateProfile(principal.getUserId(), req);
        return ResponseEntity.ok(GlobalResponse.success("تم حفظ الإعدادات بنجاح", updated));
    }
}
