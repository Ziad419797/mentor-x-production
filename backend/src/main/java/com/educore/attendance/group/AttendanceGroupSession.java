package com.educore.attendance.group;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * حصة داخل جروب الحضور.
 * المدرس يفتحها → يسجل الحضور → يغلقها (فيتم تسجيل غياب الباقين تلقائياً).
 */
@Entity
@Table(
    name = "attendance_group_sessions",
    indexes = {
        @Index(name = "idx_att_ses_group", columnList = "group_id"),
        @Index(name = "idx_att_ses_date",  columnList = "session_date"),
        @Index(name = "idx_att_ses_open",  columnList = "open")
    }
)
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class AttendanceGroupSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "group_id", nullable = false)
    private AttendanceGroup group;

    /** تاريخ الحصة */
    @Column(name = "session_date", nullable = false)
    private LocalDate sessionDate;

    /** رقم الحصة (1، 2، 3 ...) */
    @Column(name = "session_number")
    private Integer sessionNumber;

    /** عنوان الحصة مثل "الحصة الثالثة — باب القواعد" */
    @Column(nullable = false, length = 200)
    private String title;

    /** هل باب الحضور مفتوح الآن؟ */
    @Builder.Default
    @Column(nullable = false)
    private boolean open = false;

    /** متى فتح المدرس الحصة */
    @Column(name = "opened_at")
    private LocalDateTime openedAt;

    /** متى أغلق المدرس الحصة */
    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    /** سجلات الحضور في هذه الحصة */
    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<AttendanceGroupRecord> records = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
