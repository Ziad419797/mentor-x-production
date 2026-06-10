package com.educore.activitylog;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "activity_logs", indexes = {
    @Index(name = "idx_al_actor",   columnList = "actor_username"),
    @Index(name = "idx_al_created", columnList = "created_at"),
    @Index(name = "idx_al_action",  columnList = "action")
})
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class ActivityLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** اسم المنفذ الكامل */
    @Column(name = "actor_name", length = 120)
    private String actorName;

    /** username (phone/email) للمنفذ */
    @Column(name = "actor_username", length = 120)
    private String actorUsername;

    /** ID المنفذ */
    @Column(name = "actor_id")
    private Long actorId;

    /** دور المنفذ: TEACHER, STAFF, ADMIN */
    @Column(name = "actor_role", length = 30)
    private String actorRole;

    /** الإجراء المنفَّذ — مثلاً: "شحن محفظة", "قبول طالب" */
    @Column(nullable = false, length = 200)
    private String action;

    /** نوع الكيان — WALLET, STUDENT, COURSE … */
    @Column(name = "entity_type", length = 50)
    private String entityType;

    /** معرف الكيان */
    @Column(name = "entity_id", length = 100)
    private String entityId;

    /** تفاصيل إضافية */
    @Column(length = 500)
    private String details;

    @Column(name = "ip_address", length = 50)
    private String ipAddress;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
