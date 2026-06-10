package com.educore.parent;

import com.educore.common.GlobalResponse;
import com.educore.parent.dto.ChildAssignmentResultDto;
import com.educore.parent.dto.ChildQuizResultDto;
import com.educore.security.JwtUserPrincipal;
import com.educore.wallet.dto.WalletResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/parent/children")
@RequiredArgsConstructor
@PreAuthorize("hasRole('PARENT')")
@Tag(name = "Parent - Child Details", description = "ولي الأمر — تفاصيل الابن")
public class ParentChildController {

    private final ParentChildService parentChildService;

    // ─────────────────────────────────────────────────────────────
    // GET /api/parent/children/{studentId}/quizzes
    // ─────────────────────────────────────────────────────────────

    @Operation(summary = "نتائج امتحانات الابن")
    @GetMapping("/{studentId}/quizzes")
    public ResponseEntity<GlobalResponse<Page<ChildQuizResultDto>>> getQuizResults(
            @PathVariable Long studentId,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal JwtUserPrincipal principal) {

        Page<ChildQuizResultDto> results = parentChildService.getQuizResults(
                principal.getUserId(), studentId,
                PageRequest.of(page, size, Sort.by("submittedAt").descending()));

        return ResponseEntity.ok(GlobalResponse.success("تم جلب نتائج الامتحانات", results));
    }

    // ─────────────────────────────────────────────────────────────
    // GET /api/parent/children/{studentId}/assignments
    // ─────────────────────────────────────────────────────────────

    @Operation(summary = "نتائج واجبات الابن")
    @GetMapping("/{studentId}/assignments")
    public ResponseEntity<GlobalResponse<Page<ChildAssignmentResultDto>>> getAssignmentResults(
            @PathVariable Long studentId,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal JwtUserPrincipal principal) {

        Page<ChildAssignmentResultDto> results = parentChildService.getAssignmentResults(
                principal.getUserId(), studentId,
                PageRequest.of(page, size, Sort.by("submittedAt").descending()));

        return ResponseEntity.ok(GlobalResponse.success("تم جلب نتائج الواجبات", results));
    }

    // ─────────────────────────────────────────────────────────────
    // GET /api/parent/children/{studentId}/wallet
    // ─────────────────────────────────────────────────────────────

    @Operation(summary = "بيانات محفظة الابن")
    @GetMapping("/{studentId}/wallet")
    public ResponseEntity<GlobalResponse<WalletResponse>> getWallet(
            @PathVariable Long studentId,
            @AuthenticationPrincipal JwtUserPrincipal principal) {

        WalletResponse wallet = parentChildService.getWallet(principal.getUserId(), studentId);
        return ResponseEntity.ok(GlobalResponse.success("تم جلب بيانات المحفظة", wallet));
    }
}
