package com.educore.analytics.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class AchievementsDto {
    private List<Achievement> unlocked;
    private List<Achievement> locked;

    @Getter
    @Builder
    @AllArgsConstructor
    public static class Achievement {
        private String id;
        private String title;
        private String description;
        private String icon;
        private boolean unlocked;
        /** توقيت الحصول على الإنجاز (null لو لسه ملحقش) */
        private LocalDateTime unlockedAt;
        /** الثريشولد المطلوب (مثلاً 5 كويزات) */
        private int threshold;
        /** التقدم الحالي (مثلاً 3 من 5) */
        private long currentProgress;
    }
}
