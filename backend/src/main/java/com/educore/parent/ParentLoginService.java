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
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ParentLoginService {

    private final ParentRepository parentRepository;
    private final OtpService otpService;
    private final JwtService jwtService;
    private final DatabaseSessionService sessionService;

    @Value("${app.session.timeout:30}")
    private int sessionTimeoutMinutes;

    // ─────────────────────────────────────────────────────────────
    // Step 1: Send OTP to parent's phone
    // ─────────────────────────────────────────────────────────────

    public ParentStartLoginResponse startLogin(ParentStartLoginRequest request) {
        log.info("Parent login step 1 — phone: {}", request.getParentPhone());

        Parent parent = parentRepository.findByPhone(request.getParentPhone())
                .orElseThrow(() -> new AuthenticationException("رقم ولي الأمر غير مسجل"));

        otpService.generateAndSendOtp(parent.getPhone());
        log.info("OTP sent to parent phone: {}", parent.getPhone());

        return new ParentStartLoginResponse("تم إرسال رمز التحقق إلى رقم ولي الأمر");
    }

    // ─────────────────────────────────────────────────────────────
    // Step 2: Verify OTP and issue token
    // ─────────────────────────────────────────────────────────────

    @Transactional
    public ParentCompleteLoginResponse completeLogin(ParentCompleteLoginRequest request) {
        log.info("Parent login step 2 — phone: {}", request.getParentPhone());

        Parent parent = parentRepository.findByPhone(request.getParentPhone())
                .orElseThrow(() -> new AuthenticationException("رقم ولي الأمر غير مسجل"));

        // Verify OTP
        otpService.verifyOtp(parent.getPhone(), String.valueOf(request.getOtp()));
        log.info("OTP verified for parent: {}", parent.getPhone());

        String sessionId = UUID.randomUUID().toString();
        String token = jwtService.generateToken(
                parent.getPhone(),
                UserRole.PARENT.name(),
                parent.getId(),
                null,
                sessionId
        );

        sessionService.saveSession(
                parent.getId(),
                UserRole.PARENT.name(),
                token,
                "WEB",
                sessionId,
                sessionTimeoutMinutes
        );

        log.info("Parent login successful — parentId: {}", parent.getId());

        return new ParentCompleteLoginResponse("تم تسجيل الدخول بنجاح", token);
    }

    // ─────────────────────────────────────────────────────────────
    // Logout
    // ─────────────────────────────────────────────────────────────

    public void logout(Long parentId, String token) {
        sessionService.deleteUserSession(parentId, token);
        log.info("Parent {} logged out", parentId);
    }
}
