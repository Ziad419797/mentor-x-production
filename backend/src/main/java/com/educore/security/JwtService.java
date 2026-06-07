package com.educore.security;

import com.educore.exception.InvalidTokenException;
import com.educore.exception.TokenExpiredException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class JwtService {

    /**
     * No default fallback — startup fails if jwt.secret is not configured.
     * Prevents accidentally running with a weak/known key in production.
     */
    @Value("${jwt.secret}")
    private String secret;

    /** Access token lifetime in seconds (default: 30 min). */
    @Value("${jwt.expiration:1800}")
    private int expirationSeconds;

    /** Refresh token lifetime in seconds (default: 30 days). */
    @Value("${jwt.refresh-expiration:2592000}")
    private int refreshExpirationSeconds;

    /**
     * Signing key computed once at startup and cached.
     * Deriving it from bytes on every JWT operation is wasteful.
     */
    private SecretKey signingKey;

    @PostConstruct
    private void init() {
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        log.info("JwtService initialized — access token TTL: {}s, refresh TTL: {}s",
                expirationSeconds, refreshExpirationSeconds);
    }

    // ─────────────────────────────────────────────────────────────
    // Token Generation
    // ─────────────────────────────────────────────────────────────

    /**
     * Generates a full ACCESS token carrying all user context.
     * Null claims are excluded to keep the token compact.
     */
    public String generateToken(String phone, String role, Long userId,
                                String deviceId, String sessionId,
                                String studentCode, String name, String status) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", role);
        claims.put("userId", userId);
        claims.put("phone", phone);
        claims.put("tokenType", "ACCESS");

        // Only add optional fields when present — keeps token size down
        if (deviceId    != null) claims.put("deviceId", deviceId);
        if (sessionId   != null) claims.put("sessionId", sessionId);
        if (studentCode != null) claims.put("studentCode", studentCode);
        if (name        != null) claims.put("name", name);
        if (status      != null) claims.put("status", status);

        return buildJwt(claims, phone, expirationSeconds);
    }

    /** Convenience overload — used by most login flows. */
    public String generateToken(String phone, String role, Long userId,
                                String deviceId, String sessionId) {
        return generateToken(phone, role, userId, deviceId, sessionId, null, null, null);
    }

    /** Minimal overload — used only for backward-compatibility where session info isn't needed. */
    public String generateToken(String phone, String role, Long userId) {
        return generateToken(phone, role, userId, null, null, null, null, null);
    }

    /**
     * Generates a REFRESH token.
     * Contains only the minimal claims needed to issue a new access token.
     * Note: refresh tokens are long-lived (30 days) — keep them minimal and never put sensitive data in them.
     */
    public String generateRefreshToken(String phone, String role, Long userId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", role);
        claims.put("userId", userId);
        claims.put("phone", phone);
        claims.put("tokenType", "REFRESH");

        return buildJwt(claims, phone, refreshExpirationSeconds);
    }

    /**
     * Issues a new ACCESS token from a valid REFRESH token.
     *
     * @throws InvalidTokenException if the provided token is not of type REFRESH
     */
    public String refreshToken(String oldToken) {
        JwtData jwtData = parseToken(oldToken);

        // Security check: only REFRESH tokens may be used to obtain a new access token
        if (!"REFRESH".equals(jwtData.tokenType())) {
            throw new InvalidTokenException(
                    "Invalid token type for refresh — expected REFRESH, got: " + jwtData.tokenType()
            );
        }

        return generateToken(
                jwtData.phone(), jwtData.role(), jwtData.userId(),
                jwtData.deviceId(), jwtData.sessionId(),
                jwtData.studentCode(), jwtData.name(), jwtData.status()
        );
    }

    // ─────────────────────────────────────────────────────────────
    // Token Parsing & Validation
    // ─────────────────────────────────────────────────────────────

    /**
     * Parses a JWT string and returns a fully-typed {@link JwtData} record.
     *
     * @throws TokenExpiredException  if the token has passed its expiration time
     * @throws InvalidTokenException  if the token is malformed, tampered, or otherwise invalid
     */
    public JwtData parseToken(String token) {
        Claims claims = extractAllClaims(token); // throws on failure

        return JwtData.builder()
                .phone(claims.getSubject())
                .role(claims.get("role", String.class))
                .userId(claims.get("userId", Long.class))
                .deviceId(claims.get("deviceId", String.class))
                .sessionId(claims.get("sessionId", String.class))
                .studentCode(claims.get("studentCode", String.class))
                .name(claims.get("name", String.class))
                .status(claims.get("status", String.class))
                .tokenType(claims.get("tokenType", String.class))
                .token(token)
                .expiry(toLocalDateTime(claims.getExpiration()))
                .issuedAt(toLocalDateTime(claims.getIssuedAt()))
                .build();
    }

    /**
     * Returns true only if the token passes signature and expiry validation.
     * Does NOT check the database blacklist — use {@link com.educore.session.DatabaseSessionService} for that.
     */
    public boolean isTokenValid(String token) {
        try {
            extractAllClaims(token);
            return true;
        } catch (JwtException | TokenExpiredException | InvalidTokenException e) {
            log.debug("Token validation failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Returns true if the token is specifically an ACCESS token (not REFRESH).
     * Used by the auth filter to reject refresh tokens being used as access tokens.
     */
    public boolean isAccessToken(String token) {
        try {
            Claims claims = extractAllClaims(token);
            return "ACCESS".equals(claims.get("tokenType", String.class));
        } catch (Exception e) {
            return false;
        }
    }

    /** Returns remaining token lifetime in seconds, or 0 if expired/invalid. */
    public long getRemainingSeconds(String token) {
        try {
            Date expiry = extractAllClaims(token).getExpiration();
            if (expiry == null) return 0;
            return Math.max(0, (expiry.getTime() - System.currentTimeMillis()) / 1000);
        } catch (Exception e) {
            return 0;
        }
    }

    // ─────────────────────────────────────────────────────────────
    // Individual Claim Extractors (used by JwtAuthenticationFilter)
    // ─────────────────────────────────────────────────────────────

    public String extractPhone(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public Long extractUserId(String token) {
        return extractClaim(token, claims -> claims.get("userId", Long.class));
    }

    public String extractRole(String token) {
        return extractClaim(token, claims -> claims.get("role", String.class));
    }

    public String extractDeviceId(String token) {
        return extractClaim(token, claims -> claims.get("deviceId", String.class));
    }

    public String extractSessionId(String token) {
        return extractClaim(token, claims -> claims.get("sessionId", String.class));
    }

    public String extractStatus(String token) {
        return extractClaim(token, claims -> claims.get("status", String.class));
    }

    public String extractStudentCode(String token) {
        return extractClaim(token, claims -> claims.get("studentCode", String.class));
    }

    // ─────────────────────────────────────────────────────────────
    // Private Helpers
    // ─────────────────────────────────────────────────────────────

    /**
     * Core parsing method. Maps JJWT-specific exceptions to application-level exceptions
     * so callers don't need to depend on the JJWT library directly.
     */
    private Claims extractAllClaims(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(signingKey)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (ExpiredJwtException e) {
            // Token is valid but has expired — caller decides how to handle
            throw new TokenExpiredException("Token has expired");
        } catch (MalformedJwtException e) {
            throw new InvalidTokenException("Token is malformed");
        } catch (SignatureException e) {
            throw new InvalidTokenException("Token signature is invalid — possible tampering");
        } catch (JwtException e) {
            throw new InvalidTokenException("Token processing failed: " + e.getMessage());
        }
    }

    /** Helper to apply a claim extractor function with exception handling. */
    private <T> T extractClaim(String token, java.util.function.Function<Claims, T> resolver) {
        return resolver.apply(extractAllClaims(token));
    }

    /** Builds and signs a JWT with the given claims and lifetime in seconds. */
    private String buildJwt(Map<String, Object> claims, String subject, int lifetimeSeconds) {
        Date now    = new Date();
        Date expiry = new Date(now.getTime() + (long) lifetimeSeconds * 1000);

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(now)
                .setExpiration(expiry)
                .signWith(signingKey, SignatureAlgorithm.HS256)
                .compact();
    }

    /** Converts a legacy java.util.Date to LocalDateTime using the system timezone. */
    private LocalDateTime toLocalDateTime(Date date) {
        return date != null
                ? LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault())
                : null;
    }
}
