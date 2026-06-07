package com.educore.auth;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

/**
 * PasswordResetTokenService
 *
 * يخزن في الذاكرة أرقام الهواتف التي تحققت من OTP بنجاح
 * ويمنح إذن reset-password لمدة 10 دقائق فقط.
 *
 * لماذا in-memory وليس DB؟
 *   - التوكنات مؤقتة جداً (10 دقائق)
 *   - لا قيمة في تخزينها بعد الاستخدام أو الانتهاء
 *   - يكفي لبيئة single-instance؛ لو تعددت الـ instances استخدم Redis بدلاً منه
 */
@Slf4j
@Service
public class PasswordResetTokenService {

    /** مدة صلاحية التصريح بالـ reset بالدقائق */
    private static final long TTL_MINUTES = 10;

    /** phone → expiry timestamp (epoch seconds) */
    private final ConcurrentHashMap<String, Long> store = new ConcurrentHashMap<>();

    /** يُسجّل رقم الهاتف بعد التحقق الناجح من OTP */
    public void markVerified(String phone) {
        long expiresAt = Instant.now().getEpochSecond() + (TTL_MINUTES * 60);
        store.put(phone, expiresAt);
        log.debug("Password reset token issued for phone={}, expires in {}min", phone, TTL_MINUTES);
    }

    /** يتحقق إن الرقم عنده تصريح نشط لإعادة تعيين كلمة المرور */
    public boolean isVerified(String phone) {
        Long expiresAt = store.get(phone);
        if (expiresAt == null) return false;
        if (Instant.now().getEpochSecond() > expiresAt) {
            store.remove(phone);
            log.debug("Password reset token expired for phone={}", phone);
            return false;
        }
        return true;
    }

    /** يُلغي التصريح بعد استخدامه (one-time use) */
    public void consume(String phone) {
        store.remove(phone);
        log.debug("Password reset token consumed for phone={}", phone);
    }

    /** تنظيف دوري كل 15 دقيقة لمنع memory leak */
    @Scheduled(fixedDelay = 15 * 60 * 1000L)
    public void cleanupExpired() {
        long now = Instant.now().getEpochSecond();
        int removed = 0;
        for (var entry : store.entrySet()) {
            if (now > entry.getValue()) {
                store.remove(entry.getKey());
                removed++;
            }
        }
        if (removed > 0) {
            log.debug("Cleaned up {} expired password reset tokens", removed);
        }
    }
}
