package com.educore.attendance;

import com.educore.lesson.Week;
import com.educore.student.Student;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "attendance_records",
    indexes = {
        @Index(name = "idx_att_student",       columnList = "student_id"),
        @Index(name = "idx_att_week",          columnList = "week_id"),
        @Index(name = "idx_att_attended_at",   columnList = "attended_at"),
        @Index(name = "idx_att_student_week",  columnList = "student_id, week_id")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AttendanceRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ─── من هو الطالب ─────────────────────────────────────
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    // ─── أي حصة (Week = الدرس الفعلي في الكود) ──────────
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "week_id", nullable = false)
    private Week week;

    // ─── متى ─────────────────────────────────────────────
    @Column(name = "attended_at", nullable = false)
    private LocalDateTime attendedAt;

    // ─── نوع الحضور ───────────────────────────────────────
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private AttendanceType type;   // CENTER | ONLINE

    // ─── مصدر التسجيل ────────────────────────────────────
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AttendanceSource source; // QR_SCAN | ONLINE_ACCESS | MANUAL

    // ─── بيانات إضافية ───────────────────────────────────

    /** ID أو اسم الموظف اللي عمل الـ scan (لو CENTER) */
    @Column(name = "scanned_by", length = 100)
    private String scannedBy;

    /** IP address الطالب (للأونلاين) */
    @Column(name = "ip_address", length = 50)
    private String ipAddress;

    /** ملاحظات يدوية */
    @Column(length = 500)
    private String notes;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
