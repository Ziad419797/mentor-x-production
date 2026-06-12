package com.educore.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;

@Data
public class CompleteRegisterRequest {

    // ================= Authentication =================

    @NotBlank(message = "رقم الهاتف مطلوب")
    @Pattern(regexp = "^01[0-9]{9}$", message = "رقم الهاتف غير صحيح")
    private String phone;

    @NotBlank(message = "رقم ولي الأمر مطلوب")
    @Pattern(regexp = "^01[0-9]{9}$", message = "رقم ولي الأمر غير صحيح")
    private String parentPhone;

    @NotNull(message = "رمز التحقق مطلوب")
    @Min(value = 100000, message = "رمز التحقق غير صحيح")
    @Max(value = 999999, message = "رمز التحقق غير صحيح")
    private Integer otp;

    @NotBlank(message = "كلمة المرور مطلوبة")
    @Size(min = 6, message = "كلمة المرور يجب أن تكون 6 أحرف على الأقل")
    private String password;

    // ================= Personal Info =================

    @NotBlank(message = "الاسم الأول مطلوب")
    @Pattern(regexp = "^[\\u0600-\\u06FF\\s]+$", message = "الاسم الأول يجب أن يكون باللغة العربية فقط")
    private String firstName;

    @NotBlank(message = "الاسم الثاني مطلوب")
    @Pattern(regexp = "^[\\u0600-\\u06FF\\s]+$", message = "الاسم الثاني يجب أن يكون باللغة العربية فقط")
    private String secondName;

    @NotBlank(message = "الاسم الثالث مطلوب")
    @Pattern(regexp = "^[\\u0600-\\u06FF\\s]+$", message = "الاسم الثالث يجب أن يكون باللغة العربية فقط")
    private String thirdName;

    @NotBlank(message = "الاسم الرابع مطلوب")
    @Pattern(regexp = "^[\\u0600-\\u06FF\\s]+$", message = "الاسم الرابع يجب أن يكون باللغة العربية فقط")
    private String fourthName;

    /** الرقم القومي — 14 رقم، مطلوب */
    @NotBlank(message = "الرقم القومي مطلوب")
    @Pattern(regexp = "^[0-9]{14}$", message = "الرقم القومي يجب أن يكون 14 رقماً")
    private String nationalId;

    /** تاريخ الميلاد — مطلوب */
    @NotNull(message = "تاريخ الميلاد مطلوب")
    @Past(message = "تاريخ الميلاد يجب أن يكون في الماضي")
    private LocalDate dateOfBirth;

    // ================= Academic Info =================

    @NotBlank(message = "الصف الدراسي مطلوب")
    private String grade;

    @NotBlank(message = "المحافظة مطلوبة")
    private String governorate;

    @NotBlank(message = "المنطقة مطلوبة")
    private String area;

    @NotBlank(message = "اسم المدرسة مطلوب")
    private String schoolName;

    @NotBlank(message = "نوع المدرسة مطلوب")
    @Pattern(regexp = "^(عام|أزهر)$", message = "نوع المدرسة يجب أن يكون عام أو أزهر")
    private String schoolType;

    @NotBlank(message = "الإدارة التعليمية مطلوبة")
    private String educationDepartment;

    // ================= Study Type =================

    @NotNull(message = "نوع الدراسة مطلوب (أونلاين أو مركز)")
    private Boolean online;

    private String centerName;

    /** معرف جروب الحضور (الميعاد) اللي اختاره الطالب عند التسجيل */
    private Long attendanceGroupId;

    // ================= Documents =================

    private MultipartFile profileImageUrl;
    private MultipartFile identityDocumentUrl;
}
