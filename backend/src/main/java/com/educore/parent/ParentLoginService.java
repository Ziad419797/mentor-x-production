package com.educore.parent;

import com.educore.dto.request.ParentCompleteLoginRequest;
import com.educore.dto.request.ParentStartLoginRequest;
import com.educore.dto.response.ParentCompleteLoginResponse;
import com.educore.dto.response.ParentStartLoginResponse;
import com.educore.exception.AuthenticationException;
import com.educore.security.JwtService;
import com.educore.security.UserRole;
import com.educore.security.OtpService;
import com.educore.session.DatabaseSessionService;
import com.educore.student.Student;
import com.educore.student.StudentRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.UUID;
import com.educore.exception.ResourceNotFoundException;

@Slf4j
@Service
@RequiredArgsConstructor
public class ParentLoginService {

    private final StudentRepository studentRepository;
    private final OtpService otpService;
    private final JwtService jwtService;
    private final DatabaseSessionService sessionService;

    @Value("${app.session.timeout:30}")
    private int sessionTimeoutMinutes;

    // ─────────────────────────────────────────────────────────────
    // Step 1: Send OTP to parent's phone
    // ─────────────────────────────────────────────────────────────

    /**
     * Looks up the student whose parent has the given phone number,
     * then sends an OTP to the parent's phone.
     * Returns a message only — no token is issued at this stage.
     */
    public ParentStartLoginResponse startLogin(ParentStartLoginRequest request) {
        log.info("Parent login step 1 — phone: {}", request.getParentPhone());

        // Find the student linked to this parent phone
        Student student = studentRepository.findByParentPhone(request.getParentPhone())
                .orElseThrow(() -> new AuthenticationException("رقم ولي الأمر غير مسجل"));

        Parent parent = student.getParent();
        if (parent == null) {
            throw new AuthenticationException("لا يوجد ولي أمر مرتبط بهذا الرقم");
        }

        // Send OTP to the parent's registered phone — no token returned here
        otpService.generateAndSendOtp(parent.getPhone());
        log.info("OTP sent to parent phone: {}", parent.getPhone());

        return new ParentStartLoginResponse("تم إرسال رمز التحقق إلى رقم ولي الأمر");
    }

    // ─────────────────────────────────────────────────────────────
    // Step 2: Verify OTP and issue token
    // ─────────────────────────────────────────────────────────────

    /**
     * Verifies the OTP submitted by the parent.
     * Only after successful OTP verification is a JWT token generated and a session saved.
     *
     * Bug fixes applied:
     * - Was using student.getId() as userId → now correctly uses parent.getId()
     * - Was not saving the session → now persists to DatabaseSessionService
     */
    @Transactional
    public ParentCompleteLoginResponse completeLogin(ParentCompleteLoginRequest request) {
        log.info("Parent login step 2 — phone: {}", request.getParentPhone());

        Student student = studentRepository.findByParentPhone(request.getParentPhone())
                .orElseThrow(() -> new AuthenticationException("رقم ولي الأمر غير مسجل"));

        Parent parent = student.getParent();
        if (parent == null) {
            throw new AuthenticationException("لا يوجد ولي أمر مرتبط بهذا الرقم");
        }

        // Verify OTP — otpService throws AuthenticationException if invalid or expired
        otpService.verifyOtp(parent.getPhone(), String.valueOf(request.getOtp()));
        log.info("OTP verified for parent: {}", parent.getPhone());

        // Generate token using parent.getId() (was incorrectly using student.getId() before)
        String sessionId = UUID.randomUUID().toString();
        String token = jwtService.generateToken(
                parent.getPhone(),
                UserRole.PARENT.name(),
                parent.getId(),   // ← fixed: parent's own ID, not the student's
                null,
                sessionId
        );

        // Persist the session (was missing before — parent could not be validated on subsequent requests)
        sessionService.saveSession(
                parent.getId(),
                UserRole.PARENT.name(),
                token,
                "WEB",
                sessionId,
                sessionTimeoutMinutes
        );

        log.info("Parent login successful — parentId: {}, studentCode: {}",
                parent.getId(), student.getStudentCode());

        return new ParentCompleteLoginResponse("تم تسجيل الدخول بنجاح", token);
    }

    // ─────────────────────────────────────────────────────────────
    // Logout: invalidate the session token
    // ─────────────────────────────────────────────────────────────

    public void logout(Long parentId, String token) {
        sessionService.deleteUserSession(parentId, token);
        log.info("Parent {} logged out", parentId);
    }
}
