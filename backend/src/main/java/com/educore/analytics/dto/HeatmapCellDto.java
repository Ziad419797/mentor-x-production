package com.educore.analytics.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class HeatmapCellDto {
    private int dayOfWeek;   // 0=Sunday … 6=Saturday
    private int hour;        // 0-23
    private long count;
}
