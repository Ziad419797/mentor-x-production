package com.educore.auth;

import com.educore.common.GlobalResponse;
import com.educore.dto.request.*;
import com.educore.dto.response.AuthResponse;
import com.educore.dto.response.OtpResponse;
import com.educore.hybrid.DeviceFingerprintUtil;
import com.educore.security.JwtData;
import com.educore.security.JwtService;
import com.educore.security.UserRole;
import com.educore.security.JwtUserPrincipal;
import com.educore.session.DatabaseSessionService;
import com.educore.student.Student;
import com.educore.student.StudentRepository;
import com.educore.student.StudentStatus;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final StudentRepository studentRepository;
    // Injected directly — AuthService no longer exposes these as public getters
    private final JwtService jwtService;
    private final DatabaseSessionService sessionService;

    // ─────────────────────────────────────────────────────────────
    // Entry Point
    // ─────────────────────────────────────────────────────────────

    @GetMapping("/entry-point")
    public ResponseEntity<GlobalResponse<Map<String, Object>>> entryPoint(
            HttpServletRequest request,
            @RequestHeader(value = "Authorization", required = false) String authHeader
    ) {
        try {
            String token    = extractToken(authHeader);
            String deviceId = DeviceFingerprintUtil.generate(request);
            Map<String, Object> data = authService.entryPoint(token, deviceId);
            return ResponseEntity.ok(GlobalResponse.success("", data));
        } catch (Exception e) {
            log.error("Entry point error: {}", e.getMessage(), e);
            Map<String, Object> fallback = Map.of(
                    "screen", "LOGIN",
                    "title", "حدث خطأ",
                    "message", "حدث خطأ في النظام، يرجى المحاولة مرة أخرى",
                    "showRegisterLink", true
            );
            return ResponseEntity.ok(GlobalResponse.success("", fallback));
        }
    }

    // ─────────────────────────────────────────────────────────────
    // Student Login
    // ─────────────────────────────────────────────────────────────

    @PostMapping("/student/login")
    public ResponseEntity<GlobalResponse<Map<String, Object>>> studentLogin(
            @RequestBody @Valid LoginRequest request,
            HttpServletRequest httpRequest
    ) {
        AuthResponse authResponse = authService.studentLogin(request, httpRequest);

        Map<String, Object> data = new HashMap<>();
        data.put("token",         authResponse.getToken());
        data.put("studentCode",   authResponse.getStudentCode());
        data.put("deviceId",      authResponse.getDeviceId());
        data.put("devicesCount",  authResponse.getDevicesCount());
        data.put("logoutCount",   authResponse.getLogoutCount());
        data.put("accountStatus", authResponse.getAccountStatus());
        data.put("refreshToken",  authResponse.getRefreshToken());
        data.put("redirectTo",    "/student/home");
        data.put("autoRedirect",  true);

        log.info("Student login successful");
        return ResponseEntity.ok(
                GlobalResponse.success(authResponse.getMessage(), data));
    }

    // ─────────────────────────────────────────────────────────────
    // Conflict QR — الطالب مسجل من جهاز تاني ويريد QR الحضور
    // ─────────────────────────────────────────────────────────────

    @Operation(summary = "الحصول على QR الحضور بدون login كامل — لحالة تعارض الأجهزة")
    @PostMapping("/student/conflict-qr")
    public ResponseEntity<GlobalResponse<Map<String, Object>>> getConflictQrToken(
            @RequestBody @Valid LoginRequest request) {
        Map<String, Object> data = authService.getConflictQrToken(request);
        return ResponseEntity.ok(
                GlobalResponse.success("تم إرجاع QR الحضور — أرِه للمدرس ليسكنه", data));
    }

    // ─────────────────────────────────────────────────────────────
    // Student Logout
    // ─────────────────────────────────────────────────────────────

    @PostMapping("/student/logout")
    public ResponseEntity<GlobalResponse<Void>> studentLogout(
            @RequestHeader(value = "Authorization", required = false) String authHeader
    ) {
        String token = extractToken(authHeader);
        if (token != null) {
            try {
                authService.studentLogout(token);
            } catch (Exception e) {
                log.warn("Logout service error (continuing): {}", e.getMessage());
            }
        }
        return ResponseEntity.ok(GlobalResponse.<Void>builder()
                .success(true)
                .message("تم تسجيل الخروج بنجاح")
                .build());
    }

    // ─────────────────────────────────────────────────────────────
    // General Logout (any role)
    // ─────────────────────────────────────────────────────────────

    @PostMapping("/logout")
    public ResponseEntity<GlobalResponse<Void>> logout(
            @RequestHeader(value = "Authorization", required = false) String authHeader
    ) {
        String token = extractToken(authHeader);
        if (token != null) {
            try {
                authService.studentLogout(token);
            } catch (Exception e) {
                log.debug("Logout service error: {}", e.getMessage());
            }
        }
        return ResponseEntity.ok(GlobalResponse.<Void>builder()
                .success(true)
                .message("تم تسجيل الخروج بنجاح")
                .build());
    }

    // ─────────────────────────────────────────────────────────────
    // Forgot Password (Students)
    // ─────────────────────────────────────────────────────────────

    @Operation(summary = "طلب استعادة كلمة المرور")
    @PostMapping("/forgot-password")
    public ResponseEntity<GlobalResponse<OtpResponse>> forgotPassword(
            @Valid @RequestBody StudentForgotPasswordRequest request) {
        OtpResponse response = authService.initiateStudentForgotPassword(request.getPhone());
        return ResponseEntity.ok(GlobalResponse.success(response.message(), response));
    }

    @Operation(summary = "إعادة إرسال كود التحقق")
    @PostMapping("/resend-otp")
    public ResponseEntity<GlobalResponse<Void>> resendOtp(
            @Valid @RequestBody ResendOtpRequest request) {
        authService.resendStudentOtp(request.getPhone());
        return ResponseEntity.ok(GlobalResponse.<Void>builder()
                .success(true).message("تم إعادة إرسال الكود").build());
    }

    @Operation(summary = "التأكد من كود التحقق")
    @PostMapping("/verify-otp")
    public ResponseEntity<GlobalResponse<Void>> verifyOtp(
            @Valid @RequestBody VerifyOtpRequest request) {
        authService.verifyStudentOtp(request.getPhone(), String.valueOf(request.getOtp()));
        return ResponseEntity.ok(GlobalResponse.<Void>builder()
                .success(true).message("كود التحقق صحيح").build());
    }

    @Operation(summary = "تعيين كلمة المرور الجديدة")
    @PostMapping("/reset-password")
    public ResponseEntity<GlobalResponse<Void>> resetPassword(
            @Valid @RequestBody StudentResetPasswordRequest request) {
        authService.resetStudentPassword(request.getPhone(), request.getNewPassword());
        return ResponseEntity.ok(GlobalResponse.<Void>builder()
                .success(true).message("تم تغيير كلمة المرور بنجاح، يمكنك تسجيل الدخول الآن").build());
    }

    // ─────────────────────────────────────────────────────────────
    // Staff Login
    // ─────────────────────────────────────────────────────────────

    @PostMapping("/staff/login")
    public ResponseEntity<GlobalResponse<AuthService.StaffLoginResponse>> staffLogin(
            @Valid @RequestBody LoginRequest request) {
        AuthService.StaffLoginResponse response = authService.staffLogin(request);
        return ResponseEntity.ok(GlobalResponse.success(response.message(), response));
    }

    // NOTE: Teacher login → TeacherAuthController @ /api/auth/teacher/login
    // NOTE: Parent login  → ParentLoginController @ /api/parent/start-login & /api/parent/complete-login

    // ─────────────────────────────────────────────────────────────
    // Session Status & Refresh
    // ─────────────────────────────────────────────────────────────

    @GetMapping("/session-status")
    public ResponseEntity<GlobalResponse<Map<String, Object>>> getSessionStatus(
            @RequestHeader(value = "Authorization", required = false) String authHeader
    ) {
        Map<String, Object> data = new HashMap<>();
        String token = extractToken(authHeader);

        if (token != null) {
            try {
                JwtData jwtData = jwtService.parseToken(token);
                data.put("hasSession", true);
                data.put("role",       jwtData.role());

                if (UserRole.STUDENT.name().equals(jwtData.role())) {
                    studentRepository.findById(jwtData.userId()).ifPresent(student -> {
                        data.put("status",      student.getStatus().name());
                        data.put("studentCode", student.getStudentCode());
                        data.put("redirectTo",  student.getStatus() == StudentStatus.PENDING
                                ? "/pending" : "/student/home");
                    });
                }
                return ResponseEntity.ok(GlobalResponse.success("يوجد جلسة نشطة", data));
            } catch (Exception e) {
                log.debug("Invalid token in session check: {}", e.getMessage());
                data.put("hasSession", false);
                data.put("redirectTo", "/login");
                return ResponseEntity.ok(GlobalResponse.success("انتهت الجلسة", data));
            }
        } else {
            data.put("hasSession",    false);
            data.put("redirectTo",    "/register");
            data.put("showLoginLink", true);
            return ResponseEntity.ok(GlobalResponse.success("لا توجد جلسة نشطة", data));
        }
    }

    @GetMapping("/pending-status")
    public ResponseEntity<GlobalResponse<Map<String, Object>>> getPendingStatus(
            @RequestHeader(value = "Authorization", required = false) String authHeader
    ) {
        String token = extractToken(authHeader);

        if (token == null) {
            return ResponseEntity.ok(GlobalResponse.success("", Map.of("redirectTo", "/login")));
        }

        try {
            JwtData jwtData = jwtService.parseToken(token);

            if (!UserRole.STUDENT.name().equals(jwtData.role())) {
                return ResponseEntity.ok(GlobalResponse.success("", Map.of("redirectTo", "/login")));
            }

            Map<String, Object> data = new HashMap<>();
            studentRepository.findById(jwtData.userId()).ifPresent(student -> {
                if (student.getStatus() == StudentStatus.PENDING) {
                    data.put("status",      "PENDING");
                    data.put("message",     "حسابك قيد المراجعة من قبل الإدارة");
                    data.put("studentCode", student.getStudentCode());
                    data.put("phone",       student.getPhone());
                } else {
                    data.put("redirectTo", "/login");
                }
            });
            return ResponseEntity.ok(GlobalResponse.success("", data));

        } catch (Exception e) {
            log.error("Error getting pending status: {}", e.getMessage());
            return ResponseEntity.ok(GlobalResponse.success("", Map.of("redirectTo", "/login")));
        }
    }

    /**
     * Token refresh endpoint.
     *
     * الـ Client يبعت الـ REFRESH token (مش الـ ACCESS token) في الـ Authorization header.
     * السيرفر يولد ACCESS token جديد ويحفظ session جديدة في الـ DB.
     *
     * Flow:
     *  1. Extract refresh token from Authorization header
     *  2. Parse it (refresh tokens have 30-day TTL, won't expire quickly)
     *  3. Verify tokenType == "REFRESH"
     *  4. Generate new ACCESS token with fresh sessionId
     *  5. Save new DB session
     *  6. Return new access token
     */
    @PostMapping("/refresh")
    public ResponseEntity<GlobalResponse<Map<String, Object>>> refreshSession(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestHeader(value = "X-Device-Id", required = false) String deviceIdHeader
    ) {
        String refreshToken = extractToken(authHeader);
        if (refreshToken == null) {
            return ResponseEntity.badRequest().body(
                    GlobalResponse.error("Refresh token مطلوب في Authorization header", 400));
        }

        try {
            // 1. Parse the refresh token (يرمي TokenExpiredException لو منتهي)
            JwtData refreshData = jwtService.parseToken(refreshToken);

            // 2. التحقق إنه REFRESH ومش ACCESS
            if (!"REFRESH".equals(refreshData.tokenType())) {
                return ResponseEntity.status(401).body(
                        GlobalResponse.error("يجب إرسال Refresh Token وليس Access Token", 401));
            }

            // 3. Device ID: من الـ header لو موجود، وإلا من الـ token
            String effectiveDeviceId = (deviceIdHeader != null && !deviceIdHeader.isBlank())
                    ? deviceIdHeader
                    : refreshData.deviceId();

            // 4. Session ID جديدة عشان نضمن عدم إعادة استخدام الـ session القديمة
            String newSessionId = java.util.UUID.randomUUID().toString();

            // 5. توليد ACCESS token جديد
            String newAccessToken = jwtService.generateToken(
                    refreshData.phone(), refreshData.role(), refreshData.userId(),
                    effectiveDeviceId, newSessionId
            );

            // 6. حفظ الـ session الجديدة في الـ DB (30 دقيقة)
            sessionService.saveSession(
                    refreshData.userId(), refreshData.role(),
                    newAccessToken, effectiveDeviceId, newSessionId, 30
            );

            log.info("Token refreshed for userId={} role={}", refreshData.userId(), refreshData.role());

            return ResponseEntity.ok(
                    GlobalResponse.success("تم تجديد الجلسة بنجاح", Map.of("token", newAccessToken)));

        } catch (com.educore.exception.TokenExpiredException e) {
            log.warn("Refresh token expired: {}", e.getMessage());
            return ResponseEntity.status(401).body(
                    GlobalResponse.error("انتهت صلاحية Refresh Token — يرجى تسجيل الدخول مجدداً", 401));
        } catch (com.educore.exception.InvalidTokenException e) {
            log.warn("Invalid refresh token: {}", e.getMessage());
            return ResponseEntity.status(401).body(
                    GlobalResponse.error("Refresh Token غير صالح", 401));
        }
    }

    // ─────────────────────────────────────────────────────────────
    // Force Logout
    // ─────────────────────────────────────────────────────────────

    @PostMapping("/force-logout-all")
    public ResponseEntity<GlobalResponse<Void>> forceLogoutAll(
            @RequestHeader(value = "Authorization", required = false) String authHeader
    ) {
        String token = extractToken(authHeader);
        if (token == null) {
            return ResponseEntity.badRequest().body(
                    GlobalResponse.error("التوكن مطلوب", 400));
        }

        JwtData jwtData = jwtService.parseToken(token);
        authService.forceLogoutAllSessions(jwtData.userId(), jwtData.role());

        return ResponseEntity.ok(GlobalResponse.<Void>builder()
                .success(true)
                .message("تم تسجيل الخروج من جميع الأجهزة بنجاح")
                .build());
    }

    // ─────────────────────────────────────────────────────────────
    // Current User Info
    // ─────────────────────────────────────────────────────────────

    @GetMapping("/me")
    public ResponseEntity<GlobalResponse<Map<String, Object>>> getCurrentUser(
            @AuthenticationPrincipal JwtUserPrincipal principal,
            @RequestParam(required = false) String deviceId
    ) {
        if (principal == null) {
            return ResponseEntity.status(401).body(
                    GlobalResponse.error("يرجى تسجيل الدخول أولاً", 401));
        }

        Map<String, Object> data = new HashMap<>();
        data.put("phone",    principal.getUsername());
        data.put("role",     principal.getRole());
        data.put("userId",   principal.getUserId());
        data.put("deviceId", deviceId);
        if (UserRole.STUDENT.name().equals(principal.getRole())) {
            studentRepository.findById(principal.getUserId()).ifPresent(student -> {
                data.put("studentCode", student.getStudentCode());
                data.put("fullName",    student.getFullName());
                data.put("status",      student.getStatus().name());
                data.put("enabled",     student.isEnabled());
                data.put("online",      student.getOnline());
                data.put("grade",       student.getGrade());
                data.put("centerName",  student.getCenterName());
                data.put("profileImageUrl", student.getProfileImageUrl());
            });
        }

        return ResponseEntity.ok(GlobalResponse.success("", data));
    }

    // ─────────────────────────────────────────────────────────────
    // Utilities
    // ─────────────────────────────────────────────────────────────

    @GetMapping("/check-phone/{phone}")
    public ResponseEntity<GlobalResponse<Map<String, Object>>> checkPhone(
            @PathVariable String phone) {
        boolean exists = studentRepository.findByPhone(phone).isPresent();
        Map<String, Object> data = exists
                ? Map.of("exists", true,  "action", "LOGIN")
                : Map.of("exists", false, "action", "REGISTER");
        String message = exists ? "الرقم مسجل بالفعل" : "يمكنك التسجيل بهذا الرقم";
        return ResponseEntity.ok(GlobalResponse.success(message, data));
    }

    /** Extracts the Bearer token from the Authorization header. */
    private String extractToken(String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }
}
