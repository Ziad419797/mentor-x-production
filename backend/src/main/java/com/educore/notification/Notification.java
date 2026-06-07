package com.educore.notification;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * الإشعارات الداخلية للتطبيق.
 *
 * كل إشعار يتوجه لمستخدم واحد (بـ recipientId + recipientRole).
 * ولي الأمر بيوصله إشعار عن نشاط ابنه (quizScore, attendance …).
 * الطالب بيوصله إشعار لما حصة جديدة تتفتح أو لما المدرس يبعت إعلان.
 */
@Entity
@Table(
    name = "notifications",
    indexes = {
        @Index(name = "idx_notif_recipient",       columnList = "recipient_id, recipient_role"),
        @Index(name = "idx_notif_unread",          columnList = "recipient_id, is_read"),
        @Index(name = "idx_notif_created",         columnList = "created_at"),
        @Index(name = "idx_notif_type",            columnList = "type")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ─── المستلم ─────────────────────────────────────────
    @Column(name = "recipient_id", nullable = false)
    private Long recipientId;

    /** PARENT / STUDENT / ADMIN / TEACHER */
    @Column(name = "recipient_role", nullable = false, length = 20)
    private String recipientRole;

    // ─── المحتوى ─────────────────────────────────────────
    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, length = 1000)
    private String body;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private NotificationType type;

    // ─── ربط بالحدث المصدر ───────────────────────────────
    /** ID السجل المرتبط (مثلاً: attendanceRecord.id أو quizAttempt.id) */
    @Column(name = "related_entity_id")
    private Long relatedEntityId;

    /** نوع الكيان المرتبط (مثلاً: "ATTENDANCE", "QUIZ_ATTEMPT") */
    @Column(name = "related_entity_type", length = 50)
    private String relatedEntityType;

    /** ID الطالب المعني — مفيد لولي الأمر عشان يعرف ابنه إيه */
    @Column(name = "student_id")
    private Long studentId;

    // ─── الحالة ──────────────────────────────────────────
    @Builder.Default
    @Column(name = "is_read", nullable = false)
    private boolean isRead = false;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    // ─── التوقيت ─────────────────────────────────────────
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    // ─── Helper ──────────────────────────────────────────
    public void markRead() {
        this.isRead = true;
        this.readAt = LocalDateTime.now();
    }
}
