package com.educore.security;

/**
 * Defines all user roles in the system.
 * Use this enum everywhere instead of magic strings like "STUDENT", "TEACHER", etc.
 * This prevents typos and makes role comparisons refactor-safe.
 */
public enum UserRole {
    STUDENT,
    TEACHER,
    PARENT,
    ADMIN,
    /** موظف أنشأه المدرس — صلاحياته محددة في جدول staff_permissions */
    STAFF;

    /**
     * Returns the Spring Security role name (with "ROLE_" prefix stripped).
     * Spring Security's hasRole("STUDENT") internally checks for "ROLE_STUDENT".
     * Use name() when storing in JWT claims, use this for display purposes.
     */
    public String authority() {
        return "ROLE_" + this.name();
    }
}
