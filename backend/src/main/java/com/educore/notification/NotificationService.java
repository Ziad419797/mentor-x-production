package com.educore.notification;

import com.educore.exception.ResourceNotFoundException;
import com.educore.student.StudentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository   notificationRepository;
    private final FirebaseMessagingService fcmService;
    private final StudentRepository        studentRepository;

    // ─────────────────────────────────────────────────────────────
    // إنشاء إشعار (يُستدعى @Async من باقي الـ Services)
    // ─────────────────────────────────────────────────────────────

    @Async
    @Transactional
    public void send(Long recipientId, String recipientRole,
                     String title, String body,
                     NotificationType type,
                     Long relatedEntityId, String relatedEntityType,
                     Long studentId) {
        try {
            // 1. حفظ في الـ DB
            com.educore.notification.Notification n = com.educore.notification.Notification.builder()
                    .recipientId(recipientId)
                    .recipientRole(recipientRole)
                    .title(title)
                    .body(body)
                    .type(type)
                    .relatedEntityId(relatedEntityId)
                    .relatedEntityType(relatedEntityType)
                    .studentId(studentId)
                    .build();
            notificationRepository.save(n);
            log.debug("Notification saved to DB → {} [{}]: {}", recipientId, recipientRole, title);

            // 2. إرسال Push Notification عبر FCM للطلاب فقط
            if ("STUDENT".equals(recipientRole)) {
                studentRepository.findById(recipientId).ifPresent(student -> {
                    if (student.getFcmToken() != null && !student.getFcmToken().isBlank()) {
                        fcmService.sendToDevice(
                                student.getFcmToken(),
                                title,
                                body,
                                relatedEntityId != null
                                        ? Map.of("type", type.name(),
                                                 "entityId", String.valueOf(relatedEntityId),
                                                 "entityType", relatedEntityType != null ? relatedEntityType : "")
                                        : Map.of("type", type.name())
                        );
                    }
                });
            }

        } catch (Exception e) {
            log.error("Failed to send notification to {} [{}]: {}", recipientId, recipientRole, e.getMessage());
        }
    }

    /** Overload بدون relatedEntity/studentId للإشعارات العامة */
    @Async
    @Transactional
    public void send(Long recipientId, String recipientRole,
                     String title, String body, NotificationType type) {
        send(recipientId, recipientRole, title, body, type, null, null, null);
    }

    // ─────────────────────────────────────────────────────────────
    // Factory Methods — إشعارات جاهزة لكل حدث
    // ─────────────────────────────────────────────────────────────

    /** لما الطالب يحضر في السنتر */
    @Async
    public void notifyAttendanceCenter(Long parentId, String studentName,
                                        String lessonTitle, Long attendanceId, Long studentId) {
        send(parentId, "PARENT",
             "✅ تم التسجيل في السنتر",
             studentName + " حضر حصة \"" + lessonTitle + "\" في السنتر الآن",
             NotificationType.ATTENDANCE_CENTER,
             attendanceId, "ATTENDANCE", studentId);
    }

    /** لما الطالب يفتح حصة أونلاين */
    @Async
    public void notifyAttendanceOnline(Long parentId, String studentName,
                                        String lessonTitle, Long attendanceId, Long studentId) {
        send(parentId, "PARENT",
             "📱 دخل الحصة أونلاين",
             studentName + " بدأ مشاهدة حصة \"" + lessonTitle + "\"",
             NotificationType.ATTENDANCE_ONLINE,
             attendanceId, "ATTENDANCE", studentId);
    }

    /** لما الطالب يسلم كويز */
    @Async
    public void notifyQuizResult(Long parentId, String studentName,
                                  String quizTitle, int score, boolean passed,
                                  Long attemptId, Long studentId) {
        String emoji  = passed ? "🎉" : "📚";
        String status = passed ? "نجح" : "يحتاج مراجعة";

        send(parentId, "PARENT",
             emoji + " نتيجة كويز: " + quizTitle,
             studentName + " " + status + " في كويز \"" + quizTitle + "\" بدرجة " + score,
             passed ? NotificationType.QUIZ_PASSED : NotificationType.QUIZ_FAILED,
             attemptId, "QUIZ_ATTEMPT", studentId);

        // إشعار للطالب نفسه
        send(studentId, "STUDENT",
             emoji + " نتيجة كويزك: " + quizTitle,
             (passed ? "أحسنت! حصلت على " : "حصلت على ") + score + " — " + (passed ? "الحصة التالية اتفتحت!" : "راجع الحصة وحاول تاني"),
             passed ? NotificationType.QUIZ_PASSED : NotificationType.QUIZ_FAILED,
             attemptId, "QUIZ_ATTEMPT", studentId);
    }

    /** لما الطالب يسلم واجب */
    @Async
    public void notifyAssignmentSubmitted(Long parentId, String studentName,
                                           String assignmentTitle, double score,
                                           Long attemptId, Long studentId) {
        send(parentId, "PARENT",
             "📝 سلّم الواجب",
             studentName + " سلّم واجب \"" + assignmentTitle + "\" بدرجة " + score,
             NotificationType.ASSIGNMENT_SUBMITTED,
             attemptId, "ASSIGNMENT_ATTEMPT", studentId);
    }

    /** لما الطالب يتسجل في كورس */
    @Async
    public void notifyEnrollment(Long parentId, String studentName,
                                  String courseName, Long enrollmentId, Long studentId) {
        send(parentId, "PARENT",
             "🎓 تسجيل جديد",
             studentName + " اشترك في كورس \"" + courseName + "\"",
             NotificationType.ENROLLMENT_CREATED,
             enrollmentId, "ENROLLMENT", studentId);

        send(studentId, "STUDENT",
             "🎓 تم التسجيل!",
             "تم تسجيلك في \"" + courseName + "\" — الحصة الأولى متاحة الآن",
             NotificationType.ENROLLMENT_CREATED,
             enrollmentId, "ENROLLMENT", studentId);
    }

    /**
     * لما الأدمن يوافق على الطالب.
     * بتبعت للطالب: كود الحساب + اسم أول سيشن عنده + اسم المركز لو حضور.
     *
     * @param studentId    معرف الطالب
     * @param studentCode  كود الطالب الخاص (مثل "A3F7")
     * @param centerName   اسم المركز لو حضور، null لو أونلاين
     * @param firstSession عنوان أول سيشن متاح (null لو مفيش سيشنات بعد)
     */
    @Async
    public void notifyApproval(Long studentId, String studentCode,
                               String centerName, String firstSession) {
        StringBuilder body = new StringBuilder();
        body.append("✅ تمت الموافقة على حسابك!\n");
        body.append("كودك الخاص: ").append(studentCode).append("\n");

        if (firstSession != null && !firstSession.isBlank()) {
            body.append("سيشنك الأول: ").append(firstSession).append("\n");
        }

        if (centerName != null && !centerName.isBlank()) {
            body.append("مركزك: ").append(centerName);
        } else {
            body.append("الدراسة: أونلاين");
        }

        send(studentId, "STUDENT",
             "🎉 تم قبول حسابك في MentorX",
             body.toString(),
             NotificationType.ACCOUNT_STATUS,
             studentId, "STUDENT", studentId);
    }

    /** لما حصة جديدة تتفتح للطالب */
    @Async
    public void notifyLessonUnlocked(Long studentId, String lessonTitle, Long weekId) {
        send(studentId, "STUDENT",
             "🔓 حصة جديدة متاحة!",
             "تم فتح حصة \"" + lessonTitle + "\" — يلا ادرس!",
             NotificationType.LESSON_UNLOCKED,
             weekId, "WEEK", studentId);
    }

    // ─────────────────────────────────────────────────────────────
    // قراءة الإشعارات
    // ─────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<NotificationResponse> getMyNotifications(Long recipientId, String role,
                                                          boolean unreadOnly, Pageable pageable) {
        Page<Notification> page = unreadOnly
                ? notificationRepository
                        .findByRecipientIdAndRecipientRoleAndIsReadFalseOrderByCreatedAtDesc(
                                recipientId, role, pageable)
                : notificationRepository
                        .findByRecipientIdAndRecipientRoleOrderByCreatedAtDesc(
                                recipientId, role, pageable);

        return page.map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(Long recipientId, String role) {
        return notificationRepository
                .countByRecipientIdAndRecipientRoleAndIsReadFalse(recipientId, role);
    }

    // ─────────────────────────────────────────────────────────────
    // تحديد كمقروء
    // ─────────────────────────────────────────────────────────────

    @Transactional
    public void markRead(Long notificationId, Long recipientId) {
        Notification n = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("الإشعار غير موجود"));

        if (!n.getRecipientId().equals(recipientId)) {
            throw new SecurityException("ليس لديك صلاحية لتعديل هذا الإشعار");
        }

        n.markRead();
        notificationRepository.save(n);
    }

    @Transactional
    public int markAllRead(Long recipientId, String role) {
        return notificationRepository.markAllAsRead(recipientId, role);
    }

    // ─────────────────────────────────────────────────────────────
    // Mapping
    // ─────────────────────────────────────────────────────────────

    private NotificationResponse toResponse(Notification n) {
        return NotificationResponse.builder()
                .id(n.getId())
                .title(n.getTitle())
                .body(n.getBody())
                .type(n.getType())
                .isRead(n.isRead())
                .readAt(n.getReadAt())
                .relatedEntityId(n.getRelatedEntityId())
                .relatedEntityType(n.getRelatedEntityType())
                .studentId(n.getStudentId())
                .createdAt(n.getCreatedAt())
                .build();
    }
}
