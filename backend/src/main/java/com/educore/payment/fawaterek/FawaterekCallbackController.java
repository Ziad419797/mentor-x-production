package com.educore.payment.fawaterek;

import com.educore.enrollment.EnrollmentService;
import com.educore.enrollment.EnrollmentType;
import com.educore.payment.order.Order;
import com.educore.payment.order.OrderRepository;
import com.educore.payment.payment.Payment;
import com.educore.payment.payment.PaymentRepository;
import com.educore.wallet.WalletService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.math.BigDecimal;
import java.util.HexFormat;
import java.util.Map;
import java.util.Optional;

/**
 * FawaterekCallbackController — يستقبل الـ webhook من فواتيرك.
 *
 * فواتيرك بتبعت POST request لـ callbackUrl بعد كل عملية دفع.
 * الـ endpoint ده public (بدون JWT) عشان فواتيرك بتبعت من سيرفرها.
 *
 * الـ Payload المتوقع من فواتيرك:
 * {
 *   "invoice_id":     "INV-xxxxx",
 *   "payment_status": "paid" | "failed" | "pending",
 *   "amount":         150.00,
 *   "order":          "ORD-20240101-XXXXXXXX"
 * }
 *
 * ✅ الأمان:
 * لو fawaterek.webhook-secret مضبوط في الـ config، بنتحقق من الـ HMAC-SHA256
 * signature اللي فواتيرك بتبعتها في الـ header عشان نتأكد إن الـ request حقيقي.
 * لو مش مضبوط (dev mode) بنقبل بدون تحقق مع تحذير في الـ log.
 */
@Slf4j
@RestController
@RequestMapping("/api/payment/fawaterek")
@RequiredArgsConstructor
public class FawaterekCallbackController {

    private final PaymentRepository    paymentRepo;
    private final OrderRepository      orderRepo;
    private final EnrollmentService    enrollmentService;
    private final FawaterekProperties  fawaterekProperties;
    private final WalletService        walletService;
    private final ObjectMapper         objectMapper;   // Spring-managed — لا تستخدم new ObjectMapper()

    // الـ header اللي فواتيرك بتبعت فيه الـ signature
    private static final String SIGNATURE_HEADER = "X-Fawaterek-Signature";

    /**
     * POST /api/payment/fawaterek/callback
     *
     * فواتيرك بتبعت الـ callback هنا بعد ما الطالب يدفع.
     * بنبحث عن الـ Payment بالـ invoiceId (اللي خزناه كـ transactionId).
     */
    @PostMapping("/callback")
    @Transactional
    public ResponseEntity<String> handleCallback(
            @RequestBody String rawBody,
            @RequestHeader(value = SIGNATURE_HEADER, required = false) String signature,
            @RequestParam(value = "order", required = false) String orderNumber,
            @RequestParam(value = "ref", required = false) String walletRef,
            @RequestParam(value = "type", required = false) String callbackType) {

        // ── التحقق من الـ Signature ──
        if (!verifySignature(rawBody, signature)) {
            log.warn("Fawaterek callback rejected — invalid or missing signature. order={}", orderNumber);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("invalid signature");
        }

        // ── تحويل الـ raw body لـ Map ──
        Map<String, Object> payload = parsePayload(rawBody);
        if (payload == null) {
            log.warn("Fawaterek callback — failed to parse payload");
            return ResponseEntity.ok("bad payload — ignored");
        }

        log.info("Fawaterek callback received — order={}, payload={}", orderNumber, payload);

        // فواتيرك بتبعت invoice_key مش invoice_id في الـ webhook
        String invoiceKey    = extractString(payload, "invoice_key", "invoiceKey", "invoice_id", "invoiceId");
        String invoiceIdStr  = extractString(payload, "invoice_id", "invoiceId");
        // فواتيرك بتبعت invoice_status مش payment_status
        String paymentStatus = extractString(payload, "invoice_status", "payment_status", "status").toLowerCase();
        String paymentMethod = extractString(payload, "payment_method", "paymentMethod");

        // ── التحقق من الـ hashKey بطريقة فواتيرك ──
        String receivedHash = extractString(payload, "hashKey", "hash_key");
        if (!receivedHash.isBlank() && !verifyFawaterakHash(invoiceIdStr, invoiceKey, paymentMethod, receivedHash)) {
            log.warn("Fawaterek callback hashKey mismatch — possible forgery. order={}", orderNumber);
            // لو الـ hash غلط نرد 200 بدون معالجة
            return ResponseEntity.ok("hash mismatch — ignored");
        }

        if (invoiceKey.isBlank() && invoiceIdStr.isBlank()) {
            log.warn("Fawaterek callback missing invoice_key — payload={}", payload);
            return ResponseEntity.ok("missing invoice_key — ignored");
        }

        // ── معالجة شحن المحفظة اونلاين (type=wallet في الـ query param أو payLoad) ──
        String txRef = walletRef;
        if ((txRef == null || txRef.isBlank()) && payload.get("pay_load") instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> pl = (Map<String, Object>) payload.get("pay_load");
            if ("wallet".equals(pl.get("type"))) {
                txRef = String.valueOf(pl.getOrDefault("txNumber", ""));
            }
        }

        if ("wallet".equalsIgnoreCase(callbackType) || (txRef != null && !txRef.isBlank())) {
            final String finalRef = txRef;
            switch (paymentStatus) {
                case "paid" -> {
                    walletService.completeOnlineDeposit(finalRef, invoiceKey);
                    log.info("✅ Wallet deposit completed via Fawaterak: ref={}, invoice={}", finalRef, invoiceKey);
                }
                case "failed", "rejected" -> {
                    walletService.failOnlineDeposit(finalRef, "رُفض الدفع من فواتيرك");
                    log.warn("❌ Wallet deposit failed: ref={}", finalRef);
                }
                default -> log.info("Wallet deposit callback status='{}' — no action", paymentStatus);
            }
            return ResponseEntity.ok("ok");
        }

        // ── نجيب الـ Payment بالـ invoiceId ──
        Optional<Payment> paymentOpt = paymentRepo.findByTransactionId(invoiceIdStr);

        // Fallback: لو ما لقيناش بالـ invoiceId، نجرب بـ orderNumber من الـ query param
        if (paymentOpt.isEmpty() && orderNumber != null && !orderNumber.isBlank()) {
            paymentOpt = paymentRepo.findByOrderOrderNumber(orderNumber);
        }

        if (paymentOpt.isEmpty()) {
            log.warn("No payment found for Fawaterek invoice_id={}, order={}", invoiceIdStr, orderNumber);
            return ResponseEntity.ok("not found — ignored");
        }

        Payment payment = paymentOpt.get();
        Order   order   = payment.getOrder();

        // ── تجنب المعالجة المزدوجة ──
        if (payment.isCompleted()) {
            log.info("Payment already completed — invoice={}, order={}", invoiceIdStr, order.getOrderNumber());
            return ResponseEntity.ok("already processed");
        }

        // ── معالجة حسب الحالة ──
        switch (paymentStatus) {
            case "paid" -> {
                // التحقق من أن المبلغ المستلم يساوي المبلغ المتوقع في الـ Order
                String amountStr = extractString(payload, "amount");
                if (!amountStr.isBlank()) {
                    try {
                        BigDecimal paidAmount   = new BigDecimal(amountStr);
                        BigDecimal orderTotal   = order.getTotal();
                        if (orderTotal != null && paidAmount.compareTo(orderTotal) < 0) {
                            log.error("Amount mismatch! order={} expected={} got={}",
                                order.getOrderNumber(), orderTotal, paidAmount);
                            payment.fail("مبلغ الدفع أقل من المطلوب");
                            order.markAsFailed("Amount mismatch");
                            paymentRepo.save(payment);
                            orderRepo.save(order);
                            return ResponseEntity.ok("amount mismatch — ignored");
                        }
                    } catch (NumberFormatException e) {
                        log.warn("Could not parse amount '{}' from Fawaterek callback", amountStr);
                    }
                }
                handlePaid(payment, order, invoiceIdStr);
            }
            case "failed", "rejected" -> handleFailed(payment, order);
            default -> log.info("Fawaterek callback status='{}' — no action", paymentStatus);
        }

        return ResponseEntity.ok("ok");
    }

    /* ══════════════════════════════════════════════════════
       Private Handlers
    ══════════════════════════════════════════════════════ */

    private void handlePaid(Payment payment, Order order, String invoiceId) {
        payment.complete(invoiceId, "Paid via Fawaterek gateway");
        order.markAsPaid(payment.getPaymentMethod());
        paymentRepo.save(payment);
        orderRepo.save(order);

        int enrollments = createEnrollments(order);

        log.info("✅ Fawaterek payment completed — invoice={}, order={}, enrollments={}",
                invoiceId, order.getOrderNumber(), enrollments);
    }

    private void handleFailed(Payment payment, Order order) {
        payment.fail("رُفض الدفع من بوابة فواتيرك");
        order.markAsFailed("Fawaterek payment failed or rejected");
        paymentRepo.save(payment);
        orderRepo.save(order);
        log.warn("❌ Fawaterek payment failed — order={}", order.getOrderNumber());
    }

    /* ══════════════════════════════════════════════════════
       Enrollment Creation
    ══════════════════════════════════════════════════════ */

    private int createEnrollments(Order order) {
        int count     = 0;
        Long studentId = order.getStudent().getId();

        for (var item : order.getItems()) {
            if ("CATEGORY".equals(item.getProductType())) {
                for (var course : item.getCategory().getCourses()) {
                    boolean created = enrollmentService.enrollAfterPayment(
                            studentId, course.getId(), item.getCategory().getId(),
                            EnrollmentType.CATEGORY_PURCHASE, "FAWATEREK"
                    ).isPresent();
                    if (created) count++;
                }
            } else {
                boolean created = enrollmentService.enrollAfterPayment(
                        studentId, item.getCourse().getId(), null,
                        EnrollmentType.COURSE_PURCHASE, "FAWATEREK"
                ).isPresent();
                if (created) count++;
            }
        }

        return count;
    }

    /* ══════════════════════════════════════════════════════
       Helpers
    ══════════════════════════════════════════════════════ */

    /** يجيب الـ value من الـ map — يحاول أكثر من key اسم (بعض APIs بتختلف). */
    private String extractString(Map<String, Object> map, String... keys) {
        for (String key : keys) {
            Object val = map.get(key);
            if (val != null && !String.valueOf(val).isBlank()) {
                return String.valueOf(val);
            }
        }
        return "";
    }

    /**
     * يتحقق من الـ HMAC-SHA256 signature اللي بتبعتها فواتيرك.
     *
     * لو webhook-secret مش مضبوط → بنقبل الـ request (dev mode) مع تحذير.
     * لو مضبوط → بنحسب الـ HMAC ونقارنه بالـ signature.
     */
    private boolean verifySignature(String rawBody, String signature) {
        String secret = fawaterekProperties.getWebhookSecret();

        // dev mode: لو مفيش secret مضبوط نقبل بدون تحقق
        if (secret == null || secret.isBlank()) {
            log.warn("Fawaterek webhook secret not configured — skipping signature verification (dev mode)");
            return true;
        }

        if (signature == null || signature.isBlank()) {
            log.warn("Fawaterek callback missing signature header '{}'", SIGNATURE_HEADER);
            return false;
        }

        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] hash = mac.doFinal(rawBody.getBytes(StandardCharsets.UTF_8));
            String expected = HexFormat.of().formatHex(hash);

            // مقارنة constant-time لتجنب timing attacks
            return constantTimeEquals(expected, signature.toLowerCase());
        } catch (Exception e) {
            log.error("Fawaterek signature verification failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * يتحقق من الـ hashKey اللي بتبعته فواتيرك في الـ webhook payload.
     *
     * الصيغة: HMAC-SHA256("InvoiceId=X&InvoiceKey=Y&PaymentMethod=Z", providerKey)
     */
    private boolean verifyFawaterakHash(String invoiceId, String invoiceKey, String paymentMethod, String receivedHash) {
        String providerKey = fawaterekProperties.getProviderKey();
        if (providerKey == null || providerKey.isBlank()) {
            log.warn("Fawaterak providerKey not configured — skipping hash verification (dev mode)");
            return true;
        }
        try {
            String message = "InvoiceId=" + invoiceId + "&InvoiceKey=" + invoiceKey + "&PaymentMethod=" + paymentMethod;
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(providerKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] hash = mac.doFinal(message.getBytes(StandardCharsets.UTF_8));
            String expected = HexFormat.of().formatHex(hash);
            return constantTimeEquals(expected, receivedHash.toLowerCase());
        } catch (Exception e) {
            log.error("Fawaterak hash verification failed: {}", e.getMessage());
            return false;
        }
    }

    /** مقارنة string بطريقة constant-time لمنع timing attacks */
    private boolean constantTimeEquals(String a, String b) {
        if (a.length() != b.length()) return false;
        int result = 0;
        for (int i = 0; i < a.length(); i++) {
            result |= a.charAt(i) ^ b.charAt(i);
        }
        return result == 0;
    }

    /** يحوّل الـ JSON string لـ Map */
    @SuppressWarnings("unchecked")
    private Map<String, Object> parsePayload(String rawBody) {
        try {
            return objectMapper.readValue(rawBody, Map.class);
        } catch (Exception e) {
            log.error("Failed to parse Fawaterek payload: {}", e.getMessage());
            return null;
        }
    }
}
