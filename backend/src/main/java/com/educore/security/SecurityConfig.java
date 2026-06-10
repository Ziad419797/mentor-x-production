package com.educore.security;

import com.educore.security.ratelimit.RateLimitFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final RateLimitFilter         rateLimitFilter;
    private final ObjectMapper            objectMapper;

    /**
     * Allowed CORS origins loaded from application config.
     * Set CORS_ALLOWED_ORIGINS env variable in production.
     * Never hardcode "*" with credentials=true — browsers reject it and it's a security hole.
     */
    @Value("${app.cors.allowed-origins}")
    private List<String> allowedOrigins;

    // ─── Endpoints that require NO authentication ──────────────────────────────
    private static final String[] PUBLIC_ENDPOINTS = {
            // Auth flows
            "/api/auth/student/login",
            "/api/student/register/**",
            "/api/auth/refresh",        // ⬅️ أضف هذا السطر
            "/api/auth/teacher/reset-password",  // ✅ تأكد من وجوده
            "/api/ai/health",           // AI service health check (public)
            "/api/auth/teacher/login",
            "/api/auth/admin/login",
            "/api/auth/staff/login",
            "/api/auth/staff/forgot-password",
            "/api/auth/staff/verify-otp",
            "/api/auth/staff/reset-password",
            "/api/auth/teacher/register",
            "/api/auth/teacher/forgot-password", // استعادة كلمة مرور المعلم
            "/api/auth/teacher/verify-otp",
            "/api/auth/teacher/reset-password",
            "/api/auth/forgot-password",
            "/api/auth/resend-otp",
            "/api/auth/verify-otp",
            "/api/auth/reset-password",
            "/api/auth/check-phone/**",
            "/api/auth/student/conflict-qr",
            "/api/auth/entry-point",
            "/api/payment/methods",
            // Parent OTP login (2-step flow — unauthenticated by nature)
            "/api/parent/start-login",
            "/api/parent/complete-login",
            // Public content
            "/api/public/**",
            "/api/health",
            // الكورسات والمحتوى: قائمة المستويات والكاتيجوري عامة، المحتوى الفعلي يتطلب تسجيل دخول
            "/api/levels",
            "/api/levels/{id}",
            "/api/categories",
            "/api/categories/{id}",
            "/api/categories/level/{levelId}",
            // قائمة الكورسات بدون محتوى (للعرض قبل الاشتراك)
            "/api/courses",
            "/api/courses/latest-with-enrollment",

            "/api/courses/{id}",
            "/api/courses/category/{categoryId}",
            "/api/files/**",
            // Centers — public list for registration & social links
            "/api/centers",
            "/api/centers/{id}",
            "/api/centers/governorate/**",
            // مواعيد الجروبات للتسجيل (بدون توكن)
            "/api/student/register/groups",
            "/api/student/register/centers-with-groups",
            // Banners & Announcements — active/public feed visible to everyone
            "/api/banners/active",
            "/api/announcements/active",

            // Fawaterek payment callback — يجي من سيرفر فواتيرك (بدون JWT)
            "/api/payment/fawaterek/callback",
            // Phase 1: Attendance & Cards — new endpoints (auth enforced via @PreAuthorize)
            // (no public exceptions needed — all require roles)
            // API documentation — consider restricting in production
            "/swagger-ui/**",
            "/v3/api-docs/**",
            "/swagger-ui.html",
            "/swagger-resources/**",
            "/webjars/**",
            // Actuator — restrict further via firewall/IP in production
            "/actuator/health",
            // Static
            "/css/**", "/js/**", "/images/**", "/favicon.ico", "/"
    };

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                // CSRF is not needed for stateless JWT APIs
                .csrf(AbstractHttpConfigurer::disable)

                // CORS uses explicit allowed origins from application config
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // Stateless — no HTTP sessions created or used
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                .authorizeHttpRequests(auth -> auth
                        // Public endpoints — no token required
                        .requestMatchers(PUBLIC_ENDPOINTS).permitAll()
                        .requestMatchers("/api/analytics/**").authenticated() // أي حد معاه توكن يدخل الفلتر
                        // Quiz and leaderboard — students and teachers only
                        .requestMatchers("/api/quizzes/**").hasAnyRole("STUDENT", "TEACHER")
                        .requestMatchers("/api/quiz-attempts/**").hasAnyRole("STUDENT", "TEACHER")
                        .requestMatchers("/api/leaderboard/**").hasAnyRole("STUDENT", "TEACHER")

                        // Questions — teacher management only
                        .requestMatchers("/api/questions/**").hasAnyRole("TEACHER","STUDENT")
                        .requestMatchers("/api/teacher/profile/public").hasAnyRole("STUDENT", "TEACHER", "STAFF", "ADMIN")
                        .requestMatchers("/api/teacher/profile/**").hasAnyRole("TEACHER", "STAFF")
                        .requestMatchers("/api/teacher/profile").hasAnyRole("TEACHER", "STAFF")
                        // Question Bank — topics, questions, and exam generation (TEACHER/ADMIN)
                        .requestMatchers("/api/question-bank/topics/week/**").hasAnyRole("TEACHER", "ADMIN", "STUDENT")
                        .requestMatchers("/api/question-bank/**").hasAnyRole("TEACHER", "ADMIN")
                        .requestMatchers("/api/ai/quiz").hasAnyRole("TEACHER", "ADMIN", "STAFF")
                        .requestMatchers("/api/ai/summarize/staff").hasAnyRole("TEACHER", "ADMIN", "STAFF")
                        .requestMatchers("/api/ai/chat").hasRole("STUDENT")
                        .requestMatchers("/api/ai/summarize/student").hasRole("STUDENT")

                        // Wallet — student sees own, teacher/admin can top-up and view any
                        .requestMatchers("/api/wallet/my/**").hasRole("STUDENT")
                        .requestMatchers("/api/wallet/**").hasAnyRole("TEACHER", "ADMIN", "STUDENT")

                        // Coupons — management by TEACHER/ADMIN, preview by STUDENT
                        .requestMatchers("/api/coupons/preview").hasRole("STUDENT")
                        .requestMatchers("/api/coupons/valid").hasAnyRole("TEACHER", "ADMIN", "STUDENT")
                        .requestMatchers("/api/coupons/**").hasAnyRole("TEACHER", "ADMIN")

                        // Analytics & Dashboard
                        .requestMatchers("/api/analytics/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/analytics/teacher/**").hasAnyRole("TEACHER", "ADMIN", "STAFF")
                        .requestMatchers("/api/analytics/student/**").hasRole("STUDENT")
                        .requestMatchers("/api/analytics/parent/**").hasRole("PARENT")
                        .requestMatchers("/api/analytics/**").hasAnyRole("TEACHER", "ADMIN", "STAFF")

                        // Assignment endpoints — require authentication (were incorrectly public before)
                        .requestMatchers("/api/assignments/**").hasAnyRole("STUDENT", "TEACHER")
                        .requestMatchers("/api/assignment-attempts/**").hasAnyRole("STUDENT", "TEACHER")
                        .requestMatchers("/api/assignment-questions/**").hasAnyRole("STUDENT", "TEACHER")

                        // Enrollment — students track their own, teachers view all
                        .requestMatchers("/api/enrollments/**").hasAnyRole("STUDENT", "TEACHER")

                        // Payment — students only (teachers use access codes)
                        .requestMatchers("/api/payment/**").hasRole("STUDENT")

                        // Access codes — teacher generates, student redeems
                        .requestMatchers("/api/v1/access-codes/generate", "/api/v1/access-codes/my-codes",
                                "/api/v1/access-codes/batch/**").hasAnyRole("TEACHER", "ADMIN")
                        .requestMatchers("/api/v1/access-codes/redeem").hasRole("STUDENT")
                        .requestMatchers("/api/v1/access-codes/**").hasAnyRole("STUDENT", "TEACHER", "ADMIN")

                        // Banners & Announcements management — teacher only
                        // (active/public endpoints are already covered in PUBLIC_ENDPOINTS above)
                        .requestMatchers("/api/banners/admin/**", "/api/banners/**").hasRole("TEACHER")                        .requestMatchers("/api/announcements/admin/**", "/api/announcements/**").hasRole("TEACHER")
                        // Phase 1 — Attendance, Cards, Lesson Gate
                        .requestMatchers("/api/attendance/scan").hasAnyRole("TEACHER", "ADMIN")
                        .requestMatchers("/api/attendance/online/**").hasRole("STUDENT")
                        .requestMatchers("/api/attendance/my").hasRole("STUDENT")
                        .requestMatchers("/api/attendance/**").hasAnyRole("TEACHER", "ADMIN", "STUDENT")
                        .requestMatchers("/api/cards/issue/**").hasAnyRole("TEACHER", "ADMIN")
                        .requestMatchers("/api/cards/my").hasRole("STUDENT")
                        .requestMatchers("/api/cards/**").hasAnyRole("TEACHER", "ADMIN")

                        // Notifications — each role sees only their own
                        .requestMatchers("/api/notifications/**").authenticated()

                        // Role-specific endpoints
                        // NOTE: /api/parent/** is NOT in permitAll — the fix for the original bug
                        .requestMatchers("/api/parent/**", "/parent/**").hasRole("PARENT")
                        .requestMatchers("/api/student/**", "/student/**").hasRole("STUDENT")
                        // STAFF and ADMIN can reach teacher-facing routes — fine-grained access via @PreAuthorize("@perm.can(...)")
                        .requestMatchers("/api/teacher/**", "/teacher/**").hasAnyRole("TEACHER", "STAFF", "ADMIN")
                        .requestMatchers("/api/teacher/students/**").hasAnyRole("TEACHER", "STAFF", "ADMIN")

                        .requestMatchers("/api/admin/**",   "/admin/**").hasRole("ADMIN")

                        // Everything else requires a valid token
                        .anyRequest().authenticated()
                )

                // Rate Limit filter — يشتغل أول حاجة قبل JWT وقبل أي معالجة
                .addFilterBefore(rateLimitFilter, UsernamePasswordAuthenticationFilter.class)

                // JWT filter runs before Spring's username/password filter
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)

                // Return structured JSON on authentication/authorization failures
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) ->
                                writeJsonError(response, 401, "غير مصرح", "يرجى تسجيل الدخول أولاً"))
                        .accessDeniedHandler((request, response, accessDeniedException) ->
                                writeJsonError(response, 403, "غير مسموح", "ليس لديك صلاحية للوصول إلى هذا المورد"))
                )

                .build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        // Origins loaded from application config (app.cors.allowed-origins)
        // Set CORS_ALLOWED_ORIGINS env variable in production
        config.setAllowedOrigins(allowedOrigins);

        config.setAllowedMethods(Arrays.asList(
                "GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"
        ));

        config.setAllowedHeaders(Arrays.asList(
                "Authorization", "Content-Type", "Accept",
                "X-Requested-With", "Cache-Control",
                "X-Device-Id", "X-Session-Id", "Origin"
        ));

        config.setExposedHeaders(Arrays.asList(
                "Authorization", "X-Device-Id", "X-Session-Id"
        ));

        config.setAllowCredentials(true);

        // Cache preflight response for 1 hour
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration)
            throws Exception {
        return configuration.getAuthenticationManager();
    }

    /**
     * BCrypt strength 12 — strong for production.
     * Benchmark before increasing: strength 12 ≈ 250ms/hash on modern hardware.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    // ─────────────────────────────────────────────────────────────
    // Private Helpers
    // ─────────────────────────────────────────────────────────────

    /**
     * Writes a structured JSON error response using ObjectMapper.
     * Replaces the previous hardcoded JSON strings which were fragile and unmaintainable.
     */
    private void writeJsonError(jakarta.servlet.http.HttpServletResponse response,
                                int status, String error, String message) {
        try {
            response.setContentType("application/json;charset=UTF-8");
            response.setStatus(status);
            response.getWriter().write(
                    objectMapper.writeValueAsString(Map.of(
                            "success", false,
                            "error", error,
                            "message", message
                    ))
            );
        } catch (Exception e) {
            response.setStatus(status);
        }
    }
}