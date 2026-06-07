package com.educore.DtoEnroll.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EnrollmentRequest {

    @NotNull(message = "معرف الكورس مطلوب")
    private Long courseId;

    private LocalDateTime expiresAt;

    private String notes;
}
