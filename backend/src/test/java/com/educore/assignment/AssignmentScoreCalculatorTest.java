package com.educore.assignment;

import com.educore.assignment.assignmentQuestion.AssignmentQuestion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("AssignmentScoreCalculator Tests")
class AssignmentScoreCalculatorTest {

    private AssignmentScoreCalculator calculator;
    private Assignment assignment;

    @BeforeEach
    void setUp() {
        calculator = new AssignmentScoreCalculator();
        assignment = new Assignment();
    }

    // ── Helper Methods ────────────────────────────────────────────────

    private AssignmentQuestion createQuestion(Long id, String correctAnswer, int mark) {
        AssignmentQuestion question = new AssignmentQuestion();
        question.setId(id);
        question.setCorrectAnswer(correctAnswer);
        question.setMark(mark);
        return question;
    }

    private void setQuestions(Assignment assignment, AssignmentQuestion... questions) {
        assignment.setQuestions(new HashSet<>(Arrays.asList(questions)));
    }

    private Map<Long, String> createAnswers(Map<Long, String> answers) {
        return new HashMap<>(answers);
    }

    // ─────────────────────────────────────────────────────────
    // calculateScore Tests
    // ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("كل الإجابات صح — الدرجة كاملة")
    void allCorrect_fullScore() {
        // Given
        AssignmentQuestion q1 = createQuestion(1L, "Cairo", 10);
        AssignmentQuestion q2 = createQuestion(2L, "Paris", 10);
        AssignmentQuestion q3 = createQuestion(3L, "London", 10);
        setQuestions(assignment, q1, q2, q3);

        Map<Long, String> answers = new HashMap<>();
        answers.put(1L, "Cairo");
        answers.put(2L, "Paris");
        answers.put(3L, "London");

        // When
        AssignmentScoreCalculator.AssignmentScoreResult result =
                calculator.calculateScore(assignment, answers);

        // Then
        assertEquals(30, result.getScore());
        assertEquals(30, result.getTotalMarks());
        assertEquals(100.0, result.getPercentage());
    }

    @Test
    @DisplayName("الإجابات case-insensitive — 'cairo' تساوي 'Cairo'")
    void caseInsensitive_correctAnswer() {
        // Given
        AssignmentQuestion question = createQuestion(1L, "Cairo", 10);
        setQuestions(assignment, question);

        Map<Long, String> answers = new HashMap<>();
        answers.put(1L, "cairo"); // lowercase

        // When
        AssignmentScoreCalculator.AssignmentScoreResult result =
                calculator.calculateScore(assignment, answers);

        // Then
        assertEquals(10, result.getScore(), "يجب قبول الإجابة بغض النظر عن الـ case");
        assertEquals(10, result.getTotalMarks());
        assertEquals(100.0, result.getPercentage());
    }

    @Test
    @DisplayName("إجابة null — لا تُحسب")
    void nullAnswer_notCounted() {
        // Given
        AssignmentQuestion question = createQuestion(1L, "Cairo", 10);
        setQuestions(assignment, question);

        Map<Long, String> answers = new HashMap<>();
        answers.put(1L, null);

        // When
        AssignmentScoreCalculator.AssignmentScoreResult result =
                calculator.calculateScore(assignment, answers);

        // Then
        assertEquals(0, result.getScore());
        assertEquals(10, result.getTotalMarks());
        assertEquals(0.0, result.getPercentage());
    }

    @Test
    @DisplayName("إجابة غير موجودة في الخريطة — تُعتبر خطأ")
    void missingAnswer_treatedAsWrong() {
        // Given
        AssignmentQuestion question = createQuestion(1L, "Cairo", 10);
        setQuestions(assignment, question);

        Map<Long, String> answers = new HashMap<>(); // Empty map - missing answer

        // When
        AssignmentScoreCalculator.AssignmentScoreResult result =
                calculator.calculateScore(assignment, answers);

        // Then
        assertEquals(0, result.getScore());
        assertEquals(10, result.getTotalMarks());
        assertEquals(0.0, result.getPercentage());
    }

    @Test
    @DisplayName("مفيش أسئلة — كل حاجة صفر")
    void noQuestions_zeroScore() {
        // Given
        assignment.setQuestions(null); // or empty set

        // When
        AssignmentScoreCalculator.AssignmentScoreResult result =
                calculator.calculateScore(assignment, new HashMap<>());

        // Then
        assertEquals(0, result.getScore());
        assertEquals(0, result.getTotalMarks());
        assertEquals(0.0, result.getPercentage());
    }

    @Test
    @DisplayName("مجموعة أسئلة فارغة — كل حاجة صفر")
    void emptyQuestionsSet_zeroScore() {
        // Given
        assignment.setQuestions(new HashSet<>());

        // When
        AssignmentScoreCalculator.AssignmentScoreResult result =
                calculator.calculateScore(assignment, new HashMap<>());

        // Then
        assertEquals(0, result.getScore());
        assertEquals(0, result.getTotalMarks());
        assertEquals(0.0, result.getPercentage());
    }

    @Test
    @DisplayName("إجابات جزئية — درجة جزئية")
    void partialAnswers_partialScore() {
        // Given
        AssignmentQuestion q1 = createQuestion(1L, "A", 5);
        AssignmentQuestion q2 = createQuestion(2L, "B", 5);
        AssignmentQuestion q3 = createQuestion(3L, "C", 10);
        setQuestions(assignment, q1, q2, q3);

        Map<Long, String> answers = new HashMap<>();
        answers.put(1L, "A"); // صحيح ← 5 درجات
        answers.put(2L, "X"); // خطأ ← 0
        answers.put(3L, "C"); // صحيح ← 10 درجات

        // When
        AssignmentScoreCalculator.AssignmentScoreResult result =
                calculator.calculateScore(assignment, answers);

        // Then
        assertEquals(15, result.getScore());
        assertEquals(20, result.getTotalMarks());
        assertEquals(75.0, result.getPercentage());
    }

    @Test
    @DisplayName("أسئلة بدرجات مختلفة — حساب صحيح للمجموع")
    void differentMarks_correctTotalCalculation() {
        // Given
        AssignmentQuestion q1 = createQuestion(1L, "A", 3);
        AssignmentQuestion q2 = createQuestion(2L, "B", 7);
        AssignmentQuestion q3 = createQuestion(3L, "C", 15);
        AssignmentQuestion q4 = createQuestion(4L, "D", 5);
        setQuestions(assignment, q1, q2, q3, q4);

        Map<Long, String> answers = new HashMap<>();
        answers.put(1L, "A");  // صحيح
        answers.put(2L, "B");  // صحيح
        answers.put(3L, "C");  // صحيح
        answers.put(4L, "Wrong"); // خطأ

        // When
        AssignmentScoreCalculator.AssignmentScoreResult result =
                calculator.calculateScore(assignment, answers);

        // Then
        assertEquals(25, result.getScore()); // 3 + 7 + 15
        assertEquals(30, result.getTotalMarks()); // 3 + 7 + 15 + 5
        assertEquals(83.33, result.getPercentage(), 0.01); // 83.33%
    }

    // ─────────────────────────────────────────────────────────
    // getPercentage Edge Cases
    // ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("نسبة مئوية لما TotalMarks = صفر = 0.0 بدون ArithmeticException")
    void percentage_whenTotalMarksZero_returnsZero() {
        // When
        AssignmentScoreCalculator.AssignmentScoreResult result =
                new AssignmentScoreCalculator.AssignmentScoreResult(0, 0);

        // Then
        assertEquals(0.0, result.getPercentage());
    }

    @Test
    @DisplayName("نسبة مئوية عندما TotalMarks > 0")
    void percentage_whenTotalMarksPositive_correctCalculation() {
        // When
        AssignmentScoreCalculator.AssignmentScoreResult result =
                new AssignmentScoreCalculator.AssignmentScoreResult(15, 20);

        // Then
        assertEquals(75.0, result.getPercentage());
    }

    @Test
    @DisplayName("نسبة مئوية مع قيم عشرية - يتم التقريب")
    void percentage_withDecimalValues_roundedCorrectly() {
        // When
        AssignmentScoreCalculator.AssignmentScoreResult result1 =
                new AssignmentScoreCalculator.AssignmentScoreResult(10, 30);
        AssignmentScoreCalculator.AssignmentScoreResult result2 =
                new AssignmentScoreCalculator.AssignmentScoreResult(1, 3);

        // Then
        assertEquals(33.33, result1.getPercentage(), 0.01);
        assertEquals(33.33, result2.getPercentage(), 0.01);
    }
}