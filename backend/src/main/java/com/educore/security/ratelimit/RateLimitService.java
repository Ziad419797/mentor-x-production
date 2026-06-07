package com.educore.security.ratelimit;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * RateLimitService — Sliding Window Rate Limiter.
 *
 * الفكرة:
 *   - لكل (IP + category) عندنا deque من timestamps.
 *   - قبل كل request بنشيل الـ timestamps الأقدم من الـ window.
 *   - لو عدد الـ timestamps >= الحد المسموح → نرفض.
 *   - لو مسموح → نضيف الـ timestamp الحالي ونسمح.
 *
 * المزايا:
 *   ✅ بدون dependencies إضافية
 *   ✅ Sliding window (أدق من Fixed window)
 *   ✅ Thread-safe بالكامل
 *   ✅ Cleanup تلقائي كل 30 دقيقة
 */
@Slf4j
@Service
public class RateLimitService {

    /**
     * key   = "ip:CATEGORY_NAME"
     * value = timestamps (epoch millis) للطلبات الأخيرة
     */
    private final Map<String, Deque<Long>> store = new ConcurrentHashMap<>();

    /* ══════════════════════════════════════════════════════
       isAllowed — الدالة الرئيسية
    ══════════════════════════════════════════════════════ */

    /**
     * هل الـ IP المحدد مسموح له بإرسال طلب للـ category دي؟
     *
     * @param ip       عنوان الـ IP
     * @param category نوع الـ endpoint
     * @return RateLimitResult يحتوي على القرار وعدد الطلبات المتبقية
     */
    public RateLimitResult isAllowed(String ip, RateLimitCategory category) {
        String key      = ip + ":" + category.name();
        long   now      = System.currentTimeMillis();
        long   windowMs = (long) category.windowMinutes * 60 * 1000;

        Deque<Long> timestamps = store.computeIfAbsent(key, k -> new ConcurrentLinkedDeque<>());

        synchronized (timestamps) {
            // ازل الـ timestamps الأقدم من الـ window
            timestamps.removeIf(t -> now - t > windowMs);

            int current   = timestamps.size();
            int remaining = category.maxRequests - current;

            if (current >= category.maxRequests) {
                // أقدم طلب في الـ window + مدة الـ window = وقت الـ reset
                long oldestTs   = timestamps.peekFirst() != null ? timestamps.peekFirst() : now;
                long resetAfter = (oldestTs + windowMs - now) / 1000; // بالثواني

                log.warn("Rate limit exceeded — ip={}, category={}, requests={}/{}",
                        ip, category, current, category.maxRequests);

                return RateLimitResult.rejected(remaining, resetAfter, category);
            }

            timestamps.addLast(now);
            return RateLimitResult.allowed(remaining - 1, category);
        }
    }

    /* ══════════════════════════════════════════════════════
       Cleanup — كل 30 دقيقة
    ══════════════════════════════════════════════════════ */

    /**
     * بيحذف entries الـ IPs اللي ما بعتوش طلبات منذ فترة.
     * عشان ما تتراكمش في الذاكرة.
     */
    @Scheduled(fixedDelay = 30 * 60 * 1000L)
    public void cleanup() {
        long now = System.currentTimeMillis();
        // أقصى window هي 15 دقيقة — لو أقدم timestamp من ساعة → ابعد الـ entry
        long maxWindow = 60 * 60 * 1000L;

        store.entrySet().removeIf(entry -> {
            Deque<Long> ts = entry.getValue();
            synchronized (ts) {
                ts.removeIf(t -> now - t > maxWindow);
                return ts.isEmpty();
            }
        });

        log.debug("Rate limit store cleaned up. Remaining keys: {}", store.size());
    }

    /* ══════════════════════════════════════════════════════
       Result Record
    ══════════════════════════════════════════════════════ */

    /**
     * نتيجة فحص الـ Rate Limit.
     */
    public record RateLimitResult(
            boolean           allowed,
            int               remaining,    // طلبات متبقية في الـ window
            long              retryAfterSeconds, // ثواني للانتظار لو مرفوض
            RateLimitCategory category
    ) {
        public static RateLimitResult allowed(int remaining, RateLimitCategory cat) {
            return new RateLimitResult(true, remaining, 0, cat);
        }

        public static RateLimitResult rejected(int remaining, long retryAfter, RateLimitCategory cat) {
            return new RateLimitResult(false, 0, retryAfter, cat);
        }
    }
}
