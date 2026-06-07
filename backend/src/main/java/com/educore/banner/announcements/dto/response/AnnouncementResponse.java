package com.educore.banner.announcements.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@Schema(description = "بيانات الإعلان")
public class AnnouncementResponse {

    @Schema(description = "معرف الإعلان", example = "1")
    private Long id;

    @Schema(description = "عنوان الإعلان", example = "عطلة رسمية بمناسبة عيد الفطر")
    private String title;

    @Schema(description = "نص الإعلان", example = "سوف تكون المنصة مغلقة يوم الأحد القادم")
    private String description;

    @Schema(description = "تاريخ الإعلان", example = "2024-01-01T00:00:00")
    private LocalDateTime announcementDate;

    @Schema(description = "تاريخ انتهاء الإعلان", example = "2024-12-31T23:59:59")
    private LocalDateTime expiryDate;

    @Schema(description = "هل الإعلان نشط حالياً؟", example = "true")
    private boolean active;

    @Schema(description = "تاريخ الإنشاء")
    private LocalDateTime createdAt;
}
