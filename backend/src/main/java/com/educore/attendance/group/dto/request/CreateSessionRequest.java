package com.educore.attendance.group.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter @Setter
public class CreateSessionRequest {

    @NotNull(message = "تاريخ الحصة مطلوب")
    private LocalDate sessionDate;

    @NotBlank(message = "عنوان الحصة مطلوب")
    private String title;

    /**
     * رقم الحصة — لو مش محدد يتحسب تلقائياً (آخر رقم + 1)
     */
    private Integer sessionNumber;
}
