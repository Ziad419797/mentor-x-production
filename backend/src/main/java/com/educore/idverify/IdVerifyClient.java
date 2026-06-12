package com.educore.idverify;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.time.Duration;
import java.util.Map;

/**
 * HTTP client بيتصل بـ ID-Verify Python/FastAPI service.
 *
 * Base URL: ${idverify.service.url} (افتراضي: http://localhost:8001)
 * Endpoint: POST /api/v1/extract-id  (multipart/form-data, field = "file")
 */
@Slf4j
@Component
public class IdVerifyClient {

    private final RestClient restClient;

    public IdVerifyClient(
            @Value("${idverify.service.url:http://localhost:8001}") String baseUrl,
            @Value("${idverify.service.timeout-seconds:180}") int timeoutSeconds
    ) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(10));
        factory.setReadTimeout(Duration.ofSeconds(timeoutSeconds));

        this.restClient = RestClient.builder()
                .requestFactory(factory)
                .baseUrl(baseUrl)
                .build();

        log.info("IdVerifyClient initialized → baseUrl={} timeout={}s", baseUrl, timeoutSeconds);
    }

    /**
     * بيبعت صورة البطاقة للـ ID-Verify service ويرجع نتيجة الاستخراج والتحقق.
     *
     * @param imageUrl  رابط صورة البطاقة (profileImageUrl أو identityDocumentUrl)
     * @param studentId للـ logging بس
     * @return الـ response JSON كـ Map
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> verifyIdCard(String imageUrl, Long studentId) {
        log.info("IdVerify: downloading image for studentId={} url={}", studentId, imageUrl);

        // ── تحميل الصورة من الـ URL ──
        byte[] imageBytes;
        String filename;
        try {
            URL url = new URI(imageUrl).toURL();
            try (InputStream in = url.openStream()) {
                imageBytes = in.readAllBytes();
            }
            // استخراج اسم الملف من الـ URL
            String path = url.getPath();
            filename = path.contains("/") ? path.substring(path.lastIndexOf('/') + 1) : "id_card.jpg";
            if (!filename.matches(".*\\.(jpg|jpeg|png|webp)$")) filename += ".jpg";
        } catch (Exception e) {
            log.error("IdVerify: failed to download image for studentId={}: {}", studentId, e.getMessage());
            throw new RuntimeException("فشل تحميل صورة البطاقة: " + e.getMessage(), e);
        }

        log.info("IdVerify: sending {} bytes ({}) to service for studentId={}", imageBytes.length, filename, studentId);

        // ── إرسال الصورة كـ multipart ──
        try {
            final String finalFilename = filename;
            final byte[] finalBytes = imageBytes;

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", new ByteArrayResource(finalBytes) {
                @Override public String getFilename() { return finalFilename; }
            });

            Map<String, Object> response = restClient.post()
                    .uri("/api/v1/extract-id")
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(body)
                    .retrieve()
                    .body(Map.class);

            log.info("IdVerify: extraction complete for studentId={}", studentId);
            return response;

        } catch (Exception e) {
            log.error("IdVerify: service call failed for studentId={}: {}", studentId, e.getMessage());
            throw new RuntimeException("فشل الاتصال بخدمة التحقق من الهوية: " + e.getMessage(), e);
        }
    }

    /** Health check للـ ID-Verify service */
    public boolean isHealthy() {
        try {
            Map<?, ?> response = restClient.get()
                    .uri("/api/v1/health")
                    .retrieve()
                    .body(Map.class);
            return response != null && "UP".equals(response.get("status"));
        } catch (Exception e) {
            log.warn("IdVerify health check failed: {}", e.getMessage());
            return false;
        }
    }
}
