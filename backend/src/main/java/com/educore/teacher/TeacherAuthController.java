package com.educore.teacher;

import com.educore.activitylog.ActivityLogService;
import com.educore.common.GlobalResponse;
import com.educore.dto.request.ForgotPasswordRequest;
import com.educore.dto.request.ResetPasswordRequest;
import com.educore.dto.request.TeacherLoginRequest;
import com.educore.dto.request.TeacherRegisterRequest;
import com.educore.dto.request.VerifyOtpRequest;
import com.educore.dto.response.TeacherAuthResponse;
import com.educore.security.JwtUserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/auth/teacher")
@RequiredArgsConstructor
@Tag(name = "Teacher Authentication", description = "تسجيل واستعادة كلمة مرور المعلم")
public class TeacherAuthController {

    private final TeacherAuthService teacherAuthService;
    private final ActivityLogService activityLogService;

    // ─────────────────────────────────────────────────────────────
    // Login
    // ─────────────────────────────────────────────────────────────

    @Operation(
        summary     = "تسجيل دخول المعلم",
        description = "يتحقق من بيانات الدخول ويعيد JWT صالح"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "تم تسجيل الدخول بنجاح"),
        @ApiResponse(responseCode = "401", description = "بيانات الدخول غير صحيحة أو الحساب غير مفعل")
    })
    @PostMapping("/login")
    public ResponseEntity<GlobalResponse<TeacherAuthResponse>> login(
            @Valid @RequestBody TeacherLoginRequest request) {

        log.info("POST /api/auth/teacher/login — phone: {}", request.getPhone());

        TeacherAuthResponse authResponse = teacherAuthService.login(request);
        try { activityLogService.log(authResponse.getName() != null ? authResponse.getName() : request.getPhone(), request.getPhone(), null, "TEACHER", "تسجيل دخول مدرس", "AUTH", null, "تسجيل دخول ناجح", null); } catch (Exception ignored) {}

        return ResponseEntity.ok(GlobalResponse.<TeacherAuthResponse>builder()
                .success(true)
                .message(authResponse.getMessage())
                .data(authResponse)
                .build());
    }

    // ─────────────────────────────────────────────────────────────
    // Registration
    // ─────────────────────────────────────────────────────────────

    @Operation(
        summary     = "تسجيل معلم جديد",
        description = "ينشئ حساب معلم جديد ويعيد JWT للاستخدام الفوري"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "تم إنشاء الحساب بنجاح"),
        @ApiResponse(responseCode = "400", description = "بيانات غير صحيحة أو رقم الهاتف مسجل مسبقاً")
    })
    @PostMapping("/register")
    public ResponseEntity<GlobalResponse<TeacherAuthResponse>> register(
            @Valid @RequestBody TeacherRegisterRequest request) {

        log.info("POST /api/auth/teacher/register — phone: {}", request.getPhone());

        TeacherAuthResponse authResponse = teacherAuthService.register(request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(GlobalResponse.<TeacherAuthResponse>builder()
                        .success(true)
                        .message(authResponse.getMessage())
                        .data(authResponse)
                        .build());
    }

    // ─────────────────────────────────────────────────────────────
    // Forgot Password — Step 1
    // ─────────────────────────────────────────────────────────────

    @Operation(summary = "طلب استعادة كلمة المرور", description = "يرسل كود OTP لرقم هاتف المعلم المسجل")
    @PostMapping("/forgot-password")
    public ResponseEntity<GlobalResponse<ForgotPasswordResponse>> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request) {

        ForgotPasswordResponse response = teacherAuthService.initiateForgotPasswordWithOtp(request.getPhone());

        return ResponseEntity.ok(GlobalResponse.<ForgotPasswordResponse>builder()
                .success(true)
                .message(response.getMessage())
                .data(response)
                .build());
    }

    // ─────────────────────────────────────────────────────────────
    // Forgot Password — Step 2
    // ─────────────────────────────────────────────────────────────

    @Operation(summary = "التحقق من كود الـ OTP")
    @PostMapping("/verify-otp")
    public ResponseEntity<GlobalResponse<Void>> verifyOtp(
            @Valid @RequestBody VerifyOtpRequest request) {

        teacherAuthService.verifyOtp(request.getPhone(), String.valueOf(request.getOtp()));

        return ResponseEntity.ok(GlobalResponse.<Void>builder()
                .success(true)
                .message("الكود صحيح، يمكنك تغيير كلمة المرور")
                .build());
    }

    // ─────────────────────────────────────────────────────────────
    // Forgot Password — Step 3
    // ─────────────────────────────────────────────────────────────

    @Operation(summary = "تعيين كلمة مرور جديدة")
    @PostMapping("/reset-password")
    public ResponseEntity<GlobalResponse<Void>> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request) {

        teacherAuthService.resetPassword(request.getPhone(), request.getNewPassword());

        return ResponseEntity.ok(GlobalResponse.<Void>builder()
                .success(true)
                .message("تم تغيير كلمة المرور بنجاح، يمكنك تسجيل الدخول الآن")
                .build());
    }

    // ─────────────────────────────────────────────────────────────
    // Logout
    // ─────────────────────────────────────────────────────────────

    @Operation(summary = "تسجيل خروج المعلم", description = "يُبطل التوكن الحالي ويحذف الجلسة من قاعدة البيانات")
    @PostMapping("/logout")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<GlobalResponse<Void>> logout(
            @AuthenticationPrincipal JwtUserPrincipal principal,
            HttpServletRequest request) {

        String token = extractToken(request);
        teacherAuthService.logout(principal.getUserId(), token);

        log.info("Teacher {} logged out", principal.getUserId());

        return ResponseEntity.ok(GlobalResponse.<Void>builder()
                .success(true)
                .message("تم تسجيل الخروج بنجاح")
                .build());
    }

    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return null;
    }
}
