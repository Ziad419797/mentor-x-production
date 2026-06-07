package com.educore.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class TeacherRegisterRequest {

    @NotBlank(message = "رقم الهاتف مطلوب")
    @Pattern(regexp = "^\\+?[0-9]{10,15}$", message = "صيغة رقم الهاتف غير صحيحة")
    private String phone;

    @NotBlank(message = "كلمة المرور مطلوبة")
    @Size(min = 8, message = "كلمة المرور يجب أن تكون 8 أحرف على الأقل")
    private String password;

    @NotBlank(message = "الاسم مطلوب")
    @Size(min = 2, max = 100, message = "الاسم يجب أن يكون بين 2 و 100 حرف")
    private String name;

    /** Subject the teacher teaches — optional */
    private String subject;

    /** Optional email */
    @Email(message = "صيغة البريد الإلكتروني غير صحيحة")
    private String email;
}
