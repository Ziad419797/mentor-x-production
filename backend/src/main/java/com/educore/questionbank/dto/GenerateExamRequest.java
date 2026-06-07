package com.educore.questionbank.dto;

import com.educore.questionbank.DifficultyLevel;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class GenerateExamRequest {

    @NotNull(message = "رقم الدرس مطلوب")
    private Long weekId;

    @NotBlank(message = "عنوان الكويز مطلوب")
    private String quizTitle;

    @NotNull(message = "مدة الكويز مطلوبة")
    @Min(value = 1, message = "المدة لا تقل عن دقيقة")
    private Integer durationMinutes;

    /** هل الكويز بيقيد الطالب بالوقت؟ */
    private boolean timeRestricted = true;

    /**
     * الجزئيات المطلوب السحب منها.
     * null أو فارغة = كل الجزئيات النشطة في الدرس.
     */
    private List<Long> topicIds;

    /**
     * عدد الأسئلة من كل جزئية.
     * السيستم بياخد N أسئلة من كل جزئية (1 من كل conceptTag).
     * لو الجزئية عندها 3 concepts → بياخد 3 أسئلة منها.
     * لو questionsPerConceptGroup = 1 (الافتراضي) → سؤال واحد من كل فكرة.
     */
    private int questionsPerConceptGroup = 1;

    /**
     * رتّب الأسئلة بشكل عشوائي في الكويز.
     * يضمن إن كل طالب يشوف نفس الأسئلة بترتيب مختلف.
     */
    private boolean shuffleQuestions = true;

    /**
     * رتّب الاختيارات داخل كل سؤال بشكل عشوائي.
     * لأن الـ correctAnswer متحفظ كـ text (مش index) → التصحيح التلقائي مش بيتأثر.
     */
    private boolean shuffleOptions = false;

    /** فلتر مستوى الصعوبة */
    private DifficultyLevel difficulty = DifficultyLevel.ALL;
}
