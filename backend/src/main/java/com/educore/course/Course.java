package com.educore.course;

import com.educore.category.Category;
import com.educore.unit.Session;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.math.BigDecimal;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.*;
import org.hibernate.annotations.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(
        name = "courses",
        indexes = {
                @Index(name = "idx_course_active", columnList = "active")
        }
)
@SQLDelete(sql = "UPDATE courses SET active = false WHERE id = ?")
@Where(clause = "active = true")
@Cacheable
@org.hibernate.annotations.Cache(
        usage = CacheConcurrencyStrategy.READ_WRITE
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Course {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    private String description;
    @Column(length = 500)
    private String imageUrl;  // ← رابط الصورة من Cloudinary

    @Column(precision = 15, scale = 2)
    private BigDecimal price;

    /**
     * السعر بعد الخصم — لو مش null يُعرض بدل price الأصلي.
     * السعر الأصلي يتعرض شطب عليه في الفرونتند.
     */
    @Column(name = "discounted_price", precision = 15, scale = 2)
    private BigDecimal discountedPrice;

    /**
     * نسبة الخصم (0-100) — محسوبة تلقائياً أو يحددها المدرس.
     * للعرض فقط، مش مستخدمة في الحسابات.
     */
    @Column(name = "discount_percentage")
    private Integer discountPercentage;

    /**
     * عدد أيام الوصول للكورس بعد تسجيل الطالب.
     * مثال: 30 = الطالب عنده 30 يوم من وقت الاشتراك.
     * null = وصول مفتوح.
     */
    @Column(name = "access_days")
    private Integer accessDays;

    /**
     * تاريخ انتهاء ثابت للكورس — ينتهي لجميع الطلاب في هذا التاريخ.
     * لو مضروب مع accessDays، accessDays يأخذ الأولوية لكل طالب بشكل منفصل.
     * null = بدون تاريخ انتهاء ثابت.
     */
    @Column(name = "access_expires_at")
    private LocalDate accessExpiresAt;

    /**
     * نوع الكورس (مثل: مسجل، مباشر، باكدج...)
     */
    private String courseType;

    /**
     * نوع التدريس: ONLINE = أونلاين، CENTER = سنتر (حضوري)
     */
    @Column(name = "teaching_type", length = 20)
    private String teachingType;

    /**
     * النقاط التي يحصل عليها الطالب عند إتمام الكورس
     */
    @Column(name = "student_points")
    private Integer studentPoints;

    /**
     * ترتيب الوصول لمحتويات الكورس:
     * NONE, LOCK_BY_SESSION, LOCK_BY_ELEMENT, LOCK_BY_ELEMENT_IN_SESSION
     */
    @Column(name = "content_order", length = 30)
    private String contentOrder;

    /**
     * هل الحضور/الغياب محسوب لهذا الكورس
     */
    @Builder.Default
    @Column(name = "track_attendance", columnDefinition = "boolean default false")
    private Boolean trackAttendance = false;

    /**
     * كورس مميز — يظهر في الصفحة الرئيسية للطالب
     */
    @Builder.Default
    @Column(name = "featured", columnDefinition = "boolean default false")
    private Boolean featured = false;

    /**
     * كورس مثبّت — يظهر في أعلى القائمة
     */
    @Builder.Default
    @Column(name = "pinned", columnDefinition = "boolean default false")
    private Boolean pinned = false;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Builder.Default
    @Column(name = "active")
    private boolean active = true;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "course_category",
        joinColumns = @JoinColumn(name = "course_id"),
        inverseJoinColumns = @JoinColumn(name = "category_id")
    )
    @JsonIgnoreProperties("courses")
    @Builder.Default
    private Set<Category> categories = new HashSet<>();

    @ManyToMany(mappedBy = "courses", fetch = FetchType.LAZY)
    @JsonIgnoreProperties("courses")
    @Builder.Default
    private Set<Session> sessions = new HashSet<>();

    public boolean isActive() { return active; }
}
