package com.educore.dtocourse.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class LevelStatsResponse {
    private Long   levelId;
    private String levelName;
    private long   categoriesCount;
    private long   coursesCount;
    private long   sessionsCount;
    private long   studentsCount;
    private long   videosCount;
    private long   quizzesCount;
    private long   questionsCount;
}
