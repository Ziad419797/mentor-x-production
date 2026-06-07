package com.educore.quiz;

import com.educore.question.Question;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("QuizScoreCalculator Tests")
class QuizScoreCalculatorTest {

    private QuizScoreCalculator calculator;
    private Quiz quiz;

    @BeforeEach
    void setUp() {
        calculator = new QuizScoreCalculator();
        quiz = new Quiz();
    }

    // ── Helper Methods ────────────────────────────────────────────────

    private Question createQuestion(Long id, String correctAnswer, int mark) {
        Question question = new Question();
        question.setId(id);
        question.setCorrectAnswer(correctAnswer);
        question.setMark(mark);
        return question;
    }

    private void setQuestions(Quiz quiz, Question... questions) {
        quiz.setQuestions(new HashSet<>(Arrays.asList(questions)));
    }

    private Map<Long, String> createAnswerMap(Map<Long, String> answers) {
        return new HashMap<>(answers);
    }

    // ─────────────────────────────────────────────────────────
    // calculateScore Tests
    // ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("كل الإجابات صح — الدرجة كاملة")
    void allCorrect_fullScore() {
        // Given
        Question q1 = createQuestion(1L, "A", 5);
        Question q2 = createQuestion(2L, "B", 10);
        Question q3 = createQuestion(3L, "C", 5);
        setQuestions(quiz, q1, q2, q3);

        Map<Long, String> answers = new HashMap<>();
        answers.put(1L, "A");
        answers.put(2L, "B");
        answers.put(3L, "C");

        // When
        QuizScoreCalculator.ScoreCalculationResult result =
                calculator.calculateScore(quiz, answers);

        // Then
        assertEquals(20, result.getScore());
        assertEquals(20, result.getTotalMarks());
        assertEquals(100.0, result.getPercentage());
    }

    @Test
    @DisplayName("كل الإجابات غلط — الدرجة صفر")
    void allWrong_zeroScore() {
        // Given
        Question q1 = createQuestion(1L, "A", 5);
        Question q2 = createQuestion(2L, "B", 10);
        setQuestions(quiz, q1, q2);

        Map<Long, String> answers = new HashMap<>();
        answers.put(1L, "D");
        answers.put(2L, "C");

        // When
        QuizScoreCalculator.ScoreCalculationResult result =
                calculator.calculateScore(quiz, answers);

        // Then
        assertEquals(0, result.getScore());
        assertEquals(15, result.getTotalMarks());
        assertEquals(0.0, result.getPercentage());
    }

    @Test
    @DisplayName("نص الإجابات فاضية — الدرجة صفر")
    void emptyAnswers_zeroScore() {
        // Given
        Question q1 = createQuestion(1L, "A", 5);
        Question q2 = createQuestion(2L, "B", 10);
        setQuestions(quiz, q1, q2);

        // When
        QuizScoreCalculator.ScoreCalculationResult result =
                calculator.calculateScore(quiz, new HashMap<>());

        // Then
        assertEquals(0, result.getScore());
        assertEquals(15, result.getTotalMarks());
        assertEquals(0.0, result.getPercentage());
    }

    @Test
    @DisplayName("بعض الإجابات null — تُعتبر خطأ")
    void nullAnswers_treatedAsWrong() {
        // Given
        Question q1 = createQuestion(1L, "A", 5);
        Question q2 = createQuestion(2L, "B", 10);
        setQuestions(quiz, q1, q2);

        Map<Long, String> answers = new HashMap<>();
        answers.put(1L, null);  // null answer
        answers.put(2L, "B");   // correct answer

        // When
        QuizScoreCalculator.ScoreCalculationResult result =
                calculator.calculateScore(quiz, answers);

        // Then
        assertEquals(10, result.getScore()); // Only q2 is correct
        assertEquals(15, result.getTotalMarks());
        assertEquals(66.67, result.getPercentage(), 0.01);
    }

    @Test
    @DisplayName("Case-insensitive matching — 'a' يساوي 'A'")
    void caseInsensitive_correctAnswer() {
        // Given
        Question question = createQuestion(1L, "A", 10);
        setQuestions(quiz, question);

        Map<Long, String> answers = new HashMap<>();
        answers.put(1L, "a"); // lowercase

        // When
        QuizScoreCalculator.ScoreCalculationResult result =
                calculator.calculateScore(quiz, answers);

        // Then
        assertEquals(10, result.getScore());
        assertEquals(10, result.getTotalMarks());
        assertEquals(100.0, result.getPercentage());
    }

    @Test
    @DisplayName("Trim whitespace — ' A ' يساوي 'A'")
    void trimmedWhitespace_correctAnswer() {
        // Given
        Question question = createQuestion(1L, "A", 10);
        setQuestions(quiz, question);

        Map<Long, String> answers = new HashMap<>();
        answers.put(1L, "  A  "); // with spaces

        // When
        QuizScoreCalculator.ScoreCalculationResult result =
                calculator.calculateScore(quiz, answers);

        // Then
        assertEquals(10, result.getScore());
        assertEquals(10, result.getTotalMarks());
        assertEquals(100.0, result.getPercentage());
    }

    @Test
    @DisplayName("مفيش أسئلة — كل حاجة صفر")
    void noQuestions_zeroEverything() {
        // Given
        quiz.setQuestions(null);

        // When
        QuizScoreCalculator.ScoreCalculationResult result =
                calculator.calculateScore(quiz, new HashMap<>());

        // Then
        assertEquals(0, result.getScore());
        assertEquals(0, result.getTotalMarks());
        assertEquals(0.0, result.getPercentage());
    }

    @Test
    @DisplayName("مجموعة أسئلة فارغة — كل حاجة صفر")
    void emptyQuestionsSet_zeroEverything() {
        // Given
        quiz.setQuestions(new HashSet<>());

        // When
        QuizScoreCalculator.ScoreCalculationResult result =
                calculator.calculateScore(quiz, new HashMap<>());

        // Then
        assertEquals(0, result.getScore());
        assertEquals(0, result.getTotalMarks());
        assertEquals(0.0, result.getPercentage());
    }

    @Test
    @DisplayName("بعض الإجابات صح وبعضها غلط")
    void partialCorrect_partialScore() {
        // Given
        Question q1 = createQuestion(1L, "A", 10);
        Question q2 = createQuestion(2L, "B", 10);
        Question q3 = createQuestion(3L, "C", 10);
        setQuestions(quiz, q1, q2, q3);

        Map<Long, String> answers = new HashMap<>();
        answers.put(1L, "A"); // صح
        answers.put(2L, "X"); // غلط
        answers.put(3L, "C"); // صح

        // When
        QuizScoreCalculator.ScoreCalculationResult result =
                calculator.calculateScore(quiz, answers);

        // Then
        assertEquals(20, result.getScore());
        assertEquals(30, result.getTotalMarks());
        assertEquals(66.67, result.getPercentage(), 0.01);
    }

    @Test
    @DisplayName("أسئلة بدرجات مختلفة - حساب صحيح")
    void differentMarks_correctCalculation() {
        // Given
        Question q1 = createQuestion(1L, "A", 3);
        Question q2 = createQuestion(2L, "B", 7);
        Question q3 = createQuestion(3L, "C", 15);
        Question q4 = createQuestion(4L, "D", 5);
        setQuestions(quiz, q1, q2, q3, q4);

        Map<Long, String> answers = new HashMap<>();
        answers.put(1L, "A");  // صح (3)
        answers.put(2L, "B");  // صح (7)
        answers.put(3L, "X");  // غلط (0)
        answers.put(4L, "D");  // صح (5)

        // When
        QuizScoreCalculator.ScoreCalculationResult result =
                calculator.calculateScore(quiz, answers);

        // Then
        assertEquals(15, result.getScore()); // 3 + 7 + 5
        assertEquals(30, result.getTotalMarks()); // 3 + 7 + 15 + 5
        assertEquals(50.0, result.getPercentage());
    }

    @Test
    @DisplayName("أسئلة بترتيب عشوائي - لا يؤثر على النتيجة")
    void unorderedQuestions_correctStillWorks() {
        // Given
        Question q1 = createQuestion(1L, "A", 5);
        Question q2 = createQuestion(2L, "B", 5);
        Question q3 = createQuestion(3L, "C", 5);
        setQuestions(quiz, q3, q1, q2); // Different order

        Map<Long, String> answers = new HashMap<>();
        answers.put(2L, "B");  // صح
        answers.put(1L, "A");  // صح
        answers.put(3L, "Wrong"); // غلط

        // When
        QuizScoreCalculator.ScoreCalculationResult result =
                calculator.calculateScore(quiz, answers);

        // Then
        assertEquals(10, result.getScore());
        assertEquals(15, result.getTotalMarks());
        assertEquals(66.67, result.getPercentage(), 0.01);
    }

    // ─────────────────────────────────────────────────────────
    // calculateTotalMarks Tests
    // ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("حساب مجموع درجات الاختبار")
    void calculateTotalMarks_correct() {
        // Given
        Question q1 = createQuestion(1L, "A", 5);
        Question q2 = createQuestion(2L, "B", 10);
        Question q3 = createQuestion(3L, "C", 15);
        setQuestions(quiz, q1, q2, q3);

        // When & Then
        assertEquals(30, calculator.calculateTotalMarks(quiz));
    }

    @Test
    @DisplayName("مجموع درجات بدون أسئلة = صفر")
    void calculateTotalMarks_noQuestions() {
        // Given
        quiz.setQuestions(null);

        // When & Then
        assertEquals(0, calculator.calculateTotalMarks(quiz));
    }

    @Test
    @DisplayName("مجموع درجات مع أسئلة ولكن بعضها بدرجة صفر")
    void calculateTotalMarks_withZeroMarkQuestions() {
        // Given
        Question q1 = createQuestion(1L, "A", 0);
        Question q2 = createQuestion(2L, "B", 5);
        Question q3 = createQuestion(3L, "C", 0);
        setQuestions(quiz, q1, q2, q3);

        // When & Then
        assertEquals(5, calculator.calculateTotalMarks(quiz));
    }

    @Test
    @DisplayName("مجموع درجات مع مجموعة أسئلة فارغة")
    void calculateTotalMarks_emptySet() {
        // Given
        quiz.setQuestions(new HashSet<>());

        // When & Then
        assertEquals(0, calculator.calculateTotalMarks(quiz));
    }

    // ─────────────────────────────────────────────────────────
    // getPercentage Edge Cases
    // ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("نسبة مئوية لما TotalMarks = صفر = 0.0 بدون قسمة على صفر")
    void percentage_whenTotalMarksZero_returnsZero() {
        // When
        QuizScoreCalculator.ScoreCalculationResult result =
                new QuizScoreCalculator.ScoreCalculationResult(0, 0);

        // Then
        assertEquals(0.0, result.getPercentage());
    }

    @Test
    @DisplayName("نسبة مئوية عندما Score > TotalMarks (حالة غير طبيعية)")
    void percentage_whenScoreExceedsTotalMarks_returns100() {
        // When - This shouldn't happen normally, but defensive programming
        QuizScoreCalculator.ScoreCalculationResult result =
                new QuizScoreCalculator.ScoreCalculationResult(25, 20);

        // Then
        assertEquals(100.0, result.getPercentage()); // Or could cap at 100
    }

    @Test
    @DisplayName("نسبة مئوية مع rounding صحيح")
    void percentage_correctRounding() {
        // When
        QuizScoreCalculator.ScoreCalculationResult result1 =
                new QuizScoreCalculator.ScoreCalculationResult(1, 3);
        QuizScoreCalculator.ScoreCalculationResult result2 =
                new QuizScoreCalculator.ScoreCalculationResult(2, 3);

        // Then
        assertEquals(33.33, result1.getPercentage(), 0.01);
        assertEquals(66.67, result2.getPercentage(), 0.01);
    }
}