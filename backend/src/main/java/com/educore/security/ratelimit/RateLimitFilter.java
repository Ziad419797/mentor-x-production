package com.educore.security.ratelimit;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * RateLimitFilter — يطبّق الـ Rate Limiting على endpoints الحساسة.
 *
 * Endpoints المغطاة:
 * ┌────────────────────────────────────┬──────────────┬─────────────┐
 * │ Endpoint                           │ Category     │ Limit       │
 * ├────────────────────────────────────┼──────────────┼─────────────┤
 * │ POST /api/auth/login               │ LOGIN        │ 10/15 دقيقة │
 * │ POST /api/auth/teacher/login       │ LOGIN        │ 10/15 دقيقة │
 * │ POST /api/auth/admin/login         │ ADMIN_LOGIN  │  5/15 دقيقة │
 * │ POST /api/auth/forgot-password     │ OTP_SEND     │  3/15 دقيقة │
 * │ POST /api/auth/resend-otp          │ OTP_SEND     │  3/15 دقيقة │
 * │ POST /api/auth/verify-otp          │ OTP_VERIFY   │  5/15 دقيقة │
 * │ POST /api/auth/reset-password      │ OTP_VERIFY   │  5/15 دقيقة │
 * │ POST /api/student/register/start   │ REGISTER     │  5/15 دقيقة │
 * │ POST /api/student/register/complete│ REGISTER     │  5/15 دقيقة │
 * │ POST /api/parent/start-login       │ OTP_SEND     │  3/15 دقيقة │
 * │ POST /api/parent/complete-login    │ OTP_VERIFY   │  5/15 دقيقة │
 * └────────────────────────────────────┴──────────────┴─────────────┘
 *
 * الـ Rate Limit بيتطبق per IP Address.
 *
 * Response لما يتجاوز الحد (HTTP 429):
 * {
 *   "success": false,
 *   "message": "تجاوزت عدد المحاولات المسموح بها. انتظر X ثانية.",
 *   "code": 429,
 *   "timestamp": "..."
 * }
 *
 * Headers المُضافة لكل response:
 *   X-RateLimit-Limit     → الحد الأقصى
 *   X-RateLimit-Remaining → الطلبات المتبقية
 *   X-RateLimit-Reset     → ثواني حتى الـ reset (لو مرفوض)
 *   Retry-After           → ثواني للانتظار (لو مرفوض)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimitFilter extends OncePerRequestFilter {

    private final RateLimitService rateLimitService;
    private final ObjectMapper     objectMapper;

    /* ══════════════════════════════════════════════════════
       doFilterInternal
    ══════════════════════════════════════════════════════ */

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String path   = request.getServletPath();
        String method = request.getMethod();

        // بس للـ POST requests على الـ auth endpoints
        RateLimitCategory category = resolveCategory(method, path);

        if (category == null) {
            filterChain.doFilter(request, response); // مش endpoint حساس
            return;
        }

        String clientIp = extractClientIp(request);
        RateLimitService.RateLimitResult result = rateLimitService.isAllowed(clientIp, category);

        // ── إضافة Rate Limit Headers لكل response ──
        response.setHeader("X-RateLimit-Limit",     String.valueOf(category.maxRequests));
        response.setHeader("X-RateLimit-Remaining", String.valueOf(result.remaining()));

        if (!result.allowed()) {
            response.setHeader("X-RateLimit-Reset", String.valueOf(result.retryAfterSeconds()));
            response.setHeader("Retry-After",       String.valueOf(result.retryAfterSeconds()));
            sendRateLimitResponse(response, result.retryAfterSeconds());
            return;
        }

        filterChain.doFilter(request, response);
    }

    /* ══════════════════════════════════════════════════════
       resolveCategory — URL → Category mapping
    ══════════════════════════════════════════════════════ */

    private RateLimitCategory resolveCategory(String method, String path) {
        if (!"POST".equalsIgnoreCase(method)) return null;

        return switch (path) {
            // ── Login ──
            case "/api/auth/login",
                 "/api/auth/teacher/login",
                 "/api/auth/staff/login"         -> RateLimitCategory.LOGIN;

            // ── Admin Login (أصعب) ──
            case "/api/auth/admin/login"          -> RateLimitCategory.ADMIN_LOGIN;

            // ── OTP Send ──
            case "/api/auth/forgot-password",
                 "/api/auth/resend-otp",
                 "/api/parent/start-login"        -> RateLimitCategory.OTP_SEND;

            // ── OTP Verify ──
            case "/api/auth/verify-otp",
                 "/api/auth/reset-password",
                 "/api/parent/complete-login"     -> RateLimitCategory.OTP_VERIFY;

            // ── Registration ──
            case "/api/student/register/start",
                 "/api/student/register/complete" -> RateLimitCategory.REGISTER;

            default -> null;
        };
    }

    /* ══════════════════════════════════════════════════════
       Client IP Extraction
    ══════════════════════════════════════════════════════ */

    /**
     * يجيب الـ IP الحقيقي للعميل.
     * بيأخذ بعين الاعتبار لو في Reverse Proxy (Nginx/Cloudflare).
     */
    private String extractClientIp(HttpServletRequest request) {
        // لو في Reverse Proxy بيبعت X-Forwarded-For
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            // ممكن تكون قائمة: "client_ip, proxy1_ip, proxy2_ip"
            return forwardedFor.split(",")[0].trim();
        }

        // Cloudflare بتبعت CF-Connecting-IP
        String cfIp = request.getHeader("CF-Connecting-IP");
        if (cfIp != null && !cfIp.isBlank()) {
            return cfIp.trim();
        }

        // الـ IP المباشر
        return request.getRemoteAddr();
    }

    /* ══════════════════════════════════════════════════════
       429 Response
    ══════════════════════════════════════════════════════ */

    private void sendRateLimitResponse(HttpServletResponse response, long retryAfterSeconds)
            throws IOException {

        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        Map<String, Object> body = Map.of(
                "success",   false,
                "message",   "تجاوزت عدد المحاولات المسموح بها. " +
                             "انتظر " + retryAfterSeconds + " ثانية قبل المحاولة مرة أخرى.",
                "code",      429,
                "timestamp", LocalDateTime.now().toString()
        );

        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
