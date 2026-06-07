package com.educore.copon;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "access_code_usages",
        uniqueConstraints = {
                @UniqueConstraint(
                        name  = "uk_code_student",
                        columnNames = {"access_code_id", "student_id"}
                )
        },
        indexes = {
                @Index(name = "idx_acu_code",    columnList = "access_code_id"),
                @Index(name = "idx_acu_student", columnList = "student_id")
        }
)
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class AccessCodeUsage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "access_code_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_acu_code"))
    private AccessCode accessCode;

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    @Column(name = "student_name", length = 150)
    private String studentName;

    @Column(name = "student_code", length = 20)
    private String studentCode;

    /** عدد الكورسات اللي اتفتحت لهذا الطالب من هذا الكود */
    @Column(name = "enrollments_created", nullable = false)
    @Builder.Default
    private Integer enrollmentsCreated = 0;

    @CreationTimestamp
    @Column(name = "used_at", updatable = false)
    private LocalDateTime usedAt;
}
