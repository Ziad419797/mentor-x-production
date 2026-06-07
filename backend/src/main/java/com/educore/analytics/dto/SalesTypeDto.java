package com.educore.analytics.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class SalesTypeDto {
    private String enrollmentType;  // "COURSE" or "CATEGORY"
    private long count;
    private double percentage;
}
