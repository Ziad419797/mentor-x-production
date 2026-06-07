package com.educore.banner;

import com.educore.banner.dto.request.CreateBannerRequest;
import com.educore.banner.dto.request.UpdateBannerRequest;
import com.educore.banner.dto.response.BannerResponse;
import com.educore.common.GlobalResponse;
import com.educore.security.JwtUserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/banners")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Banners", description = "إدارة البانرات")
public class BannerController {

    private final BannerService bannerService;

    // ================= للمدرس (Admin/Teacher) =================

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "إنشاء بانر جديد (للمدرس)")
    public ResponseEntity<GlobalResponse<BannerResponse>> createBanner(
            @Valid @ModelAttribute CreateBannerRequest request,
            @RequestPart(value = "imageFile", required = false) MultipartFile imageFile,
            @AuthenticationPrincipal JwtUserPrincipal principal) {
        if (imageFile != null && !imageFile.isEmpty()) {
            request.setImageFile(imageFile);
        }
        BannerResponse response = bannerService.createBanner(request, principal);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(GlobalResponse.success("تم إنشاء البانر بنجاح", response));
    }

    @GetMapping("/admin")
    @Operation(summary = "جلب جميع البانرات (للمدرس)")
    public ResponseEntity<GlobalResponse<Page<BannerResponse>>> getAllBanners(
            @PageableDefault(size = 10, sort = "displayOrder", direction = Sort.Direction.ASC) Pageable pageable) {

        Page<BannerResponse> banners = bannerService.getAllBanners(pageable);
        return ResponseEntity.ok(GlobalResponse.success(banners));
    }

    @GetMapping("/admin/{id}")
    @Operation(summary = "جلب بانر محدد (للمدرس)")
    public ResponseEntity<GlobalResponse<BannerResponse>> getBannerById(@PathVariable Long id) {

        BannerResponse banner = bannerService.getBannerById(id);
        return ResponseEntity.ok(GlobalResponse.success(banner));
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "تحديث بانر (للمدرس)")
    public ResponseEntity<GlobalResponse<BannerResponse>> updateBanner(
            @PathVariable Long id,
            @Valid @ModelAttribute UpdateBannerRequest request,
            @RequestPart(value = "imageFile", required = false) MultipartFile imageFile) {

        if (imageFile != null && !imageFile.isEmpty()) {
            request.setImageFile(imageFile);
        }

        BannerResponse response = bannerService.updateBanner(id, request);
        return ResponseEntity.ok(GlobalResponse.success("تم تحديث البانر بنجاح", response));
    }

    @PatchMapping("/{id}/toggle-status")
    @Operation(summary = "تفعيل/إلغاء بانر (للمدرس)")
    public ResponseEntity<GlobalResponse<BannerResponse>> toggleBannerStatus(@PathVariable Long id) {

        BannerResponse response = bannerService.toggleBannerStatus(id);
        return ResponseEntity.ok(GlobalResponse.success("تم تغيير حالة البانر بنجاح", response));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "حذف بانر (للمدرس)")
    public ResponseEntity<GlobalResponse<Void>> deleteBanner(@PathVariable Long id) {

        bannerService.deleteBanner(id);
        return ResponseEntity.ok(GlobalResponse.success("تم حذف البانر بنجاح", null));
    }

    // ================= للطالب (Public) =================

    // في BannerController.java - أضف Pagination للبانرات النشطة

    @GetMapping("/active")
    @Operation(summary = "جلب البانرات النشطة (للطالب) مع Pagination")
    public ResponseEntity<GlobalResponse<Page<BannerResponse>>> getActiveBanners(
            @PageableDefault(size = 10, sort = "displayOrder", direction = Sort.Direction.ASC) Pageable pageable) {

        Page<BannerResponse> banners = bannerService.getActiveBanners(pageable);
        return ResponseEntity.ok(GlobalResponse.success(banners));
    }
}