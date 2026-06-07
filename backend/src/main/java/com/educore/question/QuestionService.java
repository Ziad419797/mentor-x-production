package com.educore.question;

import com.educore.common.CacheNames;
import com.educore.common.SortValidator;
import com.educore.dtocourse.request.CreateQuestionRequest;
import com.educore.dtocourse.response.QuestionResponse;
import com.educore.exception.ResourceNotFoundException;
import com.educore.quiz.Quiz;
import com.educore.quiz.QuizRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class QuestionService {

    private final QuestionRepository questionRepository;
    private final QuizRepository quizRepository;

    private static final List<String> ALLOWED_SORT =
            List.of("id", "mark");

    private final SortValidator sortValidator;

    // ================= ADD =================

    @CacheEvict(value = {CacheNames.QUESTIONS_BY_QUIZ}, allEntries = true)
    @Transactional
    public QuestionResponse addQuestion(
            Long quizId,
            CreateQuestionRequest request
    ) {

        log.info("Adding question to quiz: {}", quizId);

        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new ResourceNotFoundException("Quiz not found with id: " + quizId));

        Question question = Question.builder()
                .imageUrl(request.getImageUrl())
                .description(request.getDescription())
                .mark(request.getMark())
                .options(request.getOptions())
                .correctAnswer(request.getCorrectAnswer())
                .quiz(quiz)
                .build();

        Question saved = questionRepository.save(question);

        log.info("Question added successfully to quiz {} with id: {}", quizId, saved.getId());

        return toResponse(saved);
    }

    // ================= GET BY QUIZ =================
    @Transactional(readOnly = true)
    @Cacheable(value = CacheNames.QUESTIONS_BY_QUIZ,
            key = "#quizId + '-' + #pageable.pageNumber")
    public Page<QuestionResponse> getQuestions(
            Long quizId,
            Pageable pageable
    ) {

        log.info("Fetching questions for quiz: {}, page: {}", quizId, pageable.getPageNumber());

        // التحقق من وجود الكويز أولاً
        if (!quizRepository.existsById(quizId)) {
            throw new ResourceNotFoundException("Quiz not found with id: " + quizId);
        }

        sortValidator.validate(pageable, ALLOWED_SORT);

        Page<Question> page =
                questionRepository.findAll(
                        (root, query, cb) ->
                                cb.equal(root.get("quiz").get("id"), quizId),
                        pageable
                );

        if (page.isEmpty()) {
            log.warn("No questions found for quiz: {}", quizId);
        }

        return page.map(this::toResponse);
    }

    // ================= DELETE =================
    @CacheEvict(value = {
            CacheNames.QUESTIONS,
            CacheNames.QUESTIONS_BY_QUIZ
    }, allEntries = true)
    @Transactional
    public void deleteQuestion(Long questionId) {

        log.info("Deleting question with id: {}", questionId);

        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new ResourceNotFoundException("Question not found with id: " + questionId));

        questionRepository.delete(question);

        log.info("Question {} deleted successfully", questionId);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = CacheNames.QUESTIONS, key = "#questionId")
    public QuestionResponse getQuestion(Long questionId) {

        log.info("Fetching question with id: {}", questionId);

        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new ResourceNotFoundException("Question not found with id: " + questionId));

        return toResponse(question);
    }

    // ================= UPDATE =================
    @CacheEvict(value = {
            CacheNames.QUESTIONS,
            CacheNames.QUESTIONS_BY_QUIZ
    }, allEntries = true)
    @Transactional
    public QuestionResponse updateQuestion(
            Long questionId,
            CreateQuestionRequest request
    ) {

        log.info("Updating question with id: {}", questionId);

        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new ResourceNotFoundException("Question not found with id: " + questionId));

        // تحديث الحقول المطلوبة فقط
        if (request.getImageUrl() != null) {
            question.setImageUrl(request.getImageUrl());
        }
        if (request.getDescription() != null) {
            question.setDescription(request.getDescription());
        }
        if (request.getMark() != null) {
            question.setMark(request.getMark());
        }
        if (request.getCorrectAnswer() != null) {
            question.setCorrectAnswer(request.getCorrectAnswer());
        }
        if (request.getOptions() != null) {
            question.setOptions(request.getOptions());
        }

        Question updated = questionRepository.save(question);

        log.info("Question {} updated successfully", questionId);

        return toResponse(updated);
    }

    // ================= DELETE ALL BY QUIZ =================
    @CacheEvict(value = {
            CacheNames.QUESTIONS,
            CacheNames.QUESTIONS_BY_QUIZ
    }, allEntries = true)
    @Transactional
    public void deleteAllQuestionsByQuiz(Long quizId) {

        log.info("Deleting all questions for quiz: {}", quizId);

        if (!quizRepository.existsById(quizId)) {
            throw new ResourceNotFoundException("Quiz not found with id: " + quizId);
        }

        questionRepository.deleteByQuizId(quizId);

        log.info("All questions deleted for quiz: {}", quizId);
    }

    // ================= HELPER =================
    private QuestionResponse toResponse(Question q) {
        return QuestionResponse.builder()
                .id(q.getId())
                .imageUrl(q.getImageUrl())
                .mark(q.getMark())
                .description(q.getDescription())
                .options(q.getOptions())
                .optionsCount(q.getOptions() != null ? q.getOptions().size() : 0)
                .correctAnswer(q.getCorrectAnswer())
                .build();
    }
}
