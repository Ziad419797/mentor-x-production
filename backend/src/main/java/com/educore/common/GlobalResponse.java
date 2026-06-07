package com.educore.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Unified API response wrapper used by banner, announcement, and other common modules.
 * The legacy com.educore.auth.GlobalResponse is kept as-is for backward compatibility
 * with existing auth/student/teacher endpoints.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GlobalResponse<T> {

    private boolean success;
    private String message;
    private T data;
    private Integer code;
    private LocalDateTime timestamp;
    private String path;

    public static <T> GlobalResponse<T> success(T data) {
        return GlobalResponse.<T>builder()
                .success(true)
                .message("تمت العملية بنجاح")
                .data(data)
                .timestamp(LocalDateTime.now())
                .build();
    }

    public static <T> GlobalResponse<T> success(String message, T data) {
        return GlobalResponse.<T>builder()
                .success(true)
                .message(message)
                .data(data)
                .timestamp(LocalDateTime.now())
                .build();
    }

    public static <T> GlobalResponse<T> error(String message) {
        return GlobalResponse.<T>builder()
                .success(false)
                .message(message)
                .timestamp(LocalDateTime.now())
                .build();
    }

    public static <T> GlobalResponse<T> error(String message, Integer code) {
        return GlobalResponse.<T>builder()
                .success(false)
                .message(message)
                .code(code)
                .timestamp(LocalDateTime.now())
                .build();
    }
}
