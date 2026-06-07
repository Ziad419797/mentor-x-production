package com.educore.payment.payment;

import com.educore.dtopayment.DepositRequestDto;
import com.educore.dtopayment.PaymentRequestDto;
import com.educore.payment.fawaterek.FawaterekClient;
import com.educore.payment.fawaterek.FawaterekClient.FawaterekInvoiceResult;
import com.educore.payment.order.Order;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * PaymentGatewayService — Strategy Pattern
 * ──────────────────────────────────────────
 * كل طريقة دفع ليها handler منفصل.
 *
 * ✅ CREDIT_CARD / FAWRY / VODAFONE_CASH → Fawaterek hosted page (redirect)
 * ✅ WALLET                               → خصم مباشر من المحفظة الداخلية
 * ✅ CASH / BANK_TRANSFER                 → يدوي (بانتظار موافقة الأدمن)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentGatewayService {

    private final FawaterekClient fawaterekClient;

    /* ══════════════════════════════════════════
       Entry Points
    ══════════════════════════════════════════ */

    /**
     * @param req   بيانات الطلب (payment method, إلخ)
     * @param order الـ Order المراد دفعه (محتاجه لـ Fawaterek)
     */
    public GatewayResult processPayment(PaymentRequestDto req, Order order) {
        log.info("Processing {} payment of {} EGP — order={}",
                req.getPaymentMethod(), order.getTotal(), order.getOrderNumber());

        return switch (req.getPaymentMethod()) {
            case WALLET        -> handleWallet();
            case CREDIT_CARD,
                 FAWRY,
                 VODAFONE_CASH -> handleFawaterek(order, req.getPaymentMethod());
            case CASH          -> handleCash();
            case BANK_TRANSFER -> handleBankTransfer();
        };
    }

    public GatewayResult processDeposit(DepositRequestDto req, Long studentId) {
        log.info("Processing deposit of {} via {} for student={}",
                req.getAmount(), req.getPaymentMethod(), studentId);

        // الإيداع عبر الـ wallet يتعمل مباشرة — مفيش redirect
        return switch (req.getPaymentMethod()) {
            case CREDIT_CARD,
                 FAWRY,
                 VODAFONE_CASH -> GatewayResult.failed(
                    "لإيداع المحفظة عبر " + req.getPaymentMethod().name() +
                    "، استخدم نظام فواتيرك من الفرونتند مباشرة.");
            default -> GatewayResult.failed("طريقة الدفع غير مدعومة للإيداع: " + req.getPaymentMethod());
        };
    }

    /* ══════════════════════════════════════════
       Handlers
    ══════════════════════════════════════════ */

    /** محفظة — لا تحتاج gateway خارجي، تُعالج في PaymentService */
    private GatewayResult handleWallet() {
        return GatewayResult.success(
                "WALLET-" + System.currentTimeMillis(),
                "تم الخصم من المحفظة"
        );
    }

    /**
     * Fawaterek — بوابة الدفع الموحدة.
     * تدعم: Visa/Mastercard، فوري، فودافون كاش، إنستاباي، وأكثر.
     * النتيجة: redirect URL لصفحة الدفع المستضافة على فواتيرك.
     */
    private GatewayResult handleFawaterek(Order order, PaymentMethod method) {
        log.info("Creating Fawaterek invoice for order={}, method={}", order.getOrderNumber(), method);

        FawaterekInvoiceResult result = fawaterekClient.createInvoice(order);

        if (!result.success()) {
            log.error("Fawaterek invoice creation failed: {}", result.errorMessage());
            return GatewayResult.failed(result.errorMessage());
        }

        log.info("Fawaterek invoice created: id={} → redirecting", result.invoiceId());
        return GatewayResult.redirect(result.invoiceId(), result.paymentUrl());
    }

    /** كاش — يتم يدوياً في السنتر، بانتظار موافقة الأدمن */
    private GatewayResult handleCash() {
        String ref = "CASH-" + System.currentTimeMillis();
        log.info("Cash payment registered: {}", ref);
        return GatewayResult.pendingApproval(ref,
                "تم تسجيل طلب الدفع كاش. سيتم التفعيل بعد موافقة الإدارة.");
    }

    /** تحويل بنكي — بانتظار موافقة الأدمن بعد التحقق */
    private GatewayResult handleBankTransfer() {
        String ref = "BANK-" + System.currentTimeMillis();
        log.info("Bank transfer registered: {}", ref);
        return GatewayResult.pendingApproval(ref,
                "تم تسجيل طلب التحويل البنكي. سيتم التفعيل بعد التحقق من الإيداع.");
    }

    /* ══════════════════════════════════════════
       Result Record
    ══════════════════════════════════════════ */

    /**
     * نتيجة الـ Gateway.
     *
     * الحالات الممكنة:
     *  success=true              → تم الدفع فوراً (Wallet)
     *  requiresRedirect=true     → الطالب لازم يتحول لصفحة Fawaterek
     *  pendingApproval=true      → ينتظر موافقة يدوية (Cash / Bank Transfer)
     *  success=false (الباقي)    → فشل
     */
    public record GatewayResult(
            boolean success,
            boolean pendingApproval,
            boolean requiresRedirect,
            String  transactionId,
            String  message,
            String  redirectUrl
    ) {
        /** دفع ناجح فوراً (Wallet) */
        public static GatewayResult success(String txnId, String msg) {
            return new GatewayResult(true, false, false, txnId, msg, null);
        }

        /** يحتاج موافقة يدوية (Cash / Bank) */
        public static GatewayResult pendingApproval(String ref, String msg) {
            return new GatewayResult(false, true, false, ref, msg, null);
        }

        /** يحتاج redirect لصفحة دفع خارجية (Fawaterek) */
        public static GatewayResult redirect(String invoiceId, String redirectUrl) {
            return new GatewayResult(false, false, true, invoiceId,
                    "جاري التحويل لبوابة الدفع الآمنة...", redirectUrl);
        }

        /** فشل */
        public static GatewayResult failed(String msg) {
            return new GatewayResult(false, false, false, null, msg, null);
        }
    }
}
