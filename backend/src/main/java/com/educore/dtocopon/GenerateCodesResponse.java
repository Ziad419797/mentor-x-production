package com.educore.dtocopon;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GenerateCodesResponse {

    private int totalGenerated;
    private String targetType;
    private String targetName;
    private String batchLabel;
    private List<String> codes;
    private LocalDateTime expiresAt;
    private Integer maxUsesPerCode;

    /** سعر الكود — null = مجاني */
    private BigDecimal price;

    /** عنوان الحصة (لو targetType = SESSION) */
    private String sessionTitle;
}