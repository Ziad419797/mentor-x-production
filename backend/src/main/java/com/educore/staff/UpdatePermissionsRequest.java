package com.educore.staff;

import jakarta.validation.constraints.NotNull;

import java.util.Set;

/**
 * طلب تحديث صلاحيات موظف — يبعته المدرس.
 * الـ Set الجديد يحل محل الـ Set القديم بالكامل.
 */
public record UpdatePermissionsRequest(

        @NotNull(message = "قائمة الصلاحيات مطلوبة (يمكن أن تكون فارغة)")
        Set<StaffPermission> permissions
) {}
