package com.educore.attendance.group;

import com.educore.student.Student;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * سجل حضور / غياب طالب في حصة معينة.
 */
@Entity
@Table(
    name = "attendance_group_records",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_record_session_student",
        columnNames = {"session_id", "student_id"}
    ),
    indexes = {
        @Index(name = "idx_att_rec_session", columnList = "session_id"),
        @Index(name = "idx_att_rec_student", columnList = "student_id"),
        @Index(name = "idx_att_rec_status",  columnList = "status")
    }
)
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class AttendanceGroupRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_id", nullable = false)
    private AttendanceGroupSession session;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    // ─── الحالة والطريقة ─────────────────────────────────────
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AttendanceStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "scan_method", nullable = false, length = 20)
    private ScanMethod scanMethod;

    // ─── Alert لو في مشكلة ───────────────────────────────────
    @Enumerated(EnumType.STRING)
    @Column(name = "alert_type", length = 30)
    private GroupAlertType alertType;   // null = لا يوجد تحذير

    /** رسالة توضيحية للـ alert مثل: "الطالب مسجّل في سنتر الدقي" */
    @Column(name = "alert_message", length = 300)
    private String alertMessage;

    // ─── تعليق المدرس ────────────────────────────────────────
    @Column(name = "teacher_comment", length = 1000)
    private String teacherComment;

    // ─── توقيت ───────────────────────────────────────────────
    @Column(name = "scanned_at")
    private LocalDateTime scannedAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
