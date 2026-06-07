package com.educore.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminAuthResponse {
    private Long   id;
    private String name;
    private String phone;
    private String role;
    private String accessToken;
    private String message;
}
