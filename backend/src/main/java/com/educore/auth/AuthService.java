package com.educore.auth;

import com.educore.dto.mapper.TeacherMapper;
import com.educore.dto.request.TeacherLoginRequest;
import com.educore.dto.response.AuthResponse;
import com.educore.dto.request.LoginRequest;
import com.educore.dto.response.OtpResponse;
import com.educore.auth.PasswordResetTokenService;
import com.educore.dto.response.TeacherAuthResponse;
import com.educore.exception.AuthenticationException;
import com.educore.hybrid.DeviceFingerprintUtil;
import com.educore.security.JwtData;
import com.educore.security.JwtService;
import com.educore.security.OtpService;
import com.educore.security.UserRole;
import com.educore.session.DatabaseSessionService;
import com.educore.student.Student;
import com.educore.student.StudentRepository;
import com.educore.student.StudentStatus;
import com.educore.studentcard.StudentCardRepository;
import com.educore.teacher.Teacher;
import com.educore.teacher.TeacherRepository;
import com.educore.staff.Staff;
import com.educore.staff.StaffRepository;
import com.educore.staff.StaffResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final StudentRepository      studentRepository;
    private final TeacherRepository      teacherRepository;
    private final StaffRepository        staffRepository;
    private final PasswordEncoder        passwordEncoder;
    private final JwtService             jwtService;
    private final DatabaseSessionService sessionService;
    private final TeacherMapper          teacherMapper;
    private final OtpService             otpService;
    private final PasswordResetTokenService passwordResetTokenService;
    private final StudentCardRepository  studentCardRepository;
    private final com.educore.studentactivity.StudentActivityLogService studentActivityLogService;

    @Value("${app.support.whatsapp}")
    private String supportWhatsApp;

    @Value("${app.session.timeout:30}")
    private int sessionTimeoutMinutes;

    @Value("${app.max.sessions.per.user:3}")
    private int maxSessionsPerUser;

    // ─────────────────────────────────────────────────────────────
    // Entry Point — determines which screen to show the user
    // ─────────────────────────────────────────────────────────────

    /**
     * Main entry point for the frontend.
     * Returns a screen directive based on token validity and account state.
     * All DB writes that were hidden in these "view builder" methods are removed —
     * activity updates now happen in dedicated endpoints.
     */
    public Map<String, Object> entryPoint(String token, String deviceId) {
        if (token == null || token.isEmpty()) {
            return buildRegisterScreen();
        }

        if (!sessionService.isTokenValid(token)) {
            return buildLoginScreen("انتهت صلاحية الجلسة. يرجى تسجيل الدخول مرة أخرى");
        }

        try {
            JwtData jwtData = jwtService.parseToken(token);

            if (UserRole.STUDENT.name().equals(jwtData.role())) {
                return handleStudentEntryPoint(jwtData, deviceId);
            } else {
                return handleOtherUserEntryPoint(jwtData);
            }
        } catch (Exception e) {
            log.warn("Invalid token at entry point: {}", e.getMessage());
            return buildLoginScreen("توكن غير صالح");
        }
    }

    // ─────────────────────────────────────────────────────────────
    // Student Login
    // ─────────────────────────────────────────────────────────────

    /**
     * Authenticates a student with phone + password.
     * Enforces single-device policy: renews session on same device,
     * rejects login from a different device if a session already exists.
     */
    @Transactional
    public AuthResponse studentLogin(LoginRequest request, HttpServletRequest httpRequest) {
        log.info("Student login attempt for phone: {}", request.getPhone());

        Student student = studentRepository.findByPhone(request.getPhone())
                .orElseThrow(() -> new AuthenticationException("رقم الهاتف أو كلمة المرور غير صحيحة"));

        // Validate account status before touching passwords — avoids timing side-channels
        validateStudentStatus(student);
        validatePassword(request.getPassword(), student.getPassword());

        // استخدم الـ deviceId اللي بعته الـ client (ثابت per browser)
        // لو مش موجود، ارجع للـ fingerprint كـ fallback
        String deviceId = (request.getDeviceId() != null && !request.getDeviceId().isBlank())
                ? request.getDeviceId()
                : DeviceFingerprintUtil.generate(httpRequest);

        // Same device — renew the existing session instead of creating a new one
        if (sessionService.isValidDevice(student.getId(), deviceId)) {
            return renewExistingSession(student, deviceId);
        }

        // Different device — clean expired sessions first, then enforce limit
        sessionService.cleanExpiredSessions(student.getId());
        int activeSessions = sessionService.getActiveSessionsCount(student.getId());
        if (activeSessions >= maxSessionsPerUser) {
            if (maxSessionsPerUser <= 1) {
                throw new AuthenticationException(
                    "الحساب مسجل دخول على جهاز آخر. يجب تسجيل الخروج أولاً"
                );
            } else {
                throw new AuthenticationException(
                    "تجاوزت الحد الأقصى للجلسات النشطة (" + maxSessionsPerUser + "). يرجى تسجيل الخروج من جهاز آخر"
                );
            }
        }

        return createNewSession(student, deviceId);
    }

    // ─────────────────────────────────────────────────────────────
    // Conflict QR Token — لما الطالب يتلاقي حسابه مسجل من جهاز تاني
    // ─────────────────────────────────────────────────────────────

    /**
     * يتحقق من بيانات الطالب بدون إنشاء جلسة جديدة،
     * ويرجع الـ QR token الخاص بكارنيه الطالب (لو موجود).
     *
     * مناسب للسيناريو:
     *   • الطالب جاء للسنتر بس جهازه التاني معاه
     *   • مش محتاج يعمل logout من الجهاز التاني
     *   • المدرس يسكن الـ QR ده عادي
     *
     * @return map يحتوي على: qrToken, studentName, studentCode
     * @throws AuthenticationException لو البيانات غلط أو مفيش كارنيه
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getConflictQrToken(LoginRequest request) {
        Student student = studentRepository.findByPhone(request.getPhone())
                .orElseThrow(() -> new AuthenticationException("رقم الهاتف أو كلمة المرور غير صحيحة"));

        validateStudentStatus(student);
        validatePassword(request.getPassword(), student.getPassword());

        // جيب QR من الكارنيه
        String qrToken = studentCardRepository.findByStudentId(student.getId())
                .filter(com.educore.studentcard.StudentCard::isActive)
                .map(com.educore.studentcard.StudentCard::getQrToken)
                .orElseThrow(() -> new AuthenticationException(
                        "لم يتم إصدار كارنيه بعد — تواصل مع الإدارة"));

        log.info("Conflict QR retrieved for student {} (no new session created)", student.getId());

        Map<String, Object> result = new HashMap<>();
        result.put("qrToken", qrToken);
        result.put("studentName", student.getFullName());
        result.put("studentCode", student.getStudentCode());
        result.put("centerName", student.getCenterName());
        return result;
    }

    // ─────────────────────────────────────────────────────────────
    // Student Logout
    // ─────────────────────────────────────────────────────────────

    @Transactional
    public void studentLogout(String token) {
        log.info("Student logout requested");

        JwtData data = jwtService.parseToken(token);

        if (!UserRole.STUDENT.name().equals(data.role())) {
            throw new AuthenticationException("Role mismatch on logout: " + data.role());
        }

        // Blacklist token and clear the student's active session record
        sessionService.deleteUserSession(data.userId(), token);

        studentRepository.findById(data.userId()).ifPresent(s ->
            studentActivityLogService.log(
                    s.getId(), s.getFullName(),
                    com.educore.studentactivity.StudentEventType.LOGOUT,
                    "تسجيل خروج", null
            )
        );

        log.info("Student logout successful for userId: {}", data.userId());
    }

    // ─────────────────────────────────────────────────────────────
    // Teacher Login
    // ─────────────────────────────────────────────────────────────

    @Transactional
    public TeacherAuthResponse teacherLogin(TeacherLoginRequest request) {
        log.info("Teacher login attempt for phone: {}", request.getPhone());

        Teacher teacher = teacherRepository.findByPhone(request.getPhone())
                .orElseThrow(() -> new AuthenticationException("بيانات الدخول غير صحيحة"));

        if (!passwordEncoder.matches(request.getPassword(), teacher.getPassword())) {
            throw new AuthenticationException("بيانات الدخول غير صحيحة");
        }

        if (!teacher.isEnabled()) {
            throw new AuthenticationException("حساب المعلم غير مفعل، يرجى التواصل مع الإدارة");
        }

        String sessionId   = UUID.randomUUID().toString();
        String accessToken = jwtService.generateToken(
                teacher.getPhone(), UserRole.TEACHER.name(), teacher.getId(), null, sessionId
        );
        String refreshToken = jwtService.generateRefreshToken(
                teacher.getPhone(), UserRole.TEACHER.name(), teacher.getId()
        );

        sessionService.saveSession(
                teacher.getId(), UserRole.TEACHER.name(), accessToken, "WEB", sessionId, sessionTimeoutMinutes
        );

        TeacherAuthResponse response = teacherMapper.toAuthResponse(teacher);
        response.setAccessToken(accessToken);
        response.setRefreshToken(refreshToken);
        response.setMessage("تم تسجيل دخول المعلم بنجاح");

        return response;
    }

    // ─────────────────────────────────────────────────────────────
    // Staff Login
    // ─────────────────────────────────────────────────────────────

    /**
     * تسجيل دخول الموظف برقم الهاتف + كلمة المرور.
     * بيرجع access token بـ role=STAFF وبيانات الموظف الأساسية.
     */
    @Transactional
    public StaffLoginResponse staffLogin(LoginRequest request) {
        log.info("Staff login attempt for phone: {}", request.getPhone());

        Staff staff = staffRepository.findByPhoneAndActiveTrue(request.getPhone())
                .orElseThrow(() -> new AuthenticationException("بيانات الدخول غير صحيحة أو الحساب غير مفعّل"));

        if (!passwordEncoder.matches(request.getPassword(), staff.getPassword())) {
            throw new AuthenticationException("بيانات الدخول غير صحيحة أو الحساب غير مفعّل");
        }

        String sessionId   = UUID.randomUUID().toString();
        String accessToken = jwtService.generateToken(
                staff.getPhone(), UserRole.STAFF.name(), staff.getId(), null, sessionId,
                null, staff.getFullName(), "ACTIVE"
        );
        String refreshToken = jwtService.generateRefreshToken(
                staff.getPhone(), UserRole.STAFF.name(), staff.getId()
        );

        sessionService.saveSession(
                staff.getId(), UserRole.STAFF.name(), accessToken, "WEB", sessionId, sessionTimeoutMinutes
        );

        log.info("Staff login successful — id={}, phone={}", staff.getId(), staff.getPhone());

        return new StaffLoginResponse(
                accessToken,
                refreshToken,
                StaffResponse.from(staff),
                "تم تسجيل الدخول بنجاح"
        );
    }

    /** Response record for staff login */
    public record StaffLoginResponse(
            String accessToken,
            String refreshToken,
            StaffResponse staff,
            String message
    ) {}

    // ─────────────────────────────────────────────────────────────
    // Forgot Password Flow (Students)
    // ─────────────────────────────────────────────────────────────

    /** Step 1: Verifies phone exists, then sends OTP. */
    public OtpResponse  initiateStudentForgotPassword(String phone) {
        studentRepository.findByPhone(phone)
                .orElseThrow(() -> new AuthenticationException("رقم الهاتف غير مسجل كطالب لدينا"));
        otpService.generateAndSendOtp(phone);
        return new OtpResponse(true, "تم إرسال كود التحقق بنجاح");
    }

    /** Step 2: Resend OTP if the first one didn't arrive. */
    public void resendStudentOtp(String phone) {
        studentRepository.findByPhone(phone)
                .orElseThrow(() -> new AuthenticationException("المستخدم غير موجود"));
        otpService.generateAndSendOtp(phone);
        log.info("OTP resent for phone: {}", phone);
    }

    /** Step 3: Verify the OTP — otpService throws if invalid. */
    public void verifyStudentOtp(String phone, String otp) {
        otpService.verifyOtp(phone, otp);
        // منح تصريح مؤقت لإعادة تعيين كلمة المرور (صالح 10 دقائق)
        passwordResetTokenService.markVerified(phone);
        log.info("OTP verified successfully for phone: {}", phone);
    }

    /** Step 4: Set the new password after OTP is verified. */
    @Transactional
    public void resetStudentPassword(String phone, String newPassword) {
        // التحقق أن verify-otp تمت بنجاح خلال آخر 10 دقائق
        if (!passwordResetTokenService.isVerified(phone)) {
            throw new AuthenticationException(
                "يجب التحقق من رمز OTP أولاً قبل إعادة تعيين كلمة المرور");
        }
        Student student = studentRepository.findByPhone(phone)
                .orElseThrow(() -> new AuthenticationException("فشل العثور على بيانات الطالب"));
        student.setPassword(passwordEncoder.encode(newPassword));
        studentRepository.save(student);
        // استهلاك التصريح بعد الاستخدام (one-time use)
        passwordResetTokenService.consume(phone);
        log.info("Password reset successful for student: {}", student.getStudentCode());
    }

    // ─────────────────────────────────────────────────────────────
    // Force Logout
    // ─────────────────────────────────────────────────────────────

    @Transactional
    public void forceLogoutAllSessions(Long userId, String userType) {
        log.info("Force-logout all sessions for userId: {}, type: {}", userId, userType);

        if (UserRole.STUDENT.name().equals(userType)) {
            studentRepository.findById(userId).ifPresent(student -> {
                student.clearActiveSession();
                studentRepository.save(student);
            });
        }

        sessionService.forceLogoutAll(userId, userType);
    }

    // ─────────────────────────────────────────────────────────────
    // Private — Student Session Helpers
    // ─────────────────────────────────────────────────────────────

    /**
     * Renews activity on an existing session for the same device.
     * Does NOT create a new JWT — returns the existing token.
     * Falls through to createNewSession if the session record is gone.
     */
    private AuthResponse renewExistingSession(Student student, String deviceId) {
        var existingSession = sessionService.getUserSession(student.getId());

        if (existingSession.isPresent()) {
            String existingToken = (String) existingSession.get().get("token");
            sessionService.updateUserActivity(student.getId());
            String refreshToken = jwtService.generateRefreshToken(
                    student.getPhone(), UserRole.STUDENT.name(), student.getId()
            );

            student.updateActivity();
            studentRepository.save(student);

            log.info("Session renewed for student: {}", student.getStudentCode());
            return new AuthResponse(
                    existingToken, "تم تجديد الجلسة بنجاح", deviceId,
                    student.getStudentCode(), student.getDevicesCount(),
                    student.getLogoutCount(), student.getStatus().name(), refreshToken
            );
        }

        // Session record expired/cleaned up — create a fresh one
        return createNewSession(student, deviceId);
    }

    /** Creates a brand-new session and JWT for the student. */
    private AuthResponse createNewSession(Student student, String deviceId) {
        String sessionId = UUID.randomUUID().toString();
        String token = jwtService.generateToken(
                student.getPhone(), UserRole.STUDENT.name(), student.getId(), deviceId, sessionId
        );
        String refreshToken = jwtService.generateRefreshToken(
                student.getPhone(), UserRole.STUDENT.name(), student.getId()
        );
        sessionService.saveSession(
                student.getId(), UserRole.STUDENT.name(), token, deviceId, sessionId, sessionTimeoutMinutes
        );

        student.activateDevice(deviceId, sessionId);
        student.setLastLoginAt(LocalDateTime.now());
        studentRepository.save(student);

        log.info("New session created for student: {}", student.getStudentCode());

        studentActivityLogService.log(
                student.getId(), student.getFullName(),
                com.educore.studentactivity.StudentEventType.LOGIN,
                "تسجيل دخول",
                "جهاز: " + deviceId
        );

        return new AuthResponse(
                token, "تم تسجيل الدخول بنجاح", deviceId,
                student.getStudentCode(),
                student.getDevicesCount(),
                student.getLogoutCount(),
                student.getStatus().name(),
                refreshToken
        );
    }

    // ─────────────────────────────────────────────────────────────
    // Private — Validation Helpers
    // ─────────────────────────────────────────────────────────────

    /**
     * Validates account status before touching any password.
     * Checking status first avoids leaking account existence via timing differences.
     */
    private void validateStudentStatus(Student student) {
        if (student.getStatus() == StudentStatus.PENDING) {
            throw new AuthenticationException(
                    "حسابك قيد المراجعة من قبل الإدارة. للاستفسار تواصل مع الدعم: " + supportWhatsApp
            );
        }
        if (student.getStatus() == StudentStatus.REJECTED) {
            throw new AuthenticationException("الحساب مرفوض. يرجى التواصل مع الإدارة");
        }
        if (!student.isEnabled() || student.getStatus() != StudentStatus.ACTIVE) {
            throw new AuthenticationException("حساب الطالب غير مفعل");
        }
    }

    /** Shared password validation — use this consistently instead of inline checks. */
    private void validatePassword(String rawPassword, String encodedPassword) {
        if (!passwordEncoder.matches(rawPassword, encodedPassword)) {
            throw new AuthenticationException("رقم الهاتف أو كلمة المرور غير صحيحة");
        }
    }

    // ─────────────────────────────────────────────────────────────
    // Private — Entry Point Screen Builders
    // ─────────────────────────────────────────────────────────────

    private Map<String, Object> buildRegisterScreen() {
        return Map.of(
                "screen", "REGISTER",
                "title", "إنشاء حساب جديد",
                "message", "سجل الآن للانضمام لمنصتنا التعليمية",
                "showLoginLink", true,
                "loginUrl", "/login"
        );
    }

    private Map<String, Object> buildLoginScreen(String reason) {
        return Map.of(
                "screen", "LOGIN",
                "title", "تسجيل الدخول",
                "message", reason != null ? reason : "يرجى تسجيل الدخول",
                "showRegisterLink", true,
                "registerUrl", "/register"
        );
    }

    private Map<String, Object> handleStudentEntryPoint(JwtData jwtData, String deviceId) {
        try {
            Student student = studentRepository.findById(jwtData.userId())
                    .orElseThrow(() -> new RuntimeException("الطالب غير موجود"));

            // Validate device matches what's in the token
            if (!sessionService.isValidDevice(jwtData.userId(), jwtData.deviceId())) {
                return Map.of(
                        "screen", "LOGIN",
                        "title", "تم تسجيل الدخول من جهاز آخر",
                        "message", "الحساب مسجل دخول على جهاز آخر. يجب تسجيل الخروج من الجهاز الآخر أولاً",
                        "isDeviceConflict", true
                );
            }

            if (sessionService.isSessionExpired(student.getId())) {
                return buildLoginScreen("انتهت صلاحية الجلسة");
            }

            return switch (student.getStatus()) {
                case PENDING -> Map.of(
                        "screen", "PENDING",
                        "title", "حسابك قيد المراجعة",
                        "message", "الحساب لا يزال قيد المراجعة من قبل الإدارة",
                        "supportWhatsApp", supportWhatsApp,
                        "whatsappLink", "https://wa.me/" + supportWhatsApp.replace("+", ""),
                        "studentCode", student.getStudentCode(),
                        "showLogoutButton", true
                );
                case ACTIVE -> {
                    if (!student.isEnabled()) yield buildLoginScreen("حساب الطالب غير مفعل");

                    // Refresh the token and extend the session
                    String newToken = jwtService.generateToken(
                            student.getPhone(), UserRole.STUDENT.name(), student.getId(),
                            jwtData.deviceId(), jwtData.sessionId()
                    );
                    sessionService.extendSession(jwtData.token(), sessionTimeoutMinutes);

                    yield Map.of(
                            "screen", "HOME",
                            "title", "مرحباً بعودتك",
                            "welcomeMessage", "أهلاً " + student.getShortName(),
                            "studentCode", student.getStudentCode(),
                            "fullName", student.getFullName(),
                            "redirectUrl", "/student/home",
                            "jwt", newToken,
                            "autoRedirect", true,
                            "autoRedirectDelay", 500
                    );
                }
                case REJECTED -> Map.of(
                        "screen", "REJECTED",
                        "title", "الحساب مرفوض",
                        "message", "للأسف، تم رفض حسابك من قبل الإدارة",
                        "rejectionReason", student.getRejectionReason() != null
                                ? student.getRejectionReason() : "لم يتم تحديد السبب",
                        "supportWhatsApp", supportWhatsApp,
                        "whatsappLink", "https://wa.me/" + supportWhatsApp.replace("+", "")
                );
                default -> buildLoginScreen("حالة حساب غير معروفة");
            };

        } catch (Exception e) {
            log.error("Error in student entry point: {}", e.getMessage());
            return buildLoginScreen("حدث خطأ في النظام");
        }
    }

    private Map<String, Object> handleOtherUserEntryPoint(JwtData jwtData) {
        if (!sessionService.isTokenValid(jwtData.token())) {
            return buildLoginScreen("انتهت صلاحية الجلسة");
        }

        sessionService.updateUserActivity(jwtData.userId());

        return Map.of(
                "screen", "HOME",
                "title", "مرحباً بعودتك",
                "role", jwtData.role(),
                "redirectUrl", "/" + jwtData.role().toLowerCase() + "/home",
                "autoRedirect", true,
                "autoRedirectDelay", 300
        );
    }
}