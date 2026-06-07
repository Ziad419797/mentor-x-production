package com.educore.payment.fawaterek;

import com.educore.payment.order.Order;
import com.educore.payment.order.OrderItem;
import com.educore.student.Student;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

/**
 * FawaterekClient — يتكلم مع Fawaterek API لإنشاء invoices.
 *
 * الـ Flow الكامل:
 *   1. Backend يعمل createInvoice(order) → يرجع hosted payment URL
 *   2. Frontend يـ redirect الطالب لـ URL ده
 *   3. الطالب يختار طريقة الدفع ويدفع على صفحة فواتيرك
 *      (فيزا، فودافون كاش، فوري، إنستاباي، إلخ)
 *   4. فواتيرك بتبعت POST callback للسيرفر ← FawaterekCallbackController
 *   5. فواتيرك بتعمل redirect للطالب لـ successUrl / failUrl
 */
@Slf4j
@Service
public class FawaterekClient {

    private final FawaterekProperties props;
    private final RestTemplate         restTemplate;

    public FawaterekClient(FawaterekProperties props, RestTemplateBuilder builder) {
        this.props        = props;
        this.restTemplate = builder.build();
    }

    /* ══════════════════════════════════════════════════════
       createInvoice — الدالة الرئيسية
    ══════════════════════════════════════════════════════ */

    /**
     * ينشئ invoice على فواتيرك ويرجع الـ hosted payment URL.
     *
     * @param order الطلب المراد دفعه
     * @return FawaterekInvoiceResult بيحتوي على الـ URL والـ invoiceId
     */
    public FawaterekInvoiceResult createInvoice(Order order) {
        Student student  = order.getStudent();
        String  orderNum = order.getOrderNumber();

        try {
            Map<String, Object> body = buildRequestBody(order, student, orderNum);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(props.getApiKey());

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
            String url = props.getBaseUrl() + "/invoiceInitPay";

            log.info("Calling Fawaterek invoiceInitPay for order={}", orderNum);
            ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);

            return parseResponse(response.getBody(), orderNum);

        } catch (Exception ex) {
            log.error("Fawaterek API call failed for order={}: {}", orderNum, ex.getMessage(), ex);
            return FawaterekInvoiceResult.failed("فشل الاتصال بفواتيرك: " + ex.getMessage());
        }
    }

    /* ══════════════════════════════════════════════════════
       Request Builder
    ══════════════════════════════════════════════════════ */

    private Map<String, Object> buildRequestBody(Order order, Student student, String orderNum) {
        Map<String, Object> body = new LinkedHashMap<>();

        body.put("payment_method_id", 2);   // Visa/Mastercard — always redirect
        body.put("redirectOption",    true); // always return a redirectTo URL
        body.put("cartTotal",         order.getTotal());
        body.put("currency",          "EGP");

        // ── Customer Info ──
        body.put("customer", buildCustomer(student));

        // ── Cart Items ──
        body.put("cartItems", buildCartItems(order));

        // ── Redirection + Webhook URLs ──
        Map<String, String> rUrls = new LinkedHashMap<>();
        rUrls.put("successUrl",  props.getSuccessUrl() + "&order=" + orderNum);
        rUrls.put("failUrl",     props.getFailUrl()    + "&order=" + orderNum);
        rUrls.put("pendingUrl",  props.getPendingUrl() + "&order=" + orderNum);
        rUrls.put("webhookUrl",  props.getCallbackUrl() + "?order=" + orderNum);
        body.put("redirectionUrls", rUrls);

        // ── payLoad لتمرير الـ reference ──
        body.put("payLoad", Map.of("orderNumber", orderNum));
        body.put("sendEmail", false);

        return body;
    }

    private Map<String, String> buildCustomer(Student student) {
        // Student entity stores name in separate fields (firstName, secondName, ...)
        String firstName = (student.getFirstName() != null && !student.getFirstName().isBlank())
                ? student.getFirstName() : "Student";
        String lastName  = (student.getSecondName() != null && !student.getSecondName().isBlank())
                ? student.getSecondName() : "-";
        // Student has no email field — derive a placeholder from phone
        String phone     = (student.getPhone() != null) ? student.getPhone() : "01000000000";
        String email     = phone + "@students.educore.app";

        Map<String, String> customer = new LinkedHashMap<>();
        customer.put("first_name", firstName);
        customer.put("last_name",  lastName);
        customer.put("email",      email);
        customer.put("phone",      phone);
        return customer;
    }

    private List<Map<String, Object>> buildCartItems(Order order) {
        List<Map<String, Object>> items = new ArrayList<>();
        for (OrderItem item : order.getItems()) {
            Map<String, Object> cartItem = new LinkedHashMap<>();
            cartItem.put("name",     item.getProductName());
            cartItem.put("price",    item.getUnitPrice().toPlainString());
            cartItem.put("quantity", item.getQuantity() != null ? item.getQuantity() : 1);
            items.add(cartItem);
        }
        return items;
    }

    /* ══════════════════════════════════════════════════════
       Response Parser
    ══════════════════════════════════════════════════════ */

    @SuppressWarnings("unchecked")
    private FawaterekInvoiceResult parseResponse(Map<?, ?> responseBody, String orderNum) {
        if (responseBody == null) {
            return FawaterekInvoiceResult.failed("لا يوجد رد من فواتيرك");
        }

        String status = String.valueOf(responseBody.get("status"));

        if (!"success".equalsIgnoreCase(status)) {
            // error message can be a string or a validation map
            Object msgObj = responseBody.get("message");
            if (msgObj == null) msgObj = responseBody.get("msg");
            String msg = (msgObj != null) ? String.valueOf(msgObj) : "خطأ غير معروف من فواتيرك";
            log.error("Fawaterek returned error for order={}: {}", orderNum, msg);
            return FawaterekInvoiceResult.failed(msg);
        }

        Map<String, Object> data = (Map<String, Object>) responseBody.get("data");
        if (data == null) {
            return FawaterekInvoiceResult.failed("رد فواتيرك لا يحتوي على بيانات");
        }

        // invoice_id and invoice_key are top-level in data
        String invoiceKey = String.valueOf(data.getOrDefault("invoice_key",
                data.getOrDefault("invoiceKey",
                data.getOrDefault("invoice_id", "UNKNOWN"))));

        // redirect URL is nested under payment_data.redirectTo
        String paymentUrl = "";
        Object paymentDataObj = data.get("payment_data");
        if (paymentDataObj instanceof Map) {
            Map<String, Object> paymentData = (Map<String, Object>) paymentDataObj;
            paymentUrl = String.valueOf(paymentData.getOrDefault("redirectTo",
                    paymentData.getOrDefault("url", "")));
        }

        // pass full payment_data so caller can use Fawry code / Meeza QR etc.
        Object paymentDataForCaller = data.get("payment_data");

        log.info("Fawaterek invoice created: key={}, url={} for order={}", invoiceKey, paymentUrl, orderNum);
        return FawaterekInvoiceResult.success(invoiceKey, paymentUrl, paymentDataForCaller);
    }

    /* ══════════════════════════════════════════════════════
       createWalletDepositInvoice — شحن محفظة اونلاين
    ══════════════════════════════════════════════════════ */

    /** جيب طرق الدفع المتاحة من فواتيرك */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getPaymentMethods() {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(props.getApiKey());
            HttpEntity<Void> entity = new HttpEntity<>(headers);
            ResponseEntity<Map> response = restTemplate.exchange(
                    props.getBaseUrl() + "/getPaymentmethods", HttpMethod.GET, entity, Map.class);
            Map<?, ?> body = response.getBody();
            if (body != null && "success".equalsIgnoreCase(String.valueOf(body.get("status")))) {
                return (List<Map<String, Object>>) body.get("data");
            }
        } catch (Exception ex) {
            log.error("Failed to fetch Fawaterek payment methods: {}", ex.getMessage());
        }
        return List.of();
    }

    public FawaterekInvoiceResult createWalletDepositInvoice(
            java.math.BigDecimal amount,
            com.educore.student.Student student,
            String txNumber,
            int paymentMethodId) {
        try {
            String firstName = (student.getFirstName() != null && !student.getFirstName().isBlank())
                    ? student.getFirstName() : "Student";
            String lastName  = (student.getSecondName() != null && !student.getSecondName().isBlank())
                    ? student.getSecondName() : "-";
            String phone     = student.getPhone() != null ? student.getPhone() : "01000000000";
            String email     = phone + "@students.educore.app";

            java.util.Map<String, Object> body = new java.util.LinkedHashMap<>();
            body.put("payment_method_id", paymentMethodId);
            body.put("redirectOption",    true); // always return redirect URL
            body.put("cartTotal", amount);
            body.put("currency",  "EGP");
            body.put("customer",  java.util.Map.of(
                    "first_name", firstName, "last_name", lastName,
                    "email", email, "phone", phone));
            body.put("cartItems", java.util.List.of(java.util.Map.of(
                    "name", "شحن محفظة EduCore",
                    "price", amount.toPlainString(),
                    "quantity", "1")));
            java.util.Map<String, String> rUrls = new java.util.LinkedHashMap<>();
            rUrls.put("successUrl", props.getSuccessUrl() + "&ref=" + txNumber + "&type=wallet");
            rUrls.put("failUrl",    props.getFailUrl()    + "&ref=" + txNumber + "&type=wallet");
            rUrls.put("pendingUrl", props.getPendingUrl() + "&ref=" + txNumber + "&type=wallet");
            rUrls.put("webhookUrl", props.getCallbackUrl() + "?ref=" + txNumber + "&type=wallet");
            body.put("redirectionUrls", rUrls);
            body.put("payLoad", java.util.Map.of("txNumber", txNumber, "type", "wallet"));
            body.put("sendEmail", false);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(props.getApiKey());
            HttpEntity<java.util.Map<String, Object>> entity = new HttpEntity<>(body, headers);

            log.info("Fawaterek wallet deposit: txNumber={}, amount={}", txNumber, amount);
            ResponseEntity<Map> response =
                    restTemplate.postForEntity(props.getBaseUrl() + "/invoiceInitPay", entity, Map.class);

            return parseResponse(response.getBody(), txNumber);
        } catch (Exception ex) {
            log.error("Fawaterek wallet deposit failed txNumber={}: {}", txNumber, ex.getMessage());
            return FawaterekInvoiceResult.failed("فشل الاتصال بفواتيرك: " + ex.getMessage());
        }
    }

    /* ══════════════════════════════════════════════════════
       Result Record
    ══════════════════════════════════════════════════════ */

    /**
     * نتيجة إنشاء الـ invoice على فواتيرك.
     */
    public record FawaterekInvoiceResult(
            boolean success,
            String  invoiceId,
            String  paymentUrl,
            String  errorMessage,
            Object  invoiceData   // payment_data من فواتيرك (Fawry code, QR, إلخ)
    ) {
        public static FawaterekInvoiceResult success(String invoiceId, String url, Object invoiceData) {
            return new FawaterekInvoiceResult(true, invoiceId, url, null, invoiceData);
        }

        public static FawaterekInvoiceResult failed(String error) {
            return new FawaterekInvoiceResult(false, null, null, error, null);
        }
    }
}
