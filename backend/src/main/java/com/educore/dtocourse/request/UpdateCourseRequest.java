package com.educore.dtocourse.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateCourseRequest {

    @NotBlank(message = "Course title is required")
    private String title;

    private String description;
    // 🔥 أضف هذا الحقل
    @Size(max = 500, message = "Image URL max 500 characters")
    private String imageUrl;  // ← رابط الصورة
    private BigDecimal price;  // ✅ أضف هذا

    private Integer orderNumber;

    private Boolean active;

    /** السعر بعد الخصم (null = بدون خصم) */
    private BigDecimal discountedPrice;

    /** نسبة الخصم 0-100 (للعرض فقط) */
    @Min(0) @Max(100)
    private Integer discountPercentage;

    /** عدد أيام الوصول بعد الاشتراك (null = مفتوح) */
    @Min(1)
    private Integer accessDays;

    /** تاريخ انتهاء ثابت للكورس (null = بدون انتهاء) */
    private LocalDate accessExpiresAt;

    private String courseType;
    private String teachingType;
    private Integer studentPoints;
    private String contentOrder;
    private Boolean trackAttendance;
    private Boolean featured;
    private Boolean pinned;
}
