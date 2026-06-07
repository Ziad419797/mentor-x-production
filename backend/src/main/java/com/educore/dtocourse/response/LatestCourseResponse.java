package com.educore.dtocourse.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LatestCourseResponse {
    private Long id;
    private String title;
    private String description;
    private String imageUrl;
    private BigDecimal price;
    private LocalDateTime createdAt;
    private Integer totalSessions;
    private Integer totalWeeks;
    private boolean isEnrolled;  // للطلاب فقط
    private Long enrolledStudentsCount;  // ✅ أضف هذا

}