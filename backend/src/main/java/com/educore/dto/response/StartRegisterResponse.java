package com.educore.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

// Response for starting registration
@Getter
@AllArgsConstructor
public class StartRegisterResponse {
    private String message;
    private String phone;
    private String otpCode;  // الحقل الجديد لا تحذفه مهم
    private String action;  // أضف هذا الحقل

}
