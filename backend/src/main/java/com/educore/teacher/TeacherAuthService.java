package com.educore.teacher;

import com.educore.dto.mapper.TeacherMapper;
import com.educore.dto.request.TeacherLoginRequest;
import com.educore.dto.request.TeacherRegisterRequest;
import com.educore.dto.response.TeacherAuthResponse;
import com.educore.exception.AuthenticationException;
import com.educore.security.JwtService;
import com.educore.security.OtpService;
import com.educore.security.UserRole;
import com.educore.session.DatabaseSessionService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class TeacherAuthService {

    private final TeacherRepository teacherRepository;
    private final OtpService otpService;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final DatabaseSessionService sessionService;
    private final TeacherMapper teacherMapper;

    @Value("${app.session.timeout:30}")
    private int sessionTimeoutMinutes;

    // ─────────────────────────────────────────────────────────────
    // Login
    // ─────────────────────────────────────────────────────────────

    @Transactional
    public TeacherAuthResponse login(TeacherLoginRequest request) {
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

        log.info("Teacher login successful: phone={}", teacher.getPhone());
        return response;
    }

    // ─────────────────────────────────────────────────────────────
    // Registration
    // ─────────────────────────────────────────────────────────────

    /**
     * Registers a new teacher account and returns a ready-to-use JWT.
     * Teachers are auto-approved (enabled=true) — admin approval can be layered on later.
     */
    @Transactional
    public TeacherAuthResponse register(TeacherRegisterRequest request) {

        log.info("Teacher registration attempt for phone: {}", request.getPhone());

        // Uniqueness checks
        if (teacherRepository.existsByPhone(request.getPhone())) {
            throw new IllegalArgumentException("رقم الهاتف مسجل بالفعل");
        }

        if (StringUtils.hasText(request.getEmail())
                && teacherRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("البريد الإلكتروني مسجل بالفعل");
        }

        // Build entity
        Teacher teacher = new Teacher();
        teacher.setPhone(request.getPhone());
        teacher.setPassword(passwordEncoder.encode(request.getPassword()));
        teacher.setName(request.getName());
        teacher.setSubject(request.getSubject());
        teacher.setEmail(request.getEmail());
        teacher.setEnabled(true); // auto-approve; change to false when admin approval is added

        teacherRepository.save(teacher);

        // Issue JWT immediately so the teacher can log in right away
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
        response.setMessage("تم إنشاء حساب المعلم بنجاح");

        log.info("Teacher registered successfully: phone={}", teacher.getPhone());
        return response;
    }

    // ─────────────────────────────────────────────────────────────
    // Forgot Password — Step 1: Send OTP
    // ─────────────────────────────────────────────────────────────

    // ─────────────────────────────────────────────────────────────
// Forgot Password — Step 1: Send OTP (with OTP in response)
// ─────────────────────────────────────────────────────────────

    public ForgotPasswordResponse initiateForgotPasswordWithOtp(String phone) {
        Teacher teacher = teacherRepository.findByPhone(phone)
                .orElseThrow(() -> new AuthenticationException("رقم الهاتف غير مسجل لدينا"));

        String otp = otpService.generateAndSendOtpWithReturn(phone); // تعديل هذه الدالة لتعيد الـ OTP

        log.info("Forgot-password OTP sent for teacher phone: {}", phone);

        return ForgotPasswordResponse.builder()
                .success(true)
                .message("تم إرسال كود التحقق إلى هاتفك")
                .otp(otp)
                .build();
    }

    // ─────────────────────────────────────────────────────────────
    // Forgot Password — Step 2: Verify OTP
    // ─────────────────────────────────────────────────────────────

    public void verifyOtp(String phone, String otp) {
        otpService.verifyOtp(phone, otp);
        log.info("OTP verified successfully for teacher phone: {}", phone);
    }

    // ─────────────────────────────────────────────────────────────
    // Forgot Password — Step 3: Reset Password
    // ─────────────────────────────────────────────────────────────

    @Transactional
    public void resetPassword(String phone, String newPassword) {
        Teacher teacher = teacherRepository.findByPhone(phone)
                .orElseThrow(() -> new AuthenticationException("حدث خطأ، المستخدم غير موجود"));

        teacher.setPassword(passwordEncoder.encode(newPassword));
        teacherRepository.save(teacher);

        log.info("Password reset successfully for teacher phone: {}", phone);
    }

    // ─────────────────────────────────────────────────────────────
    // Logout
    // ─────────────────────────────────────────────────────────────

    public void logout(Long teacherId, String token) {
        sessionService.deleteUserSession(teacherId, token);
        log.info("Teacher {} logged out successfully", teacherId);
    }
}
