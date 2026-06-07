package com.educore.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateParentProfileRequest {

    @Size(min = 2, max = 100, message = "الاسم بين 2 و 100 حرف")
    private String name;
}
