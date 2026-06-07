package com.educore.banner.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;

@Getter
@Setter
@Schema(description = "طلب إنشاء بانر جديد")
public class CreateBannerRequest {

    @NotBlank(message = "عنوان البانر مطلوب")
    @Schema(description = "عنوان البانر", example = "عرض خاص بمناسبة رمضان")
    private String title;

    @Schema(description = "وصف البانر", example = "خصم 50% على جميع الكورسات")
    private String description;

    @Schema(hidden = true)  // ✅ مخفي من Swagger (سيتم استقباله من @RequestPart)
    private MultipartFile imageFile;

    @Schema(description = "رابط عند الضغط", example = "/courses/1")
    private String linkUrl;

    @Schema(description = "ترتيب العرض", example = "1")
    private Integer displayOrder;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)

    @Schema(description = "تاريخ بدء الظهور", example = "2024-01-01T00:00:00")
    private LocalDateTime startDate;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)

    @Schema(description = "تاريخ انتهاء الظهور", example = "2024-12-31T23:59:59")
    private LocalDateTime endDate;
}