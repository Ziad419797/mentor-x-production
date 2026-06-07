package com.educore.dtocourse.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuizResponse {
    private Long id;
    private String title;
    private Boolean active;
    private Boolean deleted;
    private Boolean timeRestricted;
    private Integer durationMinutes;
    private Long weekId;
    private Long courseId;
    private List<QuestionResponse> questions;
    private Integer totalMarks;
    private Integer questionsCount;
    private Integer passingScore;
    private Integer attemptsAllowed;
    private String quizType;
    private String questionOrder;
    private Integer points;
    private String prizeName;
    private Integer prizeScore;
    private Boolean improvable;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
