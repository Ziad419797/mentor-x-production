package com.educore.payment.payment;

import com.educore.dtopayment.*;
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
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@Slf4j
@RestController
@RequestMapping("/api/payment")
@RequiredArgsConstructor
@Tag(name = "Payment System", description = "نظام الدفع — محفظة، بطاقة، فوري، فودافون كاش")
public class PaymentController {

    private final PaymentService paymentService;

    /* ════════════════ ORDER ════════════════ */

    @Operation(summary = "إنشاء طلب شراء")
    @PostMapping("/orders")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<OrderDto> createOrder(
            @Valid @RequestBody CreateOrderRequest request,
            @AuthenticationPrincipal JwtUserPrincipal principal,
            @RequestHeader("X-Device-Id") String deviceId

    ) {
        return ResponseEntity.ok(
                paymentService.createOrder(principal.getUserId(), request));
    }

    @Operation(summary = "طلب بالرقم")
    @GetMapping("/orders/{orderNumber}")
    public ResponseEntity<OrderDto> getOrder(@PathVariable String orderNumber) {
        return ResponseEntity.ok(paymentService.getOrderByNumber(orderNumber));
    }

    @Operation(summary = "طلباتي")
    @GetMapping("/orders/my-orders")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<Page<OrderDto>> getMyOrders(
            @AuthenticationPrincipal JwtUserPrincipal principal,
            @RequestHeader("X-Device-Id") String deviceId,

            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable
    ) {
        return ResponseEntity.ok(
                paymentService.getStudentOrders(principal.getUserId(), pageable));
    }

    /* ════════════════ PAYMENT ════════════════ */

    /**
     * POST /api/payment/process
     * الطرق المقبولة: WALLET, CREDIT_CARD, FAWRY, VODAFONE_CASH
     * ❌ CASH و BANK_TRANSFER غير مدعومين — استخدم أكواد الوصول
     */
    @Operation(summary = "ادفع (بطاقة / فوري / فودافون كاش / محفظة)")
    @PostMapping("/process")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<PaymentResultDto> processPayment(
            @Valid @RequestBody PaymentRequestDto request,
            @AuthenticationPrincipal JwtUserPrincipal principal,
            @RequestHeader("X-Device-Id") String deviceId

    ) {
        return ResponseEntity.ok(
                paymentService.processPayment(principal.getUserId(), request));
    }

    /* ════════════════ WALLET ════════════════ */

    @Operation(summary = "شحن المحفظة")
    @PostMapping("/wallet/deposit")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<WalletTransactionDto> deposit(
            @Valid @RequestBody DepositRequestDto request,
            @AuthenticationPrincipal JwtUserPrincipal principal,
            @RequestHeader("X-Device-Id") String deviceId

    ) {
        return ResponseEntity.ok(
                paymentService.depositToWallet(principal.getUserId(), request));
    }

    @Operation(summary = "رصيد المحفظة")
    @GetMapping("/wallet/balance")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<Map<String, Object>> getBalance(
            @AuthenticationPrincipal JwtUserPrincipal principal,
            @RequestHeader("X-Device-Id") String deviceId

    ) {
        BigDecimal balance = paymentService.getWalletBalance(principal.getUserId());
        return ResponseEntity.ok(Map.of(
                "balance",          balance,
                "formattedBalance", String.format("%.2f ج.م", balance),
                "currency",         "EGP"
        ));
    }

    @Operation(summary = "تفاصيل المحفظة")
    @GetMapping("/wallet")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<WalletDto> getWallet(
            @AuthenticationPrincipal JwtUserPrincipal principal,
            @RequestHeader("X-Device-Id") String deviceId

    ) {
        return ResponseEntity.ok(paymentService.getWallet(principal.getUserId()));
    }

    @Operation(summary = "معاملات المحفظة")
    @GetMapping("/wallet/transactions")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<Page<WalletTransactionDto>> getTransactions(
            @AuthenticationPrincipal JwtUserPrincipal principal,
            @RequestHeader("X-Device-Id") String deviceId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable
    ) {
        return ResponseEntity.ok(
                paymentService.getWalletTransactions(principal.getUserId(), pageable));
    }

    /* ════════════════ PAYMENT METHODS INFO ════════════════ */

    @Operation(summary = "طرق الدفع المتاحة")
    @GetMapping("/methods")
    public ResponseEntity<List<PaymentMethodInfoDto>> getMethods() {
        return ResponseEntity.ok(List.of(
                method("WALLET",        "المحفظة الرقمية",         "1",  "10000", true),
                method("CREDIT_CARD",   "بطاقة فيزا / ماستركارد", "10", "50000", true),
                method("FAWRY",         "فوري",                    "1",  "20000", true),
                method("VODAFONE_CASH", "فودافون كاش",             "1",  "15000", true)
        ));
    }

    private PaymentMethodInfoDto method(
            String code, String name, String min, String max, boolean enabled) {
        return PaymentMethodInfoDto.builder()
                .code(code).nameAr(name)
                .minAmount(new BigDecimal(min))
                .maxAmount(new BigDecimal(max))
                .enabled(enabled)
                .instantActivation(true)
                .build();
    }
}