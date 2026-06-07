package com.educore.analytics.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class PlatformTimeDto {
    private double avgSessionMinutes;
    private double maxSessionMinutes;
    private long totalActiveSessions;
}
