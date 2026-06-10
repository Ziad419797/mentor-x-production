package com.educore.staff;

import java.time.LocalDateTime;
import java.util.Set;

/**
 * الـ response اللي بيتبعت للمدرس لما يشوف/يعدل موظف.
 * كلمة المرور ما بتتبعتش أبداً.
 */
public record StaffResponse(
        Long id,
        String fullName,
        String phone,
        Long teacherId,
        Set<StaffPermission> permissions,
        boolean active,
        String notes,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        String role
) {
    public static StaffResponse from(Staff s) {
        return new StaffResponse(
                s.getId(),
                s.getFullName(),
                s.getPhone(),
                s.getTeacherId(),
                s.getPermissions(),
                s.isActive(),
                s.getNotes(),
                s.getCreatedAt(),
                s.getUpdatedAt(),
                "STAFF"
        );
    }
}
