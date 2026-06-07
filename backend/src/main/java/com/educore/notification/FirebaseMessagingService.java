package com.educore.notification;

import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.Notification;  // ✅

import com.google.firebase.messaging.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * خدمة إرسال Push Notifications عبر Firebase Cloud Messaging (FCM).
 *
 * كل الـ methods بـ @Async عشان ما تحبسش الـ thread الأصلي.
 * لو Firebase مش initialized، بتعمل log warning وترجع بدون exception.
 */
@Slf4j
@Service
public class FirebaseMessagingService {

    // ─────────────────────────────────────────────────────────────
    // Single Device
    // ─────────────────────────────────────────────────────────────

    /**
     * بعت إشعار لجهاز واحد عبر FCM token.
     *
     * @param fcmToken  FCM token بتاع الجهاز
     * @param title     عنوان الإشعار
     * @param body      محتوى الإشعار
     * @param data      بيانات إضافية (اختياري) — بتوصل للـ app كـ key-value
     */
    @Async
    public void sendToDevice(String fcmToken, String title, String body, Map<String, String> data) {
        if (!isFirebaseAvailable()) return;
        if (fcmToken == null || fcmToken.isBlank()) {
            log.debug("FCM token is null/empty — skipping push notification");
            return;
        }

        try {
            Message.Builder messageBuilder = Message.builder()
                    .setToken(fcmToken)
                    .setNotification(Notification.builder()
                            .setTitle(title)
                            .setBody(body)
                            .build())
                    .setAndroidConfig(AndroidConfig.builder()
                            .setPriority(AndroidConfig.Priority.HIGH)
                            .setNotification(AndroidNotification.builder()
                                    .setSound("default")
                                    .build())
                            .build())
                    .setApnsConfig(ApnsConfig.builder()
                            .setAps(Aps.builder()
                                    .setSound("default")
                                    .setBadge(1)
                                    .build())
                            .build());

            // إضافة الـ data payload لو موجود
            if (data != null && !data.isEmpty()) {
                messageBuilder.putAllData(data);
            }

            String response = FirebaseMessaging.getInstance().send(messageBuilder.build());
            log.info("FCM push sent successfully. MessageId={}", response);

        } catch (FirebaseMessagingException e) {
            handleFcmException(e, fcmToken);
        } catch (Exception e) {
            log.error("Unexpected error sending FCM notification: {}", e.getMessage(), e);
        }
    }

    /**
     * Overload بدون data payload.
     */
    @Async
    public void sendToDevice(String fcmToken, String title, String body) {
        sendToDevice(fcmToken, title, body, null);
    }

    // ─────────────────────────────────────────────────────────────
    // Multiple Devices (Multicast)
    // ─────────────────────────────────────────────────────────────

    /**
     * بعت إشعار لأكتر من جهاز في وقت واحد (مثلاً: أول ولي الأمر والطالب).
     *
     * @param fcmTokens  قائمة FCM tokens
     * @param title      عنوان الإشعار
     * @param body       محتوى الإشعار
     */
    @Async
    public void sendToMultipleDevices(List<String> fcmTokens, String title, String body) {
        sendToMultipleDevices(fcmTokens, title, body, null);
    }

    /**
     * بعت إشعار لأكتر من جهاز مع data payload.
     */
    @Async
    public void sendToMultipleDevices(List<String> fcmTokens, String title, String body, Map<String, String> data) {
        if (!isFirebaseAvailable()) return;
        if (fcmTokens == null || fcmTokens.isEmpty()) return;

        // FCM بيقبل 500 token كحد أقصى في طلب واحد
        List<String> validTokens = fcmTokens.stream()
                .filter(t -> t != null && !t.isBlank())
                .limit(500)  // حد أقصى 500 token في الطلب الواحد
                .toList();

        if (validTokens.isEmpty()) return;

        try {
            MulticastMessage.Builder messageBuilder = MulticastMessage.builder()
                    .addAllTokens(validTokens)
                    .setNotification(Notification.builder()
                            .setTitle(title)
                            .setBody(body)
                            .build())
                    .setAndroidConfig(AndroidConfig.builder()
                            .setPriority(AndroidConfig.Priority.HIGH)
                            .build());

            // إضافة الـ data payload لو موجود
            if (data != null && !data.isEmpty()) {
                messageBuilder.putAllData(data);
            }

            BatchResponse response = FirebaseMessaging.getInstance().sendEachForMulticast(messageBuilder.build());
            log.info("FCM multicast: sent={}, success={}, failed={}",
                    validTokens.size(), response.getSuccessCount(), response.getFailureCount());

            // Log failed tokens for debugging
            if (response.getFailureCount() > 0) {
                List<SendResponse> responses = response.getResponses();
                for (int i = 0; i < responses.size(); i++) {
                    if (!responses.get(i).isSuccessful()) {
                        log.warn("FCM failed for token[{}]: {}", i,
                                responses.get(i).getException().getMessage());
                    }
                }
            }

        } catch (FirebaseMessagingException e) {
            log.error("FCM multicast error: {}", e.getMessage(), e);
        }
    }

    // ─────────────────────────────────────────────────────────────
    // Topic (للإشعارات العامة)
    // ─────────────────────────────────────────────────────────────

    /**
     * بعت إشعار لكل الـ devices المشتركة في topic معين.
     * مفيد لإشعارات عامة زي: "حصة جديدة متاحة لكل طلاب كورس X".
     *
     * @param topic  اسم الـ topic (مثال: "course_15", "announcements")
     * @param title  عنوان الإشعار
     * @param body   محتوى الإشعار
     */
    @Async
    public void sendToTopic(String topic, String title, String body) {
        sendToTopic(topic, title, body, null);
    }

    /**
     * بعت إشعار لـ topic مع data payload.
     */
    @Async
    public void sendToTopic(String topic, String title, String body, Map<String, String> data) {
        if (!isFirebaseAvailable()) return;
        if (topic == null || topic.isBlank()) return;

        try {
            Message.Builder messageBuilder = Message.builder()
                    .setTopic(topic)
                    .setNotification(Notification.builder()
                            .setTitle(title)
                            .setBody(body)
                            .build());

            // إضافة الـ data payload لو موجود
            if (data != null && !data.isEmpty()) {
                messageBuilder.putAllData(data);
            }

            String response = FirebaseMessaging.getInstance().send(messageBuilder.build());
            log.info("FCM topic message sent. Topic={}, MessageId={}", topic, response);

        } catch (FirebaseMessagingException e) {
            log.error("FCM topic error for topic={}: {}", topic, e.getMessage(), e);
        }
    }

    // ─────────────────────────────────────────────────────────────
    // Condition (إشعارات شرطية متقدمة)
    // ─────────────────────────────────────────────────────────────

    /**
     * بعت إشعار بناءً على شرط (مثال: "'course_15' in topics && 'ios' in topics").
     *
     * @param condition  الشرط المنطقي (FCM condition syntax)
     * @param title      عنوان الإشعار
     * @param body       محتوى الإشعار
     */
    @Async
    public void sendToCondition(String condition, String title, String body) {
        if (!isFirebaseAvailable()) return;
        if (condition == null || condition.isBlank()) return;

        try {
            Message message = Message.builder()
                    .setCondition(condition)
                    .setNotification(Notification.builder()
                            .setTitle(title)
                            .setBody(body)
                            .build())
                    .build();

            String response = FirebaseMessaging.getInstance().send(message);
            log.info("FCM condition message sent. Condition={}, MessageId={}", condition, response);

        } catch (FirebaseMessagingException e) {
            log.error("FCM condition error: {}", e.getMessage(), e);
        }
    }

    // ─────────────────────────────────────────────────────────────
    // Topic Management (اشتراك/إلغاء اشتراك)
    // ─────────────────────────────────────────────────────────────

    /**
     * اشتراك جهاز واحد في topic معين.
     */
    @Async
    public void subscribeToTopic(String fcmToken, String topic) {
        if (!isFirebaseAvailable()) return;
        if (fcmToken == null || topic == null) return;

        try {
            TopicManagementResponse response = FirebaseMessaging.getInstance()
                    .subscribeToTopic(List.of(fcmToken), topic);
            log.info("Subscribed to topic '{}'. Success count: {}",
                    topic, response.getSuccessCount());
        } catch (FirebaseMessagingException e) {
            log.error("Failed to subscribe to topic '{}': {}", topic, e.getMessage());
        }
    }

    /**
     * إلغاء اشتراك جهاز واحد من topic معين.
     */
    @Async
    public void unsubscribeFromTopic(String fcmToken, String topic) {
        if (!isFirebaseAvailable()) return;
        if (fcmToken == null || topic == null) return;

        try {
            TopicManagementResponse response = FirebaseMessaging.getInstance()
                    .unsubscribeFromTopic(List.of(fcmToken), topic);
            log.info("Unsubscribed from topic '{}'. Success count: {}",
                    topic, response.getSuccessCount());
        } catch (FirebaseMessagingException e) {
            log.error("Failed to unsubscribe from topic '{}': {}", topic, e.getMessage());
        }
    }

    /**
     * اشتراك عدة أجهزة في topic.
     */
    @Async
    public void subscribeMultipleToTopic(List<String> fcmTokens, String topic) {
        if (!isFirebaseAvailable()) return;
        if (fcmTokens == null || fcmTokens.isEmpty() || topic == null) return;

        List<String> validTokens = fcmTokens.stream()
                .filter(t -> t != null && !t.isBlank())
                .toList();

        if (validTokens.isEmpty()) return;

        try {
            TopicManagementResponse response = FirebaseMessaging.getInstance()
                    .subscribeToTopic(validTokens, topic);
            log.info("Subscribed {} devices to topic '{}'. Success: {}, Failure: {}",
                    validTokens.size(), topic, response.getSuccessCount(), response.getFailureCount());
        } catch (FirebaseMessagingException e) {
            log.error("Failed to subscribe multiple devices to topic '{}': {}", topic, e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────

    /** يتحقق إن Firebase initialized — بيرجع false لو مش initialized */
    private boolean isFirebaseAvailable() {
        boolean available = !FirebaseApp.getApps().isEmpty();
        if (!available) {
            log.debug("Firebase not initialized — push notification skipped");
        }
        return available;
    }

    /** معالجة أخطاء FCM الشائعة */
    private void handleFcmException(FirebaseMessagingException e, String token) {
        MessagingErrorCode errorCode = e.getMessagingErrorCode();

        if (errorCode == MessagingErrorCode.UNREGISTERED
                || errorCode == MessagingErrorCode.INVALID_ARGUMENT) {
            // الـ token قديم أو غير صالح — يجب حذفه من الـ DB
            String shortToken = token != null && token.length() > 15
                    ? token.substring(0, 10) + "..." + token.substring(token.length() - 5)
                    : "unknown";
            log.warn("FCM token is invalid/unregistered, should be removed: {}", shortToken);
        } else if (errorCode == MessagingErrorCode.SENDER_ID_MISMATCH) {
            log.error("FCM Sender ID mismatch — check google-services.json or sender ID");
        } else if (errorCode == MessagingErrorCode.QUOTA_EXCEEDED) {
            log.error("FCM quota exceeded — too many messages sent");
        } else {
            log.error("FCM error [{}]: {}", errorCode, e.getMessage());
        }
    }

    /**
     * توليد topic للكورس (مثلاً: "course_123")
     */
    public static String getCourseTopic(Long courseId) {
        return "course_" + courseId;
    }

    /**
     * توليد topic للمستخدم (مثلاً: "student_456")
     */
    public static String getUserTopic(Long userId, String role) {
        return role.toLowerCase() + "_" + userId;
    }
}