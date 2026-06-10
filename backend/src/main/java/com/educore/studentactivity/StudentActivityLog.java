package com.educore.studentactivity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * تسجيل نشاط الطالب على المنصة:
 * دخول، حل امتحان، تسليم واجب، اشتراك في كورس، شحن محفظة، ...
 */
@Entity
@Table(name = "student_activity_logs", indexes = {
    @Index(name = "idx_sal_student",   columnList = "student_id"),
    @Index(name = "idx_sal_event",     columnList = "event_type"),
    @Index(name = "idx_sal_created",   columnList = "created_at")
})
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class StudentActivityLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    @Column(name = "student_name", length = 120)
    private String studentName;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 50)
    private StudentEventType eventType;

    /** عنوان موجز — يُعرض في القائمة */
    @Column(nullable = false, length = 200)
    private String title;

    /** تفاصيل اختيارية — اسم الكويز / الكورس / المبلغ ... */
    @Column(length = 500)
    private String details;

    @Column(name = "ip_address", length = 50)
    private String ipAddress;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
