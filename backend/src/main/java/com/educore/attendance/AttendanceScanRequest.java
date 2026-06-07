package com.educore.attendance;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AttendanceScanRequest {

    /** الـ QR Token الموجود على كارنيه الطالب */
    @NotBlank(message = "QR token مطلوب")
    private String qrToken;

    /** ID الحصة اللي الطالب حاضر فيها */
    @NotNull(message = "week ID مطلوب")
    private Long weekId;

    /** ملاحظة اختيارية من الموظف */
    private String notes;
}
