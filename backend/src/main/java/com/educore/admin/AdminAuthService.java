package com.educore.admin;

import com.educore.dto.request.AdminLoginRequest;
import com.educore.dto.response.AdminAuthResponse;
import com.educore.exception.AuthenticationException;
import com.educore.security.JwtService;
import com.educore.security.UserRole;
import com.educore.session.DatabaseSessionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminAuthService {

    private final AdminRepository      adminRepository;
    private final PasswordEncoder      passwordEncoder;
    private final JwtService           jwtService;
    private final DatabaseSessionService sessionService;

    @Value("${app.session.timeout:30}")
    private int sessionTimeoutMinutes;

    // ─────────────────────────────────────────────────────────────
    // Login
    // ─────────────────────────────────────────────────────────────

    public AdminAuthResponse login(AdminLoginRequest request) {
        log.info("Admin login attempt — phone: {}", request.getPhone());

        Admin admin = adminRepository.findByPhone(request.getPhone())
                .orElseThrow(() -> new AuthenticationException("بيانات الدخول غير صحيحة"));

        if (!admin.isEnabled()) {
            throw new AuthenticationException("هذا الحساب معطّل، تواصل مع الدعم");
        }

        if (!passwordEncoder.matches(request.getPassword(), admin.getPassword())) {
            throw new AuthenticationException("بيانات الدخول غير صحيحة");
        }

        String sessionId   = UUID.randomUUID().toString();
        String accessToken = jwtService.generateToken(
                admin.getPhone(), UserRole.ADMIN.name(), admin.getId(), null, sessionId
        );

        sessionService.saveSession(
                admin.getId(), UserRole.ADMIN.name(), accessToken, "WEB", sessionId, sessionTimeoutMinutes
        );

        log.info("Admin login successful — adminId: {}", admin.getId());

        return AdminAuthResponse.builder()
                .id(admin.getId())
                .name(admin.getName())
                .phone(admin.getPhone())
                .role("ADMIN")
                .accessToken(accessToken)
                .message("تم تسجيل الدخول بنجاح")
                .build();
    }

    // ─────────────────────────────────────────────────────────────
    // Logout
    // ─────────────────────────────────────────────────────────────

    public void logout(Long adminId, String token) {
        sessionService.deleteUserSession(adminId, token);
        log.info("Admin {} logged out", adminId);
    }
}
