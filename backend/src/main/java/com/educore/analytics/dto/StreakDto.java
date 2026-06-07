package com.educore.analytics.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@Builder
@AllArgsConstructor
public class StreakDto {
    private int currentStreakDays;
    private int longestStreakDays;  // not calculable without activity log, set 0
    private LocalDate lastActivityDate;
    private int totalActiveDays;
    private int loginCount;
}
