package com.educore.banner.announcements;

import com.educore.banner.announcements.dto.request.CreateAnnouncementRequest;
import com.educore.banner.announcements.dto.request.UpdateAnnouncementRequest;
import com.educore.banner.announcements.dto.response.AnnouncementResponse;
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
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/announcements")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Announcements", description = "إدارة الإعلانات")
public class AnnouncementController {

    private final AnnouncementService announcementService;

    // ================= للمدرس =================

    @PostMapping
    @Operation(summary = "إنشاء إعلان جديد")
    public ResponseEntity<GlobalResponse<AnnouncementResponse>> createAnnouncement(
            @Valid @RequestBody CreateAnnouncementRequest request,
            @AuthenticationPrincipal JwtUserPrincipal principal) {

        AnnouncementResponse response = announcementService.createAnnouncement(request, principal);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(GlobalResponse.success("تم إنشاء الإعلان بنجاح", response));
    }

    // ✅ للمدرس - مع Pagination & Sorting
    @GetMapping("/admin")
    @Operation(summary = "جلب جميع الإعلانات (مع Pagination & Sorting)")
    public ResponseEntity<GlobalResponse<Page<AnnouncementResponse>>> getAllAnnouncements(
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        Page<AnnouncementResponse> announcements = announcementService.getAllAnnouncements(pageable);
        return ResponseEntity.ok(GlobalResponse.success(announcements));
    }

    @GetMapping("/admin/{id}")
    @Operation(summary = "جلب إعلان محدد")
    public ResponseEntity<GlobalResponse<AnnouncementResponse>> getAnnouncementById(@PathVariable Long id) {

        AnnouncementResponse announcement = announcementService.getAnnouncementById(id);
        return ResponseEntity.ok(GlobalResponse.success(announcement));
    }

    @PutMapping("/{id}")
    @Operation(summary = "تحديث إعلان")
    public ResponseEntity<GlobalResponse<AnnouncementResponse>> updateAnnouncement(
            @PathVariable Long id,
            @Valid @RequestBody UpdateAnnouncementRequest request) {

        AnnouncementResponse response = announcementService.updateAnnouncement(id, request);
        return ResponseEntity.ok(GlobalResponse.success("تم تحديث الإعلان بنجاح", response));
    }

    @PatchMapping("/{id}/toggle-status")
    @Operation(summary = "تفعيل/إلغاء إعلان")
    public ResponseEntity<GlobalResponse<AnnouncementResponse>> toggleAnnouncementStatus(@PathVariable Long id) {

        AnnouncementResponse response = announcementService.toggleAnnouncementStatus(id);
        return ResponseEntity.ok(GlobalResponse.success("تم تغيير حالة الإعلان بنجاح", response));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "حذف إعلان")
    public ResponseEntity<GlobalResponse<Void>> deleteAnnouncement(@PathVariable Long id) {

        announcementService.deleteAnnouncement(id);
        return ResponseEntity.ok(GlobalResponse.success("تم حذف الإعلان بنجاح", null));
    }

    // ================= للطالب (Public) - مع Pagination =================
    @GetMapping("/active")
    @Operation(summary = "جلب الإعلانات النشطة (للطالب) مع Pagination")
    public ResponseEntity<GlobalResponse<Page<AnnouncementResponse>>> getActiveAnnouncements(
            @PageableDefault(size = 10, sort = "announcementDate", direction = Sort.Direction.DESC) Pageable pageable) {

        Page<AnnouncementResponse> announcements = announcementService.getActiveAnnouncements(pageable);
        return ResponseEntity.ok(GlobalResponse.success(announcements));
    }
}