package com.educore.attendance.group;

import com.educore.center.Center;
import com.educore.teacher.Teacher;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * جروب الحضور الأوفلاين الخاص بمدرس في سنتر معين.
 * مثال: "مجموعة السبت 10 صباحاً — سنتر المقطم"
 */
@Entity
@Table(
    name = "attendance_groups",
    indexes = {
        @Index(name = "idx_att_grp_teacher", columnList = "teacher_id"),
        @Index(name = "idx_att_grp_center",  columnList = "center_id"),
        @Index(name = "idx_att_grp_active",  columnList = "active")
    }
)
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class AttendanceGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ─── المدرس صاحب الجروب ──────────────────────────────────
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "teacher_id", nullable = false)
    private Teacher teacher;

    // ─── السنتر (nullable — لو مش مرتبط بسنتر محدد) ────────
    // ─── الصف الدراسي (اختياري) ─────────────────────────────
    @Column(name = "level_id")
    private Long levelId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "center_id")
    private Center center;

    /**
     * اسم السنتر كـ String (denormalized) — يُستخدم في الـ alert
     * لو center مش null يتملى منه تلقائياً
     */
    @Column(name = "center_name", length = 150)
    private String centerName;

    // ─── بيانات الجروب ───────────────────────────────────────
    @Column(nullable = false, length = 200)
    private String title;

    /** يوم الأسبوع — مثال: "السبت" */
    @Column(name = "day_of_week", length = 20)
    private String dayOfWeek;

    /** وقت الميعاد — مثال: "10:30" (HH:mm) */
    @Column(name = "meeting_time", length = 10)
    private String meetingTime;

    @Column(length = 500)
    private String description;

    @Builder.Default
    @Column(nullable = false)
    private boolean active = true;

    /** الحد الأقصى لعدد الطلاب في الجروب (null = بدون حد) */
    @Column(name = "max_capacity")
    private Integer maxCapacity;

    // ─── الحصص ───────────────────────────────────────────────
    @OneToMany(mappedBy = "group", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @OrderBy("sessionDate DESC")
    @Builder.Default
    private List<AttendanceGroupSession> sessions = new ArrayList<>();

    // ─── الأعضاء ─────────────────────────────────────────────
    @OneToMany(mappedBy = "group", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<AttendanceGroupMember> members = new ArrayList<>();

    // ─── Audit ───────────────────────────────────────────────
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
