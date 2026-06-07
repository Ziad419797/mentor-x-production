package com.educore.security;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * OtpRecord — سجل OTP في قاعدة البيانات.
 *
 * بدل الـ ConcurrentHashMap القديم، كل OTP بيتخزن في PostgreSQL.
 * الفوايد:
 *   ✅ السيرفر لو اتريستارت الـ OTPs بتفضل شغالة
 *   ✅ ممكن تتحقق من عدد المحاولات عشان تمنع brute force
 *   ✅ ممكن تشوف الـ OTPs في الـ DB لو محتاج debugging
 */
@Entity
@Table(
        name = "otp_records",
        indexes = {
                @Index(name = "idx_otp_phone", columnList = "phone"),
                @Index(name = "idx_otp_expires", columnList = "expires_at")
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OtpRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** رقم الهاتف المرسل إليه الـ OTP */
    @Column(nullable = false, length = 15)
    private String phone;

    /** الكود المكون من 6 أرقام */
    @Column(nullable = false, length = 10)
    private String otp;

    /** وقت انتهاء الصلاحية */
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    /** تم استخدامه ولا لأ */
    @Column(nullable = false)
    @Builder.Default
    private boolean used = false;

    /** عدد المحاولات الخاطئة لنفس الـ OTP */
    @Column(name = "failed_attempts", nullable = false)
    @Builder.Default
    private int failedAttempts = 0;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    /* ══════════════════════════════════════════
       Helpers
    ══════════════════════════════════════════ */

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    public boolean isValid() {
        return !used && !isExpired() && failedAttempts < 5;
    }

    public void markUsed() {
        this.used = true;
    }

    public void incrementFailedAttempts() {
        this.failedAttempts++;
    }
}
