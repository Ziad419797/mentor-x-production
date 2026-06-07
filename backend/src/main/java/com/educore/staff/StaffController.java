package com.educore.staff;

import com.educore.common.GlobalResponse;

import com.educore.security.JwtUserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * StaffController — إدارة الموظفين.
 *
 * كل الـ endpoints هنا للمدرس فقط (hasRole TEACHER أو ADMIN).
 * الموظف (STAFF) ما يدخلش هنا — هو بيستخدم الـ endpoints التانية
 * حسب الصلاحيات المحددة له.
 *
 * Base path: /api/teacher/staff
 */
@RestController
@RequestMapping("/api/teacher/staff")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
public class StaffController {

    private final StaffService staffService;

    // ─────────────────────────────────────────────────────────────
    // Permissions list — لعرض الـ checkboxes في الـ UI
    // ─────────────────────────────────────────────────────────────

    /**
     * GET /api/teacher/staff/permissions
     * يرجع كل الصلاحيات المتاحة مع أسمائها العربية.
     */
    @GetMapping("/permissions")
    public ResponseEntity<GlobalResponse<List<StaffService.PermissionInfo>>> getAvailablePermissions() {
        return ResponseEntity.ok(
                GlobalResponse.success("قائمة الصلاحيات المتاحة",staffService.getAllAvailablePermissions())
        );
    }

    // ─────────────────────────────────────────────────────────────
    // CRUD
    // ─────────────────────────────────────────────────────────────

    /**
     * POST /api/teacher/staff
     * إنشاء موظف جديد.
     */
    @PostMapping
    public ResponseEntity<GlobalResponse<StaffResponse>> createStaff(
            @AuthenticationPrincipal JwtUserPrincipal principal,
            @Valid @RequestBody StaffCreateRequest req) {

        StaffResponse response = staffService.createStaff(principal.getUserId(), req);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(GlobalResponse.success( "تم إنشاء الموظف بنجاح",response));
    }

    /**
     * GET /api/teacher/staff
     * قائمة موظفي المدرس.
     */
    @GetMapping
    public ResponseEntity<GlobalResponse<List<StaffResponse>>> getMyStaff(
            @AuthenticationPrincipal JwtUserPrincipal principal) {

        return ResponseEntity.ok(
                GlobalResponse.success(staffService.getStaffByTeacher(principal.getUserId()))
        );
    }

    /**
     * GET /api/teacher/staff/{id}
     * بيانات موظف واحد.
     */
    @GetMapping("/{id}")
    public ResponseEntity<GlobalResponse<StaffResponse>> getStaffById(
            @AuthenticationPrincipal JwtUserPrincipal principal,
            @PathVariable Long id) {

        return ResponseEntity.ok(
                GlobalResponse.success(staffService.getStaffById(principal.getUserId(), id))
        );
    }

    /**
     * PUT /api/teacher/staff/{id}
     * تعديل بيانات الموظف (الاسم / كلمة المرور / الملاحظات / التفعيل).
     */
    @PutMapping("/{id}")
    public ResponseEntity<GlobalResponse<StaffResponse>> updateStaff(
            @AuthenticationPrincipal JwtUserPrincipal principal,
            @PathVariable Long id,
            @Valid @RequestBody StaffUpdateRequest req) {

        return ResponseEntity.ok(
                GlobalResponse.success("تم التحديث بنجاح",staffService.updateStaff(principal.getUserId(), id, req))
        );
    }

    /**
     * PUT /api/teacher/staff/{id}/permissions
     * تحديث صلاحيات الموظف (يحل الـ Set الجديد محل القديم).
     */
    @PutMapping("/{id}/permissions")
    public ResponseEntity<GlobalResponse<StaffResponse>> updatePermissions(
            @AuthenticationPrincipal JwtUserPrincipal principal,
            @PathVariable Long id,
            @Valid @RequestBody UpdatePermissionsRequest req) {

        return ResponseEntity.ok(
                GlobalResponse.success(
                        "تم تحديث الصلاحيات بنجاح",
                        staffService.updatePermissions(principal.getUserId(), id, req)

                )
        );
    }

    /**
     * PATCH /api/teacher/staff/{id}/toggle
     * تفعيل / تعطيل حساب الموظف.
     */
    @PatchMapping("/{id}/toggle")
    public ResponseEntity<GlobalResponse<StaffResponse>> toggleActive(
            @AuthenticationPrincipal JwtUserPrincipal principal,
            @PathVariable Long id) {

        return ResponseEntity.ok(
                GlobalResponse.success("تم التحديث بنجاح",staffService.toggleActive(principal.getUserId(), id))
        );
    }

    /**
     * DELETE /api/teacher/staff/{id}
     * حذف الموظف نهائياً.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<GlobalResponse<Void>> deleteStaff(
            @AuthenticationPrincipal JwtUserPrincipal principal,
            @PathVariable Long id) {

        staffService.deleteStaff(principal.getUserId(), id);
        return ResponseEntity.ok(GlobalResponse.success( "تم التحديث بنجاح",null));
    }
}
