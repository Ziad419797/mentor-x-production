package com.educore.banner.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "بيانات البانر")
public class BannerResponse {

    @Schema(description = "معرف البانر", example = "1")
    private Long id;

    @Schema(description = "عنوان البانر", example = "عرض خاص بمناسبة رمضان")
    private String title;

    @Schema(description = "وصف البانر", example = "خصم 50% على جميع الكورسات")
    private String description;

    @Schema(description = "رابط الصورة", example = "https://cloudinary.com/banner.jpg")
    private String imageUrl;

    @Schema(description = "رابط عند الضغط", example = "/courses/1")
    private String linkUrl;

    @Schema(description = "ترتيب العرض", example = "1")
    private Integer displayOrder;

    @Schema(description = "هل البانر نشط حالياً؟", example = "true")
    private boolean active;

    @Schema(description = "تاريخ بدء الظهور", example = "2024-01-01T00:00:00")
    private LocalDateTime startDate;

    @Schema(description = "تاريخ انتهاء الظهور", example = "2024-12-31T23:59:59")
    private LocalDateTime endDate;

    @Schema(description = "تاريخ الإنشاء")
    private LocalDateTime createdAt;
}