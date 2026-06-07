package com.educore.staff;

import jakarta.validation.constraints.Size;

/**
 * طلب تعديل بيانات موظف — الحقول اختيارية (Patch semantics).
 */
public record StaffUpdateRequest(

        @Size(min = 3, max = 100, message = "الاسم يجب أن يكون بين 3 و100 حرف")
        String fullName,

        @Size(min = 6, max = 50, message = "كلمة المرور يجب أن تكون بين 6 و50 حرفاً")
        String newPassword,

        @Size(max = 500, message = "الملاحظات لا يمكن أن تتجاوز 500 حرف")
        String notes,

        Boolean active
) {}
