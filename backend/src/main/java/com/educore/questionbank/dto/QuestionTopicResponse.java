package com.educore.questionbank.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class QuestionTopicResponse {

    private Long   id;
    private String name;
    private String description;
    private Long   sessionId;
    private String sessionTitle;
    private Long   parentTopicId;
    private String parentTopicName;
    private Integer orderNumber;
    private int    questionCount;    // عدد الأسئلة المباشرة في الجزئية
    private int    totalQuestions;   // إجمالي الأسئلة (مع الجزئيات الفرعية)

    /** الجزئيات الفرعية — هياركي متداخل */
    private List<QuestionTopicResponse> subTopics;
}
