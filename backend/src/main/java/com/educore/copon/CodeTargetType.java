package com.educore.copon;

public enum CodeTargetType {
    CATEGORY,  // يفتح باقة كاملة (كل كورساتها)
    COURSE,    // يفتح كورس واحد بس
    SESSION,   // يفتح حصة معينة داخل كورس (الطالب يتسجل في الكورس المرتبط)
    WALLET     // يشحن محفظة الطالب بقيمة الكود
}