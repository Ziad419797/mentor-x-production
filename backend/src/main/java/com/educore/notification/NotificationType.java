package com.educore.notification;

public enum NotificationType {
    // حضور
    ATTENDANCE_CENTER,      // سجّل حضور في السنتر
    ATTENDANCE_ONLINE,      // دخل حصة أونلاين

    // تقييم
    QUIZ_SUBMITTED,         // سلّم كويز
    QUIZ_PASSED,            // نجح في الكويز وفتح الحصة التالية
    QUIZ_FAILED,            // رسب في الكويز ومحتاج يراجع
    ASSIGNMENT_SUBMITTED,   // سلّم واجب

    // تسجيل
    ENROLLMENT_CREATED,     // تسجل في كورس جديد
    ENROLLMENT_EXPIRED,     // انتهت صلاحية تسجيله

    // نظام
    SYSTEM,                 // إشعار عام من الإدارة
    LESSON_UNLOCKED,        // الحصة الجاية اتفتحت
    ACCOUNT_STATUS,         // تغيير في حالة الحساب

    // مناسبات
    BIRTHDAY                // تهنئة عيد الميلاد
}
