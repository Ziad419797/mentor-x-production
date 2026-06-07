package com.educore.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

/**
 * SmsMisrSmsService — إرسال OTP عبر SMS Misr OTP API.
 *
 * يُفعَّل لما تكون: sms.provider=smsmisr في application.properties
 *
 * ═══════════════════════════════════════════════════════════
 * خطوات الإعداد:
 *   1. سجّل دخول على smsmisr.com
 *   2. من Settings → انسخ username وpassword
 *   3. من Sender IDs → انسخ الـ token الخاص بالـ sender المعتمد
 *   4. اختار template من القائمة واحفظ الـ token الخاص بيه
 *   5. حط الإعدادات في application.properties
 *
 * للتجربة قبل الإنتاج:
 *   - environment=2  (Test)
 *   - sender=b611afb996655a94c8e942a823f1421de42bf8335d24ba1f84c437b2ab11ca27
 *   - رصيد اختبار متاح: 5000 رسالة
 *
 * للإنتاج الحقيقي:
 *   - environment=1  (Live)
 *   - sender=token_من_sender_ids_في_لوحة_التحكم
 * ═══════════════════════════════════════════════════════════
 *
 * SMS Misr OTP API:
 *   GET https://smsmisr.com/api/OTP/?environment=X&username=X&password=X
 *                                    &sender=X&mobile=X&template=X&otp=X
 *
 * Response Codes:
 *   4901 = Success
 *   4903 = Invalid username/password
 *   4904 = Invalid sender
 *   4905 = Invalid mobile
 *   4906 = Insufficient Credit
 *   4907 = Server updating
 *   4908 = Invalid OTP
 *   4909 = Invalid Template Token
 *   4912 = Invalid Environment
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "sms.provider", havingValue = "smsmisr")
public class SmsMisrSmsService implements SmsService {

    @Value("${smsmisr.username}")
    private String username;

    @Value("${smsmisr.password}")
    private String password;

    /** Sender token من لوحة التحكم → Sender IDs */
    @Value("${smsmisr.sender}")
    private String sender;

    /** Template token من لوحة التحكم → Templates */
    @Value("${smsmisr.template}")
    private String template;

    /**
     * 1 = Live (إنتاج حقيقي)
     * 2 = Test (اختبار، رصيد مجاني)
     */
    @Value("${smsmisr.environment:2}")
    private String environment;

    @Value("${smsmisr.base-url:https://smsmisr.com/api/OTP/}")
    private String baseUrl;

    private final RestTemplate restTemplate;

    public SmsMisrSmsService(RestTemplateBuilder builder) {
        this.restTemplate = builder.build();
    }

    /* ══════════════════════════════════════════════════════
       sendOtp — الدالة الرئيسية
    ══════════════════════════════════════════════════════ */

    @Override
    public void sendOtp(String phone, String otpCode) {
        String normalizedPhone = normalizePhone(phone);
        log.info("Sending OTP via SMS Misr — phone={}, env={}", normalizedPhone, environment);

        // بناء الـ GET URL مع كل الـ parameters
        String url = UriComponentsBuilder.fromHttpUrl(baseUrl)
                .queryParam("environment", environment)
                .queryParam("username",    username)
                .queryParam("password",    password)
                .queryParam("sender",      sender)
                .queryParam("mobile",      normalizedPhone)
                .queryParam("template",    template)
                .queryParam("otp",         otpCode)
                .toUriString();

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            handleResponse(response, normalizedPhone);
        } catch (SmsException ex) {
            throw ex; // re-throw our own exceptions
        } catch (Exception ex) {
            log.error("SMS Misr API call failed for phone={}: {}", normalizedPhone, ex.getMessage(), ex);
            throw new SmsException("فشل الاتصال بخدمة الرسائل. يرجى المحاولة مرة أخرى.", ex);
        }
    }

    /* ══════════════════════════════════════════════════════
       Response Handler
    ══════════════════════════════════════════════════════ */

    private void handleResponse(Map<String, Object> body, String phone) {
        if (body == null) {
            throw new SmsException("لم يصل رد من SMS Misr");
        }

        String code   = String.valueOf(body.getOrDefault("code", ""));
        String smsId  = String.valueOf(body.getOrDefault("SMSID", ""));
        String cost   = String.valueOf(body.getOrDefault("Cost", ""));

        if ("4901".equals(code)) {
            log.info("✅ OTP sent successfully — phone={}, SMSID={}, Cost={}", phone, smsId, cost);
        } else {
            String errorMsg = translateError(code);
            log.error("❌ SMS Misr error — code={}, phone={}, msg={}", code, phone, errorMsg);
            throw new SmsException(errorMsg);
        }
    }

    /* ══════════════════════════════════════════════════════
       Helpers
    ══════════════════════════════════════════════════════ */

    /**
     * يحوّل الرقم المصري للصيغة الدولية المطلوبة من SMS Misr.
     *
     * SMS Misr بتتوقع: 201XXXXXXXXX (بدون + وبدون الصفر الأول)
     *
     * مثال:
     *   01012345678  →  201012345678
     *   1012345678   →  201012345678
     */
    private String normalizePhone(String phone) {
        if (phone == null) return "";

        // ازل كل حاجة غير أرقام
        phone = phone.replaceAll("[^0-9]", "");

        // لو بيبدأ بـ 0 (مثال: 01012345678) → ازل الصفر وضيف 20
        if (phone.startsWith("0")) {
            phone = "20" + phone.substring(1);
        }
        // لو مش بيبدأ بـ 20 (مثال: 1012345678) → ضيف 20
        else if (!phone.startsWith("20")) {
            phone = "20" + phone;
        }

        return phone;
    }

    /** يترجم رموز الخطأ من SMS Misr لرسائل واضحة */
    private String translateError(String code) {
        return switch (code) {
            case "4903" -> "بيانات حساب SMS Misr غير صحيحة. تحقق من username وpassword.";
            case "4904" -> "الـ Sender ID غير صحيح أو غير مفعّل. تحقق من لوحة التحكم.";
            case "4905" -> "رقم الهاتف غير صحيح أو غير مدعوم.";
            case "4906" -> "الرصيد غير كافٍ في حساب SMS Misr. اشحن الرصيد.";
            case "4907" -> "خادم SMS Misr يتحدّث حالياً. حاول بعد قليل.";
            case "4908" -> "الـ OTP غير صالح (طوله أكثر من 10 أرقام).";
            case "4909" -> "الـ Template Token غير صحيح. تحقق من smsmisr.template.";
            case "4912" -> "قيمة environment غير صحيحة (1=Live, 2=Test).";
            default     -> "خطأ في إرسال رمز التحقق (كود: " + code + "). يرجى المحاولة مرة أخرى.";
        };
    }
}
