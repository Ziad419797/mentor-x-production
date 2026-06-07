package com.educore.security;

import com.educore.exception.InvalidTokenException;
import com.educore.exception.TokenExpiredException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("JwtService Tests")
class JwtServiceTest {

    private JwtService jwtService;

    private static final String TEST_SECRET =
            "test-secret-key-must-be-at-least-256-bits-long-for-hs256-algorithm-xyz";
    private static final String TEST_PHONE  = "01012345678";
    private static final String TEST_ROLE   = "STUDENT";
    private static final Long   TEST_USER_ID = 42L;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secret",               TEST_SECRET);
        ReflectionTestUtils.setField(jwtService, "expirationSeconds",    1800);  // 30 min
        ReflectionTestUtils.setField(jwtService, "refreshExpirationSeconds", 2592000); // 30 days
        // استدعاء @PostConstruct يدوياً
        ReflectionTestUtils.invokeMethod(jwtService, "init");
    }

    // ─────────────────────────────────────────────────────────
    // Token Generation
    // ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("generateToken يرجع token غير فاضي")
    void generateToken_returnsNonBlankToken() {
        String token = jwtService.generateToken(TEST_PHONE, TEST_ROLE, TEST_USER_ID);
        assertNotNull(token);
        assertFalse(token.isBlank());
    }

    @Test
    @DisplayName("generateToken يخزن الـ claims صح")
    void generateToken_storesCorrectClaims() {
        String token = jwtService.generateToken(TEST_PHONE, TEST_ROLE, TEST_USER_ID);

        JwtData data = jwtService.parseToken(token);

        assertEquals(TEST_PHONE,   data.phone());
        assertEquals(TEST_ROLE,    data.role());
        assertEquals(TEST_USER_ID, data.userId());
        assertEquals("ACCESS",     data.tokenType());
    }

    @Test
    @DisplayName("generateToken مع deviceId و sessionId يخزنهم صح")
    void generateToken_withDeviceAndSession_storesCorrectly() {
        String token = jwtService.generateToken(
                TEST_PHONE, TEST_ROLE, TEST_USER_ID, "device-123", "session-456");

        JwtData data = jwtService.parseToken(token);

        assertEquals("device-123",  data.deviceId());
        assertEquals("session-456", data.sessionId());
    }

    @Test
    @DisplayName("generateRefreshToken يحتوي على tokenType = REFRESH")
    void generateRefreshToken_hasRefreshType() {
        String refreshToken = jwtService.generateRefreshToken(TEST_PHONE, TEST_ROLE, TEST_USER_ID);

        JwtData data = jwtService.parseToken(refreshToken);
        assertEquals("REFRESH", data.tokenType());
    }

    // ─────────────────────────────────────────────────────────
    // Token Validation
    // ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("isTokenValid يرجع true للـ token الصحيح")
    void isTokenValid_validToken_returnsTrue() {
        String token = jwtService.generateToken(TEST_PHONE, TEST_ROLE, TEST_USER_ID);
        assertTrue(jwtService.isTokenValid(token));
    }

    @Test
    @DisplayName("isTokenValid يرجع false للـ token المزيف")
    void isTokenValid_tamperedToken_returnsFalse() {
        String token = jwtService.generateToken(TEST_PHONE, TEST_ROLE, TEST_USER_ID);
        String tampered = token.substring(0, token.length() - 5) + "XXXXX";
        assertFalse(jwtService.isTokenValid(tampered));
    }

    @Test
    @DisplayName("isTokenValid يرجع false للـ token الفاضي")
    void isTokenValid_blankToken_returnsFalse() {
        assertFalse(jwtService.isTokenValid(""));
        assertFalse(jwtService.isTokenValid(null));
    }

    @Test
    @DisplayName("isAccessToken يرجع true لـ ACCESS token")
    void isAccessToken_returnsTrue_forAccessToken() {
        String token = jwtService.generateToken(TEST_PHONE, TEST_ROLE, TEST_USER_ID);
        assertTrue(jwtService.isAccessToken(token));
    }

    @Test
    @DisplayName("isAccessToken يرجع false لـ REFRESH token")
    void isAccessToken_returnsFalse_forRefreshToken() {
        String refreshToken = jwtService.generateRefreshToken(TEST_PHONE, TEST_ROLE, TEST_USER_ID);
        assertFalse(jwtService.isAccessToken(refreshToken));
    }

    // ─────────────────────────────────────────────────────────
    // Claim Extractors
    // ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("extractPhone يرجع رقم الهاتف الصح")
    void extractPhone_returnsCorrectPhone() {
        String token = jwtService.generateToken(TEST_PHONE, TEST_ROLE, TEST_USER_ID);
        assertEquals(TEST_PHONE, jwtService.extractPhone(token));
    }

    @Test
    @DisplayName("extractUserId يرجع الـ userId الصح")
    void extractUserId_returnsCorrectId() {
        String token = jwtService.generateToken(TEST_PHONE, TEST_ROLE, TEST_USER_ID);
        assertEquals(TEST_USER_ID, jwtService.extractUserId(token));
    }

    @Test
    @DisplayName("extractRole يرجع الـ role الصح")
    void extractRole_returnsCorrectRole() {
        String token = jwtService.generateToken(TEST_PHONE, TEST_ROLE, TEST_USER_ID);
        assertEquals(TEST_ROLE, jwtService.extractRole(token));
    }

    // ─────────────────────────────────────────────────────────
    // Refresh Token Flow
    // ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("refreshToken بـ REFRESH token يرجع ACCESS token جديد")
    void refreshToken_withRefreshToken_returnsNewAccessToken() {
        String refreshToken = jwtService.generateRefreshToken(TEST_PHONE, TEST_ROLE, TEST_USER_ID);
        String newAccessToken = jwtService.refreshToken(refreshToken);

        assertNotNull(newAccessToken);
        assertTrue(jwtService.isAccessToken(newAccessToken));
        assertEquals(TEST_PHONE, jwtService.extractPhone(newAccessToken));
    }

    @Test
    @DisplayName("refreshToken بـ ACCESS token يرمي InvalidTokenException")
    void refreshToken_withAccessToken_throwsException() {
        String accessToken = jwtService.generateToken(TEST_PHONE, TEST_ROLE, TEST_USER_ID);

        assertThrows(InvalidTokenException.class,
                () -> jwtService.refreshToken(accessToken));
    }

    // ─────────────────────────────────────────────────────────
    // Remaining Time
    // ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("getRemainingSeconds يرجع وقت موجب للـ token الصالح")
    void getRemainingSeconds_validToken_returnsPositive() {
        String token = jwtService.generateToken(TEST_PHONE, TEST_ROLE, TEST_USER_ID);
        long remaining = jwtService.getRemainingSeconds(token);

        assertTrue(remaining > 0);
        assertTrue(remaining <= 1800); // لازم يكون أقل من أو يساوي 30 دقيقة
    }

    @Test
    @DisplayName("getRemainingSeconds للـ token المزيف يرجع 0")
    void getRemainingSeconds_invalidToken_returnsZero() {
        assertEquals(0, jwtService.getRemainingSeconds("invalid.token.here"));
    }

    // ─────────────────────────────────────────────────────────
    // parseToken Exceptions
    // ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("parseToken لـ token مزيف يرمي InvalidTokenException")
    void parseToken_tamperedToken_throwsInvalidTokenException() {
        assertThrows(InvalidTokenException.class,
                () -> jwtService.parseToken("this.is.not.a.valid.jwt"));
    }

    @Test
    @DisplayName("tokenان مختلفان لنفس المستخدم — مختلفان دائماً")
    void twoTokens_forSameUser_areDifferent() {
        String token1 = jwtService.generateToken(TEST_PHONE, TEST_ROLE, TEST_USER_ID);
        String token2 = jwtService.generateToken(TEST_PHONE, TEST_ROLE, TEST_USER_ID);
        // بسبب الـ timestamp في الـ iat claim
        assertNotEquals(token1, token2);
    }
}
