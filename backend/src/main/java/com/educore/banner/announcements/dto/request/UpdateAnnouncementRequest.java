package com.educore.banner.announcements.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@Schema(description = "طلب تحديث إعلان")
public class UpdateAnnouncementRequest {

    @Schema(description = "عنوان الإعلان", example = "عطلة رسمية بمناسبة عيد الفطر")
    private String title;

    @Schema(description = "نص الإعلان", example = "سوف تكون المنصة مغلقة يوم الأحد القادم")
    private String description;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    @Schema(description = "تاريخ الإعلان", example = "2024-01-01T00:00:00")
    private LocalDateTime announcementDate;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    @Schema(description = "تاريخ انتهاء الإعلان", example = "2024-12-31T23:59:59")
    private LocalDateTime expiryDate;

    @Schema(description = "تفعيل/إلغاء الإعلان", example = "true")
    private Boolean active;
}
