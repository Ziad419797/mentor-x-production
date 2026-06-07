package com.educore.admin;

import com.educore.dto.request.AdminLoginRequest;
import com.educore.dto.response.AdminAuthResponse;
import com.educore.security.JwtUserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import com.educore.common.GlobalResponse;

@Slf4j
@RestController
@RequestMapping("/api/auth/admin")
@RequiredArgsConstructor
@Tag(name = "Admin Authentication", description = "تسجيل دخول وخروج الأدمن")
public class AdminAuthController {

    private final AdminAuthService adminAuthService;

    // ─────────────────────────────────────────────────────────────
    // POST /api/auth/admin/login
    // ─────────────────────────────────────────────────────────────

    @Operation(summary = "تسجيل دخول الأدمن")
    @PostMapping("/login")
    public ResponseEntity<GlobalResponse<AdminAuthResponse>> login(
            @Valid @RequestBody AdminLoginRequest request) {

        log.info("POST /api/auth/admin/login — phone: {}", request.getPhone());

        AdminAuthResponse response = adminAuthService.login(request);

        return ResponseEntity.ok(GlobalResponse.<AdminAuthResponse>builder()
                .success(true)
                .message(response.getMessage())
                .data(response)
                .build());
    }

    // ─────────────────────────────────────────────────────────────
    // POST /api/auth/admin/logout
    // ─────────────────────────────────────────────────────────────

    @Operation(summary = "تسجيل خروج الأدمن")
    @PostMapping("/logout")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<GlobalResponse<Void>> logout(
            @AuthenticationPrincipal JwtUserPrincipal principal,
            HttpServletRequest request) {

        String token = extractToken(request);
        adminAuthService.logout(principal.getUserId(), token);

        return ResponseEntity.ok(GlobalResponse.<Void>builder()
                .success(true)
                .message("تم تسجيل الخروج بنجاح")
                .build());
    }

    // ─────────────────────────────────────────────────────────────
    // Private
    // ─────────────────────────────────────────────────────────────

    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        return (header != null && header.startsWith("Bearer ")) ? header.substring(7) : null;
    }
}
