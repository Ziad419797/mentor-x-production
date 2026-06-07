package com.educore.security;

/**
 * SmsService — الواجهة الموحدة لإرسال الـ OTP عبر SMS.
 *
 * التنفيذات المتاحة:
 *   - SmsMisrSmsService  → إرسال حقيقي عبر SMS Misr (production)
 *   - ConsoleSmsService  → يطبع الـ OTP في الـ console فقط (development)
 *
 * التبديل بينهم عبر application.properties:
 *   sms.provider=smsmisr   ← production
 *   sms.provider=console   ← development/testing
 */
public interface SmsService {

    /**
     * يرسل الـ OTP للرقم المحدد.
     *
     * @param phone   رقم الهاتف (مثال: 01012345678)
     * @param otpCode الكود المكون من 6 أرقام
     * @throws SmsException لو فشل الإرسال
     */
    void sendOtp(String phone, String otpCode);

    /** Exception خاص بفشل إرسال الـ SMS */
    class SmsException extends RuntimeException {
        public SmsException(String message) { super(message); }
        public SmsException(String message, Throwable cause) { super(message, cause); }
    }
}
