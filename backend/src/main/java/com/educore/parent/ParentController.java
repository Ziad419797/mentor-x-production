package com.educore.parent;

import com.educore.common.GlobalResponse;
import com.educore.parent.dto.ChildSummaryDto;
import com.educore.security.JwtUserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Parent root controller.
 * Provides the child-selector endpoint that the app shows right after
 * the parent logs in (before they choose which child to view).
 *
 * GET /api/parent/children  → list all children of the authenticated parent
 */
@Slf4j
@RestController
@RequestMapping("/api/parent")
@RequiredArgsConstructor
@PreAuthorize("hasRole('PARENT')")
@Tag(name = "Parent", description = "ولي الأمر — اختيار الابن")
public class ParentController {

    private final ParentProfileService parentProfileService;

    // ─────────────────────────────────────────────────────────────
    // GET /api/parent/children
    // ─────────────────────────────────────────────────────────────

    @Operation(summary = "قائمة أبناء ولي الأمر — يظهر بعد تسجيل الدخول لاختيار الابن المطلوب")
    @GetMapping("/children")
    public ResponseEntity<GlobalResponse<List<ChildSummaryDto>>> getChildren(
            @AuthenticationPrincipal JwtUserPrincipal principal) {

        log.debug("GET /api/parent/children — parentId: {}", principal.getUserId());

        List<ChildSummaryDto> children = parentProfileService.getChildren(principal.getUserId());

        return ResponseEntity.ok(GlobalResponse.<List<ChildSummaryDto>>builder()
                .success(true)
                .message("تم جلب قائمة الأبناء بنجاح — اختر الابن للمتابعة")
                .data(children)
                .build());
    }
}
