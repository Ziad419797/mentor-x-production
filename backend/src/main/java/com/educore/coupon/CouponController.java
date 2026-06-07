package com.educore.coupon;

import com.educore.common.GlobalResponse;
import com.educore.coupon.dto.*;
import com.educore.security.JwtUserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Coupon API
 *
 * POST /api/coupons                  → إنشاء كوبون (TEACHER/ADMIN)
 * GET  /api/coupons                  → قائمة الكوبونات (TEACHER/ADMIN)
 * GET  /api/coupons/valid            → الكوبونات الصالحة (TEACHER/ADMIN)
 * GET  /api/coupons/{id}             → تفاصيل كوبون
 * PUT  /api/coupons/{id}             → تعديل
 * PATCH /api/coupons/{id}/toggle     → تفعيل/تعطيل
 * POST /api/coupons/preview          → الطالب يجرب الكوبون قبل الدفع
 */
@RestController
@RequestMapping("/api/coupons")
@RequiredArgsConstructor
public class CouponController {

    private final CouponService couponService;

    // ─── إدارة الكوبونات (TEACHER/ADMIN) ─────────────────────────

    @PostMapping
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public ResponseEntity<GlobalResponse<CouponResponse>> create(
            @Valid @RequestBody CouponRequest request,
            @AuthenticationPrincipal JwtUserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(GlobalResponse.success("تم إنشاء الكوبون",
                        couponService.create(request, principal.getUsername())));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public ResponseEntity<GlobalResponse<Page<CouponResponse>>> getAll(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(GlobalResponse.success(couponService.getAll(pageable)));
    }

    @GetMapping("/valid")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN','STUDENT')")
    public ResponseEntity<GlobalResponse<List<CouponResponse>>> getValid() {
        return ResponseEntity.ok(GlobalResponse.success(couponService.getValidCoupons()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public ResponseEntity<GlobalResponse<CouponResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(GlobalResponse.success(couponService.getById(id)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public ResponseEntity<GlobalResponse<CouponResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody CouponRequest request) {
        return ResponseEntity.ok(GlobalResponse.success("تم تعديل الكوبون",
                couponService.update(id, request)));
    }

    @PatchMapping("/{id}/toggle")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public ResponseEntity<GlobalResponse<CouponResponse>> toggle(@PathVariable Long id) {
        return ResponseEntity.ok(GlobalResponse.success(couponService.toggleActive(id)));
    }

    // ─── للطالب: تجربة الكوبون قبل الدفع ────────────────────────

    @PostMapping("/preview")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<GlobalResponse<ApplyCouponResponse>> preview(
            @Valid @RequestBody ApplyCouponRequest request) {
        return ResponseEntity.ok(GlobalResponse.success(
                "تم حساب الخصم", couponService.preview(request)));
    }
}
