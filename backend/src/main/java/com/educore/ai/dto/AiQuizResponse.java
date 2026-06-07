package com.educore.ai.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * رد الـ AI service على طلب توليد كويز.
 * الـ questions هي قائمة من أسئلة MCQ.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class AiQuizResponse {

    @JsonProperty("lesson_id")
    private Long lessonId;

    @JsonProperty("num_questions")
    private int numQuestions;

    private String difficulty;

    /** قائمة الأسئلة — كل سؤال بيتيجي من الـ AI بصيغة JSON */
    private List<QuizQuestion> questions;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class QuizQuestion {
        private String              question;
        private Map<String, String> options;        // A, B, C, D

        @JsonProperty("correct_answer")
        private String correctAnswer;               // A | B | C | D

        private String explanation;
    }
}
