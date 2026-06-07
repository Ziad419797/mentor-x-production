package com.educore.staff;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * Staff — موظف في المنصة.
 *
 * المدرس (TEACHER) هو اللي بينشئ الموظفين ويختار صلاحياتهم.
 * الموظف بيسجّل دخول بـ phone + password ويحصل على JWT بـ role=STAFF.
 */
@Entity
@Table(
        name = "staff_members",
        indexes = {
                @Index(name = "idx_staff_phone",      columnList = "phone"),
                @Index(name = "idx_staff_teacher",    columnList = "teacher_id"),
                @Index(name = "idx_staff_active",     columnList = "active")
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Staff {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String fullName;

    /** رقم الهاتف — بيستخدمه لتسجيل الدخول */
    @Column(nullable = false, unique = true, length = 15)
    private String phone;

    @Column(nullable = false)
    private String password;

    /** معرّف المدرس اللي أنشأ الموظف */
    @Column(name = "teacher_id", nullable = false)
    private Long teacherId;

    /** الصلاحيات المفعّلة لهذا الموظف */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "staff_permissions",
            joinColumns = @JoinColumn(name = "staff_id",
                    foreignKey = @ForeignKey(name = "fk_staff_perm_staff"))
    )
    @Enumerated(EnumType.STRING)
    @Column(name = "permission", length = 50)
    @Builder.Default
    private Set<StaffPermission> permissions = new HashSet<>();

    /** الحساب مفعّل أم محجوب */
    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    /** ملاحظات اختيارية (المهام، القسم، إلخ) */
    @Column(length = 500)
    private String notes;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
