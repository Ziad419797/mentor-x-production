package com.educore.DtoEnroll.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class EnrollmentProgressRequest {

    @NotNull(message = "نسبة التقدم مطلوبة")
    @Min(value = 0, message = "نسبة التقدم يجب أن تكون بين 0 و 100")
    @Max(value = 100, message = "نسبة التقدم يجب أن تكون بين 0 و 100")
    private Double progress;

    private Long watchTimeSeconds;

    private Integer completedLessons;
    private Integer totalLessons;
}
