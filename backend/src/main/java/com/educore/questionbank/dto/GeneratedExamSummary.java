package com.educore.questionbank.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class GeneratedExamSummary {

    private Long   quizId;
    private String quizTitle;
    private Long   weekId;
    private String weekTitle;
    private int    questionCount;
    private int    totalMarks;
    private int    durationMinutes;
    private boolean shuffled;

    /** ملخص الجزئيات اللي اتسحب منها الأسئلة */
    private List<TopicSummary> topicsCovered;

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class TopicSummary {
        private Long   topicId;
        private String topicName;
        private int    questionsSelected;
        private int    totalConcepts;   // عدد الـ concept groups في الجزئية
    }
}
