package com.educore.questionbank.dto;

import com.educore.questionbank.DifficultyLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class BankQuestionResponse {

    private Long            id;
    private Long            topicId;
    private String          topicName;
    private Long            weekId;
    private String          conceptTag;
    private String          imageUrl;
    private String          description;
    private Integer         mark;
    private List<String>    options;
    private DifficultyLevel difficulty;
    // correctAnswer مش بيتبعت للمدرس إلا في وضع التعديل
    private String          correctAnswer;
    private int             variantCount; // عدد النسخ بنفس الـ conceptTag
}
