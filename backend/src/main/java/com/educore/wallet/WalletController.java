package com.educore.wallet;

import com.educore.common.GlobalResponse;
import com.educore.dtopayment.WalletTransactionDto;
import com.educore.dtopayment.mapper.PaymentMapper;
import com.educore.payment.fawaterek.FawaterekClient;
import com.educore.payment.payment.TransactionType;
import com.educore.security.JwtUserPrincipal;
import com.educore.student.Student;
import com.educore.student.StudentRepository;
import com.educore.wallet.dto.WalletOnlineDepositRequest;
import com.educore.wallet.dto.WalletResponse;
import com.educore.wallet.dto.WalletStatsResponse;
import com.educore.wallet.dto.WalletTopUpRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Wallet API
 *
 * GET  /api/wallet/my                         → الطالب يعرض محفظته
 * GET  /api/wallet/student/{id}               → الأدمن/المدرس يعرض محفظة طالب
 * POST /api/wallet/top-up                     → الأدمن/المدرس يشحن محفظة
 * GET  /api/wallet/student/{id}/transactions  → سجل المعاملات (paginated)
 */
@RestController
@RequestMapping("/api/wallet")
@RequiredArgsConstructor
public class WalletController {

    private final WalletService    walletService;
    private final PaymentMapper    paymentMapper;
    private final FawaterekClient  fawaterekClient;
    private final StudentRepository studentRepository;

    // ─── الطالب يعرض محفظته ──────────────────────────────────────

    @GetMapping("/my")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<GlobalResponse<WalletResponse>> getMyWallet(
            @AuthenticationPrincipal JwtUserPrincipal principal) {
        return ResponseEntity.ok(GlobalResponse.success(
                walletService.getWallet(principal.getUserId())));
    }

    // ─── الأدمن/المدرس يعرض محفظة طالب معين ─────────────────────

    @GetMapping("/student/{studentId}")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN') or @perm.can(authentication,'TOP_UP_WALLET')")
    public ResponseEntity<GlobalResponse<WalletResponse>> getStudentWallet(
            @PathVariable Long studentId) {
        return ResponseEntity.ok(GlobalResponse.success(
                walletService.getWallet(studentId)));
    }

    // ─── شحن محفظة طالب ──────────────────────────────────────────

    @PostMapping("/top-up")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN') or @perm.can(authentication,'TOP_UP_WALLET')")
    public ResponseEntity<GlobalResponse<WalletResponse>> topUp(
            @Valid @RequestBody WalletTopUpRequest request,
            @AuthenticationPrincipal JwtUserPrincipal principal) {
        String chargedByName = principal.getName() != null && !principal.getName().isBlank()
                ? principal.getName()
                : principal.getUsername();
        WalletResponse response = walletService.topUp(request, chargedByName);
        return ResponseEntity.ok(GlobalResponse.success(
                "تم شحن المحفظة بنجاح — " + request.getAmount() + " جنيه", response));
    }

    // ─── سجل معاملات طالب ────────────────────────────────────────

    @GetMapping("/student/{studentId}/transactions")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN') or @perm.can(authentication,'TOP_UP_WALLET')")
    public ResponseEntity<GlobalResponse<Page<WalletTransactionDto>>> getTransactions(
            @PathVariable Long studentId,
            @PageableDefault(size = 50, sort = "createdAt") Pageable pageable) {
        return ResponseEntity.ok(GlobalResponse.success(
                walletService.getTransactions(studentId, pageable)
                             .map(paymentMapper::toWalletTransactionDto)));
    }

    @GetMapping("/my/transactions")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<GlobalResponse<Page<WalletTransactionDto>>> getMyTransactions(
            @AuthenticationPrincipal JwtUserPrincipal principal,
            @PageableDefault(size = 50, sort = "createdAt") Pageable pageable) {
        return ResponseEntity.ok(GlobalResponse.success(
                walletService.getTransactions(principal.getUserId(), pageable)
                             .map(paymentMapper::toWalletTransactionDto)));
    }

    // ─── كل عمليات الشحن (للمدرس/الأدمن) ────────────────────────

    @GetMapping("/transactions")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN') or @perm.can(authentication,'TOP_UP_WALLET')")
    public ResponseEntity<GlobalResponse<Page<WalletTransactionDto>>> getAllTransactions(
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        return ResponseEntity.ok(GlobalResponse.success(
                walletService.getAllDepositTransactions(pageable)
                             .map(paymentMapper::toWalletTransactionDto)));
    }

    // ─── شحن اونلاين (الطالب عبر فواتيرك) ───────────────────────

    /** GET /api/wallet/payment-methods — طرق الدفع المتاحة من فواتيرك */
    @GetMapping("/payment-methods")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<GlobalResponse<Object>> getPaymentMethods() {
        Object methods = fawaterekClient.getPaymentMethods();
        return ResponseEntity.ok(GlobalResponse.success(methods));
    }

    /** GET /api/wallet/deposit/status/{txNumber} — حالة معاملة الشحن */
    @GetMapping("/deposit/status/{txNumber}")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<GlobalResponse<Map<String, String>>> getDepositStatus(
            @PathVariable String txNumber,
            @AuthenticationPrincipal JwtUserPrincipal principal) {
        String status = walletService.getDepositStatus(txNumber, principal.getUserId());
        return ResponseEntity.ok(GlobalResponse.success(Map.of("status", status)));
    }

    @PostMapping("/deposit/online")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<GlobalResponse<Map<String, String>>> depositOnline(
            @Valid @RequestBody WalletOnlineDepositRequest request,
            @AuthenticationPrincipal JwtUserPrincipal principal) {

        Long studentId = principal.getUserId();

        // 1) إنشاء معاملة PENDING وجيب رقمها
        Map<String, String> initiated = walletService.initiateOnlineDeposit(studentId, request.getAmount());
        String txNumber = initiated.get("transactionNumber");

        // 2) جيب بيانات الطالب لفواتيرك
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new com.educore.exception.ResourceNotFoundException("الطالب غير موجود"));

        // 3) أنشئ invoice على فواتيرك
        FawaterekClient.FawaterekInvoiceResult result =
                fawaterekClient.createWalletDepositInvoice(request.getAmount(), student, txNumber,
                        request.getPaymentMethodId());

        if (!result.success()) {
            walletService.failOnlineDeposit(txNumber, result.errorMessage());
            return ResponseEntity.badRequest()
                    .body(GlobalResponse.error(result.errorMessage()));
        }

        return ResponseEntity.ok(GlobalResponse.success(
                "تم إنشاء رابط الدفع بنجاح",
                Map.of("redirectUrl", result.paymentUrl(),
                       "transactionNumber", txNumber)));
    }

    // ─── إحصائيات المحفظة ────────────────────────────────────────

    @GetMapping("/stats")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN') or @perm.can(authentication,'TOP_UP_WALLET')")
    public ResponseEntity<GlobalResponse<WalletStatsResponse>> getStats() {
        return ResponseEntity.ok(GlobalResponse.success(walletService.getStats()));
    }
}
