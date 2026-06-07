package com.educore.attendance;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * خدمة التوكنات المؤقتة لـ QR الحضور في حالة تعارض الأجهزة.
 *
 * السيناريو:
 *   1. الطالب يحاول يسجل دخول من جهاز تاني فيجيبله رسالة تعارض.
 *   2. يضغط "احصل على QR الحضور" → الـ frontend يبعت POST /api/auth/student/conflict-qr
 *   3. الـ backend يتحقق من الباسورد، لو صح يرجع token مدته 10 دقايق.
 *   4. الـ frontend يعرض QR يحتوي على الـ token ده.
 *   5. المدرس يسكن الـ QR → الـ backend يتحقق من التوكن ويسجل الحضور.
 *
 * التوكنات بتتحذف تلقائياً بعد 10 دقايق أو بعد الاستخدام الأول.
 */
@Slf4j
@Service
public class ConflictQrTokenService {

    private static final long TTL_MILLIS = 10 * 60 * 1000L; // 10 دقايق

    // token → (studentId, expiryEpochMs)
    private final Map<String, TokenEntry> store = new ConcurrentHashMap<>();

    // ─── إنشاء توكن ─────────────────────────────────────────────

    /**
     * ينشئ توكن جديد مرتبط بالطالب وبيرجعه.
     * كل استدعاء يلغي أي توكن قديم لنفس الطالب.
     */
    public String generateToken(Long studentId) {
        // ألغي أي توكن قديم لنفس الطالب
        store.values().removeIf(e -> e.studentId.equals(studentId));

        String token = UUID.randomUUID().toString().replace("-", "");
        long expiry = Instant.now().toEpochMilli() + TTL_MILLIS;
        store.put(token, new TokenEntry(studentId, expiry));

        log.debug("Conflict QR token generated for student {}: expires in 10 min", studentId);
        return token;
    }

    // ─── التحقق من التوكن والاستهلاك ────────────────────────────

    /**
     * يتحقق من التوكن ويرجع studentId لو صالح.
     * التوكن يُستهلك (يُحذف) بعد التحقق الناجح الأول.
     */
    public Optional<Long> validateAndConsume(String token) {
        TokenEntry entry = store.get(token);
        if (entry == null) {
            return Optional.empty();
        }
        if (Instant.now().toEpochMilli() > entry.expiryMs) {
            store.remove(token);
            log.debug("Conflict QR token expired: {}", token);
            return Optional.empty();
        }
        // استهلك التوكن (مرة واحدة فقط)
        store.remove(token);
        log.debug("Conflict QR token consumed for student {}", entry.studentId);
        return Optional.of(entry.studentId);
    }

    /**
     * يتحقق من التوكن بدون استهلاك — للـ QR preview على الـ frontend.
     * (الاستهلاك الحقيقي يحصل عند scan المدرس)
     */
    public Optional<Long> peekStudentId(String token) {
        TokenEntry entry = store.get(token);
        if (entry == null) return Optional.empty();
        if (Instant.now().toEpochMilli() > entry.expiryMs) {
            store.remove(token);
            return Optional.empty();
        }
        return Optional.of(entry.studentId);
    }

    // ─── Cleanup ─────────────────────────────────────────────────

    @Scheduled(fixedDelay = 5 * 60 * 1000L) // كل 5 دقايق
    public void evictExpiredTokens() {
        long now = Instant.now().toEpochMilli();
        int removed = 0;
        for (Map.Entry<String, TokenEntry> e : store.entrySet()) {
            if (now > e.getValue().expiryMs) {
                store.remove(e.getKey());
                removed++;
            }
        }
        if (removed > 0) {
            log.debug("Evicted {} expired conflict QR tokens", removed);
        }
    }

    // ─── Inner record ─────────────────────────────────────────────

    private record TokenEntry(Long studentId, long expiryMs) {}
}
