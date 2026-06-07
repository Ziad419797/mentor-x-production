package com.educore.analytics.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class ActiveHourDto {
    private int hour;   // 0-23
    private long count;
}
