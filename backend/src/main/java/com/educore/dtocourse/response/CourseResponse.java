package com.educore.dtocourse.response;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CourseResponse {

    private Long id;

    private String title;

    private String description;
    private String imageUrl;  // ← رابط الصورة
    private BigDecimal price;
    private Long enrolledStudentsCount;
    private BigDecimal discountedPrice;
    private Integer discountPercentage;
    private Integer accessDays;
    private LocalDate accessExpiresAt;

    private Integer orderNumber;

    private Boolean active;

    // لإظهار الكاتيجوريز المرتبط بها الكورس
    private Set<Long> categoryIds;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private String courseType;
    private String teachingType;
    private Integer studentPoints;
    private String contentOrder;
    private Boolean trackAttendance;
    private Boolean featured;
    private Boolean pinned;
}
