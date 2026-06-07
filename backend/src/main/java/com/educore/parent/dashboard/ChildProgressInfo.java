package com.educore.parent.dashboard;

import com.educore.lessongate.LessonProgressStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChildProgressInfo {

    private Long                  weekId;
    private String                weekTitle;
    private Integer               orderNumber;
    private LessonProgressStatus  status;
    private Integer               quizScore;
    private Boolean               quizPassed;
    private LocalDateTime         unlockedAt;
    private LocalDateTime         startedAt;
    private LocalDateTime         completedAt;
}
