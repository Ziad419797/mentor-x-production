package com.educore.banner.announcements.dto.request;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@Schema(description = "طلب إنشاء إعلان جديد")
public class CreateAnnouncementRequest {

    @NotBlank(message = "عنوان الإعلان مطلوب")
    @Schema(description = "عنوان الإعلان", example = "عطلة رسمية بمناسبة عيد الفطر")
    private String title;

    @NotBlank(message = "نص الإعلان مطلوب")
    @Schema(description = "نص الإعلان", example = "سوف تكون المنصة مغلقة يوم الأحد القادم بمناسبة عيد الفطر")
    private String description;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    @Schema(description = "تاريخ الإعلان", example = "2024-01-01T00:00:00")
    private LocalDateTime announcementDate;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    @Schema(description = "تاريخ انتهاء الإعلان", example = "2024-12-31T23:59:59")
    private LocalDateTime expiryDate;
}