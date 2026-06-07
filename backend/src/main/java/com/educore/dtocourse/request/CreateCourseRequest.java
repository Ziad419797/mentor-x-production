package com.educore.dtocourse.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateCourseRequest {

    @NotBlank(message = "عنوان الكورس مطلوب")
    private String title;

    @NotBlank(message = "وصف الكورس مطلوب")
    private String description;

    @NotNull(message = "سعر الكورس مطلوب")
    private BigDecimal price;

    @NotBlank(message = "نوع التدريس مطلوب (ONLINE / CENTER / BOTH)")
    private String teachingType;

    @NotBlank(message = "ترتيب المحتوى مطلوب (NONE / LOCK_BY_SESSION / LOCK_BY_ELEMENT)")
    private String contentOrder;

    @Size(max = 500, message = "Image URL max 500 characters")
    private String imageUrl;

    private Integer orderNumber;   // optional — defaults to 1 in service if null

    private Set<Long> categoryIds; // optional — course can exist without categories

    /** fallback للـ frontend اللي بيبعت categoryId (مفرد) — يُدمج مع categoryIds */
    private Long categoryId;

    /** يرجع الـ IDs من categoryIds أو categoryId (مهما وصل الاتنين أو أي منهم) */
    public Set<Long> getEffectiveCategoryIds() {
        java.util.Set<Long> ids = new java.util.HashSet<>();
        if (categoryIds != null) ids.addAll(categoryIds);
        if (categoryId != null) ids.add(categoryId);
        return ids;
    }

    /** السعر بعد الخصم (اختياري) */
    private BigDecimal discountedPrice;

    /** نسبة الخصم 0-100 (للعرض فقط) */
    @Min(value = 0, message = "نسبة الخصم لا تقل عن 0")
    @Max(value = 100, message = "نسبة الخصم لا تزيد عن 100")
    private Integer discountPercentage;

    /** عدد أيام الوصول بعد الاشتراك (null = مفتوح) */
    @Min(value = 1, message = "عدد الأيام يجب أن يكون على الأقل يوم")
    private Integer accessDays;

    /** تاريخ انتهاء ثابت للكورس (null = بدون انتهاء) */
    private LocalDate accessExpiresAt;

    /** نوع الكورس (مثل: مسجل، مباشر...) */
    private String courseType;

    /** النقاط التي يحصل عليها الطالب عند إتمام الكورس (اختياري) */
    @Min(value = 0, message = "النقاط لا تقل عن 0")
    private Integer studentPoints;

    /** هل الحضور/الغياب محسوب لهذا الكورس */
    private boolean trackAttendance;

    /** كورس مميز — يظهر في الصفحة الرئيسية */
    private boolean featured;

    /** كورس مثبت — يظهر في أعلى القائمة */
    private boolean pinned;
}
