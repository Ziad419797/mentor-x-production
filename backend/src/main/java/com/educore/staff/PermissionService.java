package com.educore.staff;

import com.educore.security.JwtUserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * PermissionService — بين الـ @PreAuthorize expressions والصلاحيات الفعلية.
 *
 * الاستخدام في الكنترولر:
 *   @PreAuthorize("@perm.can(authentication, 'APPROVE_STUDENTS')")
 *
 * قواعد الصلاحيات:
 *   ✅ TEACHER → يعدي كل الصلاحيات تلقائياً (السوبر أدمن)
 *   ✅ ADMIN   → يعدي كل الصلاحيات تلقائياً
 *   ✅ STAFF   → بيتحقق من الـ permissions اللي اختارها المدرس له
 *   ❌ غير كده → رفض
 *
 * Cache:
 *   صلاحيات الـ STAFF بتتحفظ في ذاكرة بمدة 5 دقائق عشان ما نضربش DB
 *   في كل request. بتتحدث لما المدرس يعدّل الصلاحيات (invalidateCache).
 */
@Slf4j
@Component("perm")
@RequiredArgsConstructor
public class PermissionService {

    private final StaffRepository staffRepository;

    /**
     * Cache entry: staffId → (permissions, expiryMs)
     */
    private record CacheEntry(Set<StaffPermission> permissions, long expiryMs) {
        boolean isExpired() {
            return System.currentTimeMillis() > expiryMs;
        }
    }

    private static final long CACHE_TTL_MS = 5 * 60 * 1000L; // 5 دقائق

    private final Map<Long, CacheEntry> permCache = new ConcurrentHashMap<>();

    // ─────────────────────────────────────────────────────────────
    // Main gate — used by @PreAuthorize
    // ─────────────────────────────────────────────────────────────

    /**
     * يتحقق إذا كان المستخدم الحالي عنده الصلاحية المطلوبة.
     *
     * @param authentication الـ Authentication object من Spring Security
     * @param permissionName اسم الصلاحية (مثال: "APPROVE_STUDENTS")
     * @return true لو مسموح
     */
    public boolean can(Authentication authentication, String permissionName) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        Object principal = authentication.getPrincipal();
        if (!(principal instanceof JwtUserPrincipal user)) {
            return false;
        }

        String role = user.getRole();

        // TEACHER و ADMIN عندهم كل الصلاحيات
        if ("TEACHER".equals(role) || "ADMIN".equals(role)) {
            return true;
        }

        // STAFF — بيتحقق من الصلاحيات الخاصة به
        if ("STAFF".equals(role)) {
            return checkStaffPermission(user.getUserId(), permissionName);
        }

        return false;
    }

    /**
     * نفس can() لكن بيقبل enum مباشرة (للاستخدام من Java code مش @PreAuthorize).
     */
    public boolean can(Authentication authentication, StaffPermission permission) {
        return can(authentication, permission.name());
    }

    // ─────────────────────────────────────────────────────────────
    // Cache invalidation — يتعمل لما المدرس يعدّل صلاحيات موظف
    // ─────────────────────────────────────────────────────────────

    /**
     * يحذف الصلاحيات المحفوظة للموظف من الـ cache.
     * لازم يتعمل بعد أي تعديل على صلاحيات موظف معين.
     */
    public void invalidateCache(Long staffId) {
        permCache.remove(staffId);
        log.debug("Permission cache invalidated for staffId={}", staffId);
    }

    /**
     * يحذف الـ cache بالكامل — للاستخدام في الطوارئ أو التست.
     */
    public void invalidateAllCache() {
        permCache.clear();
        log.info("Permission cache fully cleared");
    }

    // ─────────────────────────────────────────────────────────────
    // Internal helpers
    // ─────────────────────────────────────────────────────────────

    private boolean checkStaffPermission(Long staffId, String permissionName) {
        Set<StaffPermission> permissions = getPermissionsFromCacheOrDb(staffId);
        if (permissions == null) {
            log.warn("Staff not found or inactive — staffId={}", staffId);
            return false;
        }

        try {
            StaffPermission required = StaffPermission.valueOf(permissionName);
            boolean allowed = permissions.contains(required);
            if (!allowed) {
                log.debug("STAFF permission denied — staffId={}, required={}", staffId, permissionName);
            }
            return allowed;
        } catch (IllegalArgumentException e) {
            log.warn("Unknown permission name requested: {}", permissionName);
            return false;
        }
    }

    private Set<StaffPermission> getPermissionsFromCacheOrDb(Long staffId) {
        CacheEntry cached = permCache.get(staffId);

        if (cached != null && !cached.isExpired()) {
            return cached.permissions();
        }

        // Cache miss أو منتهي — نجيب من الـ DB
        return staffRepository.findById(staffId)
                .filter(Staff::isActive)
                .map(staff -> {
                    Set<StaffPermission> perms = staff.getPermissions();
                    permCache.put(staffId, new CacheEntry(perms, System.currentTimeMillis() + CACHE_TTL_MS));
                    return perms;
                })
                .orElse(null);
    }

    // ─────────────────────────────────────────────────────────────
    // Periodic cleanup — كل 10 دقائق
    // ─────────────────────────────────────────────────────────────

    @Scheduled(fixedDelay = 10 * 60 * 1000L)
    public void cleanupExpiredCache() {
        int before = permCache.size();
        permCache.entrySet().removeIf(e -> e.getValue().isExpired());
        int removed = before - permCache.size();
        if (removed > 0) {
            log.debug("Permission cache cleanup: removed {} expired entries", removed);
        }
    }
}
