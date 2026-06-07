package com.educore.security.ratelimit;

/**
 * RateLimitCategory — الحدود المسموح بها لكل نوع endpoint.
 *
 * ┌──────────────────┬─────────────┬──────────────┐
 * │ Category         │ Max Req     │ Window       │
 * ├──────────────────┼─────────────┼──────────────┤
 * │ LOGIN            │ 20          │ 60 دقيقة     │
 * │ ADMIN_LOGIN      │ 10          │ 30 دقيقة     │
 * │ OTP_SEND         │ 10          │ 60 دقيقة     │
 * │ OTP_VERIFY       │ 20          │ 60 دقيقة     │
 * │ REGISTER         │ 20          │ 60 دقيقة     │
 * └──────────────────┴─────────────┴──────────────┘
 */
public enum RateLimitCategory {

    /** تسجيل دخول الطالب والمدرس */
    LOGIN(20, 60),

    /** تسجيل دخول الأدمن — أكثر تشدداً */
    ADMIN_LOGIN(10, 30),

    /** إرسال / إعادة إرسال OTP */
    OTP_SEND(10, 60),

    /** التحقق من OTP */
    OTP_VERIFY(20, 60),

    /** تسجيل حساب جديد */
    REGISTER(20, 60);

    /** أقصى عدد طلبات مسموح بها في الـ window */
    public final int maxRequests;

    /** مدة الـ window بالدقائق */
    public final int windowMinutes;

    RateLimitCategory(int maxRequests, int windowMinutes) {
        this.maxRequests  = maxRequests;
        this.windowMinutes = windowMinutes;
    }
}
