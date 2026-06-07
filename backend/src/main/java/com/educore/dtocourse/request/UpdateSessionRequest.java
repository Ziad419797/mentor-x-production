package com.educore.dtocourse.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateSessionRequest {

    @NotBlank(message = "Session title is required")
    private String title;

    private String description;

    private String teachingType; // ONLINE | CENTER | BOTH

    private Integer orderNumber;

    private Boolean active;
}
