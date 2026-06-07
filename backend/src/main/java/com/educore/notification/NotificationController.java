package com.educore.notification;

import com.educore.common.GlobalResponse;
import com.educore.security.JwtUserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications", description = "الإشعارات الداخلية للتطبيق")
public class NotificationController {

    private final NotificationService notificationService;

    // ─────────────────────────────────────────────────────────────
    // GET كل الإشعارات أو الغير مقروءة فقط
    // ─────────────────────────────────────────────────────────────

    @Operation(summary = "إشعاراتي (كلها أو الغير مقروءة)")
    @GetMapping("/my")
    public ResponseEntity<GlobalResponse<Page<NotificationResponse>>> getMyNotifications(
            @AuthenticationPrincipal JwtUserPrincipal principal,
            @RequestParam(defaultValue = "false") boolean unreadOnly,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable) {

        Page<NotificationResponse> page = notificationService.getMyNotifications(
                principal.getUserId(), principal.getRole(), unreadOnly, pageable);

        return ResponseEntity.ok(GlobalResponse.<Page<NotificationResponse>>builder()
                .success(true)
                .message("تم جلب الإشعارات")
                .data(page)
                .build());
    }

    // ─────────────────────────────────────────────────────────────
    // GET عدد الإشعارات الغير مقروءة — لـ badge في الـ UI
    // ─────────────────────────────────────────────────────────────

    @Operation(summary = "عدد الإشعارات الغير مقروءة")
    @GetMapping("/unread-count")
    public ResponseEntity<GlobalResponse<Map<String, Long>>> getUnreadCount(
            @AuthenticationPrincipal JwtUserPrincipal principal) {

        long count = notificationService.getUnreadCount(
                principal.getUserId(), principal.getRole());

        return ResponseEntity.ok(GlobalResponse.<Map<String, Long>>builder()
                .success(true)
                .message("تم جلب العدد")
                .data(Map.of("unreadCount", count))
                .build());
    }

    // ─────────────────────────────────────────────────────────────
    // PATCH تحديد إشعار واحد كمقروء
    // ─────────────────────────────────────────────────────────────

    @Operation(summary = "تحديد إشعار كمقروء")
    @PatchMapping("/{id}/read")
    public ResponseEntity<GlobalResponse<Void>> markRead(
            @PathVariable Long id,
            @AuthenticationPrincipal JwtUserPrincipal principal) {

        notificationService.markRead(id, principal.getUserId());

        return ResponseEntity.ok(GlobalResponse.<Void>builder()
                .success(true)
                .message("تم تحديد الإشعار كمقروء")
                .build());
    }

    // ─────────────────────────────────────────────────────────────
    // PATCH تحديد كل الإشعارات كمقروءة
    // ─────────────────────────────────────────────────────────────

    @Operation(summary = "تحديد كل الإشعارات كمقروءة")
    @PatchMapping("/read-all")
    public ResponseEntity<GlobalResponse<Map<String, Integer>>> markAllRead(
            @AuthenticationPrincipal JwtUserPrincipal principal) {

        int updated = notificationService.markAllRead(
                principal.getUserId(), principal.getRole());

        return ResponseEntity.ok(GlobalResponse.<Map<String, Integer>>builder()
                .success(true)
                .message("تم تحديد " + updated + " إشعار كمقروء")
                .data(Map.of("updatedCount", updated))
                .build());
    }
}
