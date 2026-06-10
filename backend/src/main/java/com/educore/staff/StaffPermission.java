package com.educore.staff;

/**
 * StaffPermission — كل الصلاحيات المتاحة للموظفين.
 * مطابقة لتابات السايد بار في لوحة التحكم.
 */
public enum StaffPermission {

    // ── السنوات الدراسية ─────────────────────────
    LEVELS_VIEW("السنوات الدراسية"),
    CATEGORIES_VIEW("التصنيفات"),
    COURSE_ADD("إضافة كورس"),
    LEVEL_STUDENTS("طلاب السنة الدراسية"),
    LEVEL_OVERVIEW("نظرة عامة على السنة"),

    // ── التقييم والنشاط ──────────────────────────
    QUIZZES("بنك الاختبارات"),
    ASSIGNMENTS("المهام والواجبات"),
    QUESTION_BANK("مخزن الأسئلة"),

    // ── شؤون الطلاب ──────────────────────────────
    NEW_REQUESTS("طلبات التسجيل"),
    STUDENTS_MANAGE("إدارة الطلاب"),
    ATTENDANCE("سجل الحضور"),

    // ── الأدوات والمالية ─────────────────────────
    WALLET("المحفظة المالية"),
    WALLET_HISTORY("سجل المحافظ"),
    COUPONS("الكوبونات والخصم"),
    CREATE_CODES("توليد أكواد"),
    CODES_LIST("كل الأكواد"),

    // ── السنتر والمتجر ───────────────────────────
    CENTER_SCHEDULE("جدول السناتر"),
    BOOKS_CODES("أماكن بيع الكتب والأكواد"),
    HOME_LAYOUT("تخصيص الهوم"),
    SUPPORT_CHANNELS("قنوات الدعم"),
    TOPIC_TREE("شجرة المحتوى"),
    TOPIC_ANALYTICS("تحليل نقاط الضعف"),

    // ── إدارة النظام ─────────────────────────────
    ACTIVITY_LOGS("سجل النشاطات"),
    STAFF_MANAGE("فريق العمل"),
    CENTERS_MANAGE("السناتر والفروع"),
    BANNERS("البانرات الإعلانية");

    public final String arabicLabel;

    StaffPermission(String arabicLabel) {
        this.arabicLabel = arabicLabel;
    }
}
