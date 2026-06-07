package com.educore.studentcard;

import com.educore.common.GlobalResponse;
import com.educore.security.JwtUserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/cards")
@RequiredArgsConstructor
@Tag(name = "Student Cards", description = "إصدار والتحقق من كارنيهات الطلاب (QR)")
public class StudentCardController {

    private final StudentCardService cardService;

    // ─────────────────────────────────────────────────────────────
    // إصدار كارنيه — يقوم بيه الأدمن أو المدرس
    // ─────────────────────────────────────────────────────────────

    @Operation(summary = "[ADMIN/TEACHER] إصدار كارنيه لطالب")
    @PostMapping("/issue/{studentId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ResponseEntity<GlobalResponse<StudentCardResponse>> issueCard(
            @PathVariable Long studentId,
            @AuthenticationPrincipal JwtUserPrincipal principal) {

        StudentCardResponse card = cardService.issueCard(
                studentId, principal.getUsername());

        return ResponseEntity.ok(GlobalResponse.<StudentCardResponse>builder()
                .success(true)
                .message("تم إصدار الكارنيه بنجاح")
                .data(card)
                .build());
    }

    // ─────────────────────────────────────────────────────────────
    // الطالب يشوف كارنيهه
    // ─────────────────────────────────────────────────────────────

    @Operation(summary = "[STUDENT] بيانات كارنيهي")
    @GetMapping("/my")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<GlobalResponse<StudentCardResponse>> getMyCard(
            @AuthenticationPrincipal JwtUserPrincipal principal) {

        StudentCardResponse card = cardService.getMyCard(principal.getUserId());

        return ResponseEntity.ok(GlobalResponse.<StudentCardResponse>builder()
                .success(true)
                .message("تم جلب بيانات الكارنيه")
                .data(card)
                .build());
    }

    // ─────────────────────────────────────────────────────────────
    // التحقق من الـ QR عند الـ Scan في السنتر
    // ─────────────────────────────────────────────────────────────

    @Operation(
        summary     = "[TEACHER] التحقق من QR Token",
        description = "يُستدعى لما الموظف يعمل Scan — يرجع بيانات الطالب لو التوكن صحيح"
    )
    @GetMapping("/validate/{qrToken}")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public ResponseEntity<GlobalResponse<StudentCardResponse>> validateCard(
            @PathVariable String qrToken) {

        StudentCardResponse card = cardService.validateQrToken(qrToken);

        return ResponseEntity.ok(GlobalResponse.<StudentCardResponse>builder()
                .success(true)
                .message("الكارنيه صحيح")
                .data(card)
                .build());
    }
}
