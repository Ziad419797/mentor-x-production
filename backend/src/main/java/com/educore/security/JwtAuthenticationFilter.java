package com.educore.security;

import com.educore.session.DatabaseSessionService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final DatabaseSessionService databaseSessionService;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String path   = request.getServletPath();
        String method = request.getMethod();
        log.debug("JWT Filter - {} {}", method, path);

        boolean isPublic = isPublicEndpoint(path);

        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            // No token: allow public endpoints through (anonymous), reject protected ones
            if (isPublic) {
                filterChain.doFilter(request, response);
                return;
            }
            log.warn("Missing or invalid Authorization header for path: {}", path);
            unauthorized(response, "مطلوب توكن للوصول إلى هذا المورد");
            return;
        }

        String token = header.substring(7);

        try {
            // 1. Validate token against DB (checks expiry + blacklist)
            if (!databaseSessionService.isTokenValid(token)) {
                log.warn("Invalid or blacklisted token for path: {}", path);
                unauthorized(response, "التوكن منتهي الصلاحية أو غير صالح");
                return;
            }

            // 2. Parse claims from the JWT
            JwtData jwtData = jwtService.parseToken(token);

            // 3. Extra device validation for students
            if (UserRole.STUDENT.name().equals(jwtData.role())) {
                if (!validateStudentSession(jwtData, request)) {
                    log.warn("Invalid student session for userId: {}", jwtData.userId());
                    unauthorized(response, "جلسة غير صالحة أو الجهاز غير مصرح");
                    return;
                }
            }

            // 4. Set authenticated principal in SecurityContext
            JwtUserPrincipal principal = new JwtUserPrincipal(jwtData);
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
            SecurityContextHolder.getContext().setAuthentication(authentication);

            log.debug("Authenticated: {} role: {}", jwtData.phone(), jwtData.role());
            filterChain.doFilter(request, response);

        } catch (Exception e) {
            log.error("JWT authentication error for path {}: {}", path, e.getMessage());
            unauthorized(response, "فشل في التحقق من الهوية");
        }
    }

    /**
     * Validates that a student request comes from the registered device.
     *
     * Three checks in order:
     *  1. X-Device-Id header must be present — no silent fallback.
     *  2. Header value must match the deviceId claim inside the JWT
     *     (prevents using a stolen token from a different device).
     *  3. An active, non-blacklisted session must exist in the DB for
     *     this exact (userId + deviceId) pair — catches force-logouts.
     *
     * Note: token expiry + blacklist are already handled by isTokenValid()
     * at step 1 of doFilterInternal, so we don't repeat them here.
     */
    private boolean validateStudentSession(JwtData jwtData, HttpServletRequest request) {
        try {
            // ── 1. Header must be present ─────────────────────────
            String deviceIdHeader = request.getHeader("X-Device-Id");
            if (deviceIdHeader == null || deviceIdHeader.isBlank()) {
                log.warn("Missing X-Device-Id header for studentId: {}", jwtData.userId());
                throw new SecurityException("X-Device-Id header مطلوب للطلاب");
            }

            // ── 2. Header must match JWT claim ────────────────────
            if (!deviceIdHeader.equals(jwtData.deviceId())) {
                log.warn("Device ID mismatch for studentId: {}. header={}, jwt={}",
                        jwtData.userId(), deviceIdHeader, jwtData.deviceId());
                throw new SecurityException("جهاز غير مصرح به - يرجى تسجيل الدخول مرة أخرى");
            }

            // ── 3. Active DB session must exist for (userId, deviceId) ──
            if (!databaseSessionService.isValidDevice(jwtData.userId(), deviceIdHeader)) {
                log.warn("No active session in DB for studentId: {}, deviceId: {}",
                        jwtData.userId(), deviceIdHeader);
                throw new SecurityException("الجلسة منتهية - يرجى تسجيل الدخول مرة أخرى");
            }

            return true;

        } catch (Exception e) {
            log.error("Error validating student session for userId {}: {}",
                    jwtData.userId(), e.getMessage());
            return false;
        }
    }

    /**
     * Determines whether this path is a public endpoint that skips JWT validation.
     * Must stay in sync with SecurityConfig.PUBLIC_ENDPOINTS.
     *
     * Note: assignment, quiz, and question endpoints are intentionally NOT public —
     * they were incorrectly listed as public before and have been removed.
     */
    private boolean isPublicEndpoint(String path) {
        return path.equals("/api/auth/student/login") ||
                path.equals("/api/auth/refresh") ||
                path.equals("/api/payment/methods") ||
                path.equals("/api/ai/health") ||           // AI service status — public
                path.equals("/api/auth/teacher/login") || // دخول المعلم
                path.equals("/api/auth/admin/login")   ||
                path.equals("/api/auth/staff/login")   ||
                path.startsWith("/api/auth/teacher/register") || // تسجيل المعلم الجديد (مهم)
                path.equals("/api/auth/forgot-password") ||
                path.equals("/api/auth/verify-otp")    ||
                path.equals("/api/auth/reset-password") ||
                path.startsWith("/api/auth/teacher/forgot-password") || // استعادة كلمة مرور المعلم
                path.startsWith("/api/auth/teacher/verify-otp") ||
                path.startsWith("/api/auth/teacher/reset-password") ||
                path.startsWith("/api/auth/check-phone/") ||
                path.startsWith("/api/student/register/") || // تسجيل الطالب
                path.equals("/api/parent/start-login")   ||
                path.equals("/api/parent/complete-login") ||
                path.equals("/api/courses/latest-with-enrollment") ||

                path.startsWith("/api/public/")          ||
                path.startsWith("/api/files/")           ||
                // كورسات: فقط قائمة الكورسات عامة — المحتوى (materials, weeks, sessions) يتطلب auth
                path.equals("/api/courses")              ||
                path.matches("/api/courses/[^/]+")       ||
                path.matches("/api/courses/category/.+") ||
                path.equals("/api/levels")               ||
                path.matches("/api/levels/[^/]+")        ||
                path.equals("/api/categories")           ||
                path.matches("/api/categories/[^/]+")    ||
                path.matches("/api/categories/level/.+") ||
                path.equals("/api/banners/active")       ||
                path.equals("/api/announcements/active") ||
                path.equals("/api/payment/fawaterek/callback") ||
                path.equals("/api/health")               ||
                path.startsWith("/swagger")              ||
                path.startsWith("/v3/api-docs")          ||
                path.startsWith("/swagger-ui")           ||
                path.startsWith("/actuator/health")      ||
                // Centers — public list for registration
                path.equals("/api/centers")              ||
                path.startsWith("/api/centers/")         ||
                // Attendance groups for registration (no token)
                path.equals("/api/student/register/groups") ||
                path.equals("/api/student/register/centers-with-groups") ||
                path.equals("/favicon.ico")              ||
                path.equals("/")                         ||
                path.startsWith("/error");
    }
    /** Skip the filter for OPTIONS requests (CORS preflight). */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return "OPTIONS".equalsIgnoreCase(request.getMethod());
    }

    /** Writes a 401 JSON response. */
    private void unauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(String.format(
                "{\"success\":false,\"error\":\"غير مصرح\",\"message\":\"%s\"}",
                message
        ));
    }
}
