package com.educore.attendance.group.dto.response;

import com.educore.attendance.group.GroupAlertType;
import lombok.Builder;
import lombok.Getter;

/**
 * نتيجة عملية الـ scan / التسجيل اليدوي.
 * يحتوي على بيانات الطالب + أي alert وجد.
 */
@Getter @Builder
public class MarkResult {
    private RecordResponse record;

    // ─── Alert للعرض الفوري في واجهة المدرس ──────────────────
    private boolean        hasAlert;
    private GroupAlertType alertType;
    private String         alertMessage;   // رسالة واضحة للمدرس
}
