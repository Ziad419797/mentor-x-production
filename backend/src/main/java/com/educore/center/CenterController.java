package com.educore.center;

import com.educore.center.dto.CenterRequest;
import com.educore.center.dto.CenterResponse;
import com.educore.common.GlobalResponse;
import com.educore.security.JwtUserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Center (Branch) API
 *
 * Public (no auth):
 *   GET  /api/centers                            → كل السناتر النشطة
 *   GET  /api/centers/governorate/{governorate}  → سناتر محافظة معينة
 *   GET  /api/centers/{id}                       → تفاصيل سنتر واحد (روابط سوشيال)
 *
 * Teacher/Admin:
 *   GET    /api/centers/all          → كل السناتر (نشطة + غير نشطة)
 *   POST   /api/centers              → إنشاء سنتر
 *   PUT    /api/centers/{id}         → تعديل سنتر
 *   DELETE /api/centers/{id}         → إلغاء تفعيل سنتر
 */
@RestController
@RequestMapping("/api/centers")
@RequiredArgsConstructor
@Tag(name = "Centers", description = "إدارة السناتر وروابط التواصل الاجتماعي")
public class CenterController {

    private final CenterService centerService;

    // ─── Public ──────────────────────────────────────────────────

    @Operation(summary = "كل السناتر النشطة — للتسجيل والاختيار")
    @GetMapping
    public ResponseEntity<GlobalResponse<List<CenterResponse>>> getAllActive() {
        return ResponseEntity.ok(GlobalResponse.success(centerService.getAllActive()));
    }

    @Operation(summary = "سناتر محافظة معينة")
    @GetMapping("/governorate/{governorate}")
    public ResponseEntity<GlobalResponse<List<CenterResponse>>> getByGovernorate(
            @PathVariable String governorate) {
        return ResponseEntity.ok(GlobalResponse.success(centerService.getByGovernorate(governorate)));
    }

    @Operation(summary = "تفاصيل سنتر واحد (مع روابط سوشيال)")
    @GetMapping("/{id}")
    public ResponseEntity<GlobalResponse<CenterResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(GlobalResponse.success(centerService.getById(id)));
    }

    // ─── Teacher / Admin ──────────────────────────────────────────

    @Operation(summary = "كل السناتر — نشطة وغير نشطة (للإدارة)")
    @GetMapping("/all")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public ResponseEntity<GlobalResponse<List<CenterResponse>>> getAll() {
        return ResponseEntity.ok(GlobalResponse.success(centerService.getAll()));
    }

    @Operation(summary = "إنشاء سنتر جديد")
    @PostMapping
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public ResponseEntity<GlobalResponse<CenterResponse>> create(
            @Valid @RequestBody CenterRequest request,
            @AuthenticationPrincipal JwtUserPrincipal principal) {
        CenterResponse created = centerService.create(request, principal.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(GlobalResponse.success("تم إنشاء السنتر بنجاح", created));
    }

    @Operation(summary = "تعديل سنتر")
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public ResponseEntity<GlobalResponse<CenterResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody CenterRequest request) {
        return ResponseEntity.ok(GlobalResponse.success("تم تحديث السنتر", centerService.update(id, request)));
    }

    @Operation(summary = "إلغاء تفعيل سنتر (soft delete)")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public ResponseEntity<GlobalResponse<Void>> delete(@PathVariable Long id) {
        centerService.delete(id);
        return ResponseEntity.ok(GlobalResponse.success("تم إلغاء تفعيل السنتر", null));
    }
}
