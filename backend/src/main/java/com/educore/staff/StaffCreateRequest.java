package com.educore.staff;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.Set;

/**
 * طلب إنشاء موظف جديد — يبعته المدرس.
 */
public record StaffCreateRequest(

        @NotBlank(message = "الاسم الكامل مطلوب")
        @Size(min = 3, max = 100, message = "الاسم يجب أن يكون بين 3 و100 حرف")
        String fullName,

        @NotBlank(message = "رقم الهاتف مطلوب")
        @Pattern(regexp = "^01[0125][0-9]{8}$", message = "رقم الهاتف غير صحيح")
        String phone,

        @NotBlank(message = "كلمة المرور مطلوبة")
        @Size(min = 6, max = 50, message = "كلمة المرور يجب أن تكون بين 6 و50 حرفاً")
        String password,

        /** الصلاحيات المطلوبة — يمكن أن تكون فارغة */
        Set<StaffPermission> permissions,

        @Size(max = 500, message = "الملاحظات لا يمكن أن تتجاوز 500 حرف")
        String notes
) {}
