package com.educore.copon;

import com.educore.dtocopon.*;
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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/access-codes")
@RequiredArgsConstructor
@Tag(name = " Access Codes", description = "أكواد الوصول — المدرس يولد، الطالب يُدخل")
public class AccessCodeController {

    private final AccessCodeService accessCodeService;

    /* ════════════════════════════════════════════════════
       TEACHER — توليد وإدارة الأكواد
    ════════════════════════════════════════════════════ */

    @Operation(summary = "[TEACHER] توليد دفعة من الأكواد")
    @PostMapping("/generate")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public ResponseEntity<GenerateCodesResponse> generate(
            @Valid @RequestBody GenerateCodesRequest request,
            @AuthenticationPrincipal JwtUserPrincipal principal
    ) {
        log.info("Teacher {} generating {} codes", principal.getUserId(), request.getCount());
        GenerateCodesResponse response = accessCodeService.generateCodes(
                request, principal.getUserId(), principal.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "[TEACHER] أكوادي")
    @GetMapping("/my-codes")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public ResponseEntity<Page<AccessCodeDto>> getMyCodes(
            @AuthenticationPrincipal JwtUserPrincipal principal,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable
    ) {
        return ResponseEntity.ok(
                accessCodeService.getTeacherCodes(principal.getUserId(), pageable));
    }

    @Operation(summary = "[TEACHER] أكواد دفعة محددة")
    @GetMapping("/batch/{batchLabel}")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public ResponseEntity<List<AccessCodeDto>> getBatch(
            @PathVariable String batchLabel,
            @AuthenticationPrincipal JwtUserPrincipal principal
    ) {
        return ResponseEntity.ok(
                accessCodeService.getBatch(principal.getUserId(), batchLabel));
    }

    @Operation(summary = "[TEACHER] من استخدم هذا الكود؟")
    @GetMapping("/{codeId}/usages")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public ResponseEntity<List<CodeUsageDto>> getUsages(
            @PathVariable Long codeId,
            @AuthenticationPrincipal JwtUserPrincipal principal
    ) {
        return ResponseEntity.ok(
                accessCodeService.getCodeUsages(codeId, principal.getUserId()));
    }

    @Operation(summary = "[TEACHER] إلغاء كود واحد")
    @DeleteMapping("/{codeId}")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public ResponseEntity<Map<String, String>> deactivate(
            @PathVariable Long codeId,
            @AuthenticationPrincipal JwtUserPrincipal principal
    ) {
        accessCodeService.deactivateCode(codeId, principal.getUserId());
        return ResponseEntity.ok(Map.of("message", "تم إلغاء الكود بنجاح"));
    }

    @Operation(summary = "[TEACHER] إلغاء دفعة كاملة")
    @DeleteMapping("/batch/{batchLabel}")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public ResponseEntity<Map<String, Object>> deactivateBatch(
            @PathVariable String batchLabel,
            @AuthenticationPrincipal JwtUserPrincipal principal
    ) {
        int count = accessCodeService.deactivateBatch(batchLabel, principal.getUserId());
        return ResponseEntity.ok(Map.of(
                "deactivatedCount", count,
                "message", "تم إلغاء " + count + " كود بنجاح"
        ));
    }

    /* ════════════════════════════════════════════════════
       STUDENT — تفعيل الكود
    ════════════════════════════════════════════════════ */

    @Operation(summary = "[STUDENT] تفعيل كود وصول")
    @PostMapping("/redeem")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<RedeemCodeResponse> redeem(
            @Valid @RequestBody RedeemCodeRequest request,
            @AuthenticationPrincipal JwtUserPrincipal principal,
            @RequestHeader("X-Device-Id") String deviceId
    ) {
        log.info("Student {} redeeming code: {}", principal.getUserId(), request.getCode());
        RedeemCodeResponse response = accessCodeService.redeemCode(request, principal.getUserId());
        return ResponseEntity.ok(response);
    }
}