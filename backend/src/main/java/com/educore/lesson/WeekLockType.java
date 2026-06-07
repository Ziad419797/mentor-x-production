package com.educore.lesson;

/**
 * نوع قفل الحصة الأسبوعية.
 *
 * NEVER          → الحصة مفتوحة دايماً (الافتراضي)
 * AFTER_DURATION → تقفل بعد عدد أيام محدد من أول وصول للطالب لها
 * ON_DATE        → تقفل في تاريخ محدد لكل الطلاب (سواء وصلوا أو لا)
 */
public enum WeekLockType {
    NEVER,
    AFTER_DURATION,
    ON_DATE
}
