// OtpResponse.java
package com.educore.dto.response;

public record OtpResponse(
        boolean success,
        String message
) {}
