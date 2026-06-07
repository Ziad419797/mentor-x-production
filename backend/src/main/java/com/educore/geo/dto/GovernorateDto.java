package com.educore.geo.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class GovernorateDto {
    private Long   id;
    private String nameAr;
    private String nameEn;
    private Integer displayOrder;
    /** محتوى فقط لو طُلبت مع المناطق */
    private List<AreaDto> areas;
}
