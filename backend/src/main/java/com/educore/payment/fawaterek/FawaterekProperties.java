package com.educore.payment.fawaterek;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * FawaterekProperties — إعدادات بوابة فواتيرك.
 *
 * ضيف في application.properties:
 *
 *   fawaterek.api-key=${FAWATEREK_API_KEY:your-api-key-here}
 *   fawaterek.base-url=https://app.fawaterk.com/api/v2
 *   fawaterek.success-url=https://your-frontend.com/payment/success
 *   fawaterek.fail-url=https://your-frontend.com/payment/fail
 *   fawaterek.pending-url=https://your-frontend.com/payment/pending
 *   fawaterek.callback-url=https://your-api.com/api/payment/fawaterek/callback
 */
@Data
@Component
@ConfigurationProperties(prefix = "fawaterek")
public class FawaterekProperties {

    /** مفتاح الـ API — اجبه من لوحة تحكم فواتيرك */
    private String apiKey = "";

    /** Base URL لـ API فواتيرك (production) */
    private String baseUrl = "https://app.fawaterk.com/api/v2";

    /** URL الفرونتند لما يتم الدفع بنجاح */
    private String successUrl = "http://localhost:3000/payment/success";

    /** URL الفرونتند لما يفشل الدفع */
    private String failUrl = "http://localhost:3000/payment/fail";

    /** URL الفرونتند لما الدفع في الانتظار */
    private String pendingUrl = "http://localhost:3000/payment/pending";

    /**
     * Callback URL الخاص بالسيرفر.
     * فواتيرك بتبعت POST request هنا بعد كل عملية دفع.
     * في الـ production: https://your-api-domain.com/api/payment/fawaterek/callback
     */
    private String callbackUrl = "http://localhost:8081/api/payment/fawaterek/callback";

    /**
     * providerKey (Vendor Key) — يُستخدم للتحقق من الـ webhook hashKey.
     * اجبه من لوحة تحكم فواتيرك → Integration.
     */
    private String providerKey = "";

    /** legacy webhook secret — غير مستخدم بعد الآن */
    private String webhookSecret = "";
}
