package com.educore.security.ratelimit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("RateLimitService Tests")
class RateLimitServiceTest {

    private RateLimitService rateLimitService;

    @BeforeEach
    void setUp() {
        rateLimitService = new RateLimitService();
    }

    @Test
    @DisplayName("أول طلب — مسموح دائماً")
    void firstRequest_alwaysAllowed() {
        RateLimitService.RateLimitResult result =
                rateLimitService.isAllowed("1.2.3.4", RateLimitCategory.LOGIN);

        assertTrue(result.allowed());
    }

    @Test
    @DisplayName("طلبات أقل من الحد — كلها مسموحة")
    void underLimit_allAllowed() {
        String ip = "10.0.0.1";
        // LOGIN limit = 10 requests per 15 min
        for (int i = 0; i < 9; i++) {
            RateLimitService.RateLimitResult result =
                    rateLimitService.isAllowed(ip, RateLimitCategory.LOGIN);
            assertTrue(result.allowed(), "الطلب رقم " + (i + 1) + " يجب أن يكون مسموحاً");
        }
    }

    @Test
    @DisplayName("تجاوز الحد — الطلب مرفوض")
    void exceedLimit_rejected() {
        String ip = "192.168.1.100";
        int limit = RateLimitCategory.ADMIN_LOGIN.maxRequests; // 5

        // استنفد كل المحاولات
        for (int i = 0; i < limit; i++) {
            rateLimitService.isAllowed(ip, RateLimitCategory.ADMIN_LOGIN);
        }

        // الطلب التالي يجب أن يُرفض
        RateLimitService.RateLimitResult result =
                rateLimitService.isAllowed(ip, RateLimitCategory.ADMIN_LOGIN);

        assertFalse(result.allowed(), "يجب رفض الطلب بعد تجاوز الحد");
        assertTrue(result.retryAfterSeconds() > 0, "يجب إرجاع وقت الانتظار");
    }

    @Test
    @DisplayName("IPs مختلفة — كل واحد له حد مستقل")
    void differentIPs_independentLimits() {
        int limit = RateLimitCategory.OTP_SEND.maxRequests; // 3

        // استنفد IP الأول
        for (int i = 0; i < limit; i++) {
            rateLimitService.isAllowed("1.1.1.1", RateLimitCategory.OTP_SEND);
        }

        // IP الثاني لا يزال مسموحاً له
        RateLimitService.RateLimitResult result =
                rateLimitService.isAllowed("2.2.2.2", RateLimitCategory.OTP_SEND);

        assertTrue(result.allowed(), "IP مختلف يجب أن يكون له حد مستقل");
    }

    @Test
    @DisplayName("Categories مختلفة — كل category لها حد مستقل")
    void differentCategories_independentLimits() {
        String ip = "5.5.5.5";
        int loginLimit = RateLimitCategory.ADMIN_LOGIN.maxRequests; // 5

        // استنفد ADMIN_LOGIN
        for (int i = 0; i < loginLimit; i++) {
            rateLimitService.isAllowed(ip, RateLimitCategory.ADMIN_LOGIN);
        }

        // OTP_SEND لنفس الـ IP لا يزال مسموحاً
        RateLimitService.RateLimitResult result =
                rateLimitService.isAllowed(ip, RateLimitCategory.OTP_SEND);

        assertTrue(result.allowed(), "Category مختلفة يجب أن تكون لها حدود مستقلة");
    }

    @Test
    @DisplayName("الـ remaining يقل مع كل طلب")
    void remaining_decreasesWithEachRequest() {
        String ip = "6.6.6.6";
        int limit = RateLimitCategory.LOGIN.maxRequests; // 10

        RateLimitService.RateLimitResult first =
                rateLimitService.isAllowed(ip, RateLimitCategory.LOGIN);
        RateLimitService.RateLimitResult second =
                rateLimitService.isAllowed(ip, RateLimitCategory.LOGIN);

        assertEquals(limit - 1, first.remaining());
        assertEquals(limit - 2, second.remaining());
    }

    @Test
    @DisplayName("الـ RateLimitResult.rejected يحتوي على retryAfterSeconds > 0")
    void rejectedResult_hasPositiveRetryAfter() {
        String ip = "7.7.7.7";
        int limit = RateLimitCategory.OTP_VERIFY.maxRequests; // 5

        for (int i = 0; i < limit; i++) {
            rateLimitService.isAllowed(ip, RateLimitCategory.OTP_VERIFY);
        }

        RateLimitService.RateLimitResult result =
                rateLimitService.isAllowed(ip, RateLimitCategory.OTP_VERIFY);

        assertFalse(result.allowed());
        assertEquals(0, result.remaining());
        assertTrue(result.retryAfterSeconds() > 0);
    }
}
