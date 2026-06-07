package com.educore.attendance.group;

import com.educore.student.Student;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * عضوية الطالب في جروب الحضور.
 */
@Entity
@Table(
    name = "attendance_group_members",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_member_group_student",
        columnNames = {"group_id", "student_id"}
    ),
    indexes = {
        @Index(name = "idx_att_mem_group",   columnList = "group_id"),
        @Index(name = "idx_att_mem_student", columnList = "student_id")
    }
)
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class AttendanceGroupMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "group_id", nullable = false)
    private AttendanceGroup group;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @Builder.Default
    @Column(nullable = false)
    private boolean active = true;

    @CreationTimestamp
    @Column(name = "joined_at", updatable = false)
    private LocalDateTime joinedAt;
}
