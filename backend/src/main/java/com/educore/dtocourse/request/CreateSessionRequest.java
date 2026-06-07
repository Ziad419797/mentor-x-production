package com.educore.dtocourse.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateSessionRequest {

    @NotBlank(message = "Session title is required")
    private String title;

    private String description;

    private String teachingType; // ONLINE | CENTER | BOTH

    private Integer orderNumber;  // optional — service can default to 0

    @NotEmpty(message = "At least one course is required")
    private Set<Long> courseIds;

}
