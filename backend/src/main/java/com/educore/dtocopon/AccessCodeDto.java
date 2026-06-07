package com.educore.dtocopon;

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
public class AccessCodeDto {

    private Long id;
    private String code;
    private String targetType;
    private Long categoryId;
    private String categoryName;
    private Long courseId;
    private String courseName;
    private Integer maxUses;
    private Integer usedCount;
    private Integer remainingUses;
    private Boolean active;
    private String batchLabel;
    private LocalDateTime expiresAt;
    private LocalDateTime createdAt;
    private String createdByName;

    // ── حصة مستهدفة (SESSION type) ──
    private Long sessionId;
    private String sessionTitle;

    // ── سعر الكود (null = مجاني) ──
    private BigDecimal price;
}
