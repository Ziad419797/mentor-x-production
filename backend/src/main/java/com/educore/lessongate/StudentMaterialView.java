package com.educore.lessongate;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * يتتبع المواد التعليمية التي شاهدها الطالب.
 * يُستخدم في gate نوع LOCK_BY_ELEMENT لفتح العنصر التالي.
 */
@Entity
@Table(
    name = "student_material_views",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uq_student_material",
            columnNames = {"student_id", "material_id"}
        )
    },
    indexes = {
        @Index(name = "idx_smv_student",   columnList = "student_id"),
        @Index(name = "idx_smv_material",  columnList = "material_id")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentMaterialView {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    @Column(name = "material_id", nullable = false)
    private Long materialId;

    @CreationTimestamp
    @Column(name = "viewed_at", updatable = false)
    private LocalDateTime viewedAt;
}
