package com.educore.attendance.group.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class CreateGroupRequest {

    /** يوم الأسبوع — مطلوب — مثال: "السبت" */
    @NotBlank(message = "يوم الأسبوع مطلوب")
    @Size(max = 20)
    private String dayOfWeek;

    /** وقت الميعاد — مطلوب — HH:mm — مثال: "10:30" */
    @NotBlank(message = "وقت الميعاد مطلوب")
    @Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d$", message = "صيغة الوقت غير صحيحة (HH:mm)")
    private String meetingTime;

    @Size(max = 500)
    private String description;

    /** ID السنتر (اختياري) */
    private Long centerId;

    /** الصف الدراسي — مطلوب */
    @NotNull(message = "الصف الدراسي مطلوب")
    private Long levelId;

    /** اسم السنتر كـ نص (اختياري) */
    @Size(max = 150)
    private String centerName;

    /** الحد الأقصى لعدد الطلاب (null = بدون حد) */
    private Integer maxCapacity;
}
