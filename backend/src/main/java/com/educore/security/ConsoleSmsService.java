package com.educore.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * ConsoleSmsService — تنفيذ للتطوير والاختبار فقط.
 *
 * بدل ما يبعت SMS حقيقي، بيطبع الـ OTP في الـ console.
 * يُفعَّل لما تكون: sms.provider=console (الإعداد الافتراضي)
 *
 * عشان تشتغل بـ SMS حقيقي في الـ production:
 *   غيّر في application.properties:
 *     sms.provider=smsmisr
 *
 * ⚠️ تأكد إن sms.provider=console مش موجود في الـ production!
 */
@Slf4j
@Service
@ConditionalOnProperty(
        name         = "sms.provider",
        havingValue  = "console",
        matchIfMissing = true  // الافتراضي: console (لو مفيش إعداد)
)
public class ConsoleSmsService implements SmsService {

    @Override
    public void sendOtp(String phone, String otpCode) {
        // ═══════════════════════════════════════════════════════════════
        //  ⚠️  DEV MODE — الـ OTP بيظهر في الـ console بس مش بيتبعت SMS
        // ═══════════════════════════════════════════════════════════════
        log.warn("╔══════════════════════════════════════════════════╗");
        log.warn("║   DEV MODE — OTP NOT SENT VIA SMS                ║");
        log.warn("║   Phone  : {}                          ║", phone);
        log.warn("║   OTP    : {}                                  ║", otpCode);
        log.warn("║   To enable real SMS: sms.provider=smsmisr      ║");
        log.warn("╚══════════════════════════════════════════════════╝");
    }
}
