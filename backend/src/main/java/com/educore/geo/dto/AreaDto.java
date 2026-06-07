package com.educore.geo.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AreaDto {
    private Long   id;
    private String name;
    private Integer displayOrder;
}
