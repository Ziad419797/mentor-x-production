package com.educore.questionbank;

import com.educore.exception.ResourceNotFoundException;
import com.educore.lesson.LessonRepository;
import com.educore.lesson.Week;
import com.educore.questionbank.dto.BankQuestionRequest;
import com.educore.questionbank.dto.BankQuestionResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class BankQuestionService {

    private final BankQuestionRepository  bankRepository;
    private final QuestionTopicRepository topicRepository;
    private final LessonRepository        lessonRepository;

    // ─── إضافة سؤال للبنك ───────────────────────────────────────

    @Transactional
    public BankQuestionResponse create(BankQuestionRequest request) {
        QuestionTopic topic = topicRepository.findById(request.getTopicId())
                .orElseThrow(() -> new ResourceNotFoundException("الجزئية غير موجودة: " + request.getTopicId()));

        // التحقق من إن الـ correctAnswer موجود في الـ options
        if (!request.getOptions().contains(request.getCorrectAnswer())) {
            throw new IllegalArgumentException(
                "الإجابة الصحيحة لازم تكون ضمن الاختيارات المتاحة");
        }

        // ملحوظة: الجزئيات بقت مرتبطة بالمحاضرة (Session) مش بالدرس (Week)،
        // فالحقل ده بقى اختياري ومش بنشتقه من الـ topic بعد الآن
        BankQuestion question = BankQuestion.builder()
                .topic(topic)
                .conceptTag(request.getConceptTag())
                .imageUrl(request.getImageUrl())
                .description(request.getDescription())
                .mark(request.getMark() != null ? request.getMark() : 1)
                .options(request.getOptions())
                .correctAnswer(request.getCorrectAnswer())
                .difficulty(request.getDifficulty() != null
                        ? request.getDifficulty() : DifficultyLevel.MEDIUM)
                .active(true)
                .build();

        bankRepository.save(question);
        log.info("BankQuestion created: id={}, topic={}, concept={}",
                question.getId(), topic.getName(), request.getConceptTag());

        return toResponse(question);
    }

    // ─── تعديل سؤال ─────────────────────────────────────────────

    @Transactional
    public BankQuestionResponse update(Long id, BankQuestionRequest request) {
        BankQuestion q = findOrThrow(id);

        if (request.getTopicId() != null && !request.getTopicId().equals(q.getTopic().getId())) {
            QuestionTopic newTopic = topicRepository.findById(request.getTopicId())
                    .orElseThrow(() -> new ResourceNotFoundException("الجزئية غير موجودة"));
            q.setTopic(newTopic);
        }

        if (request.getOptions() != null && !request.getOptions().contains(request.getCorrectAnswer())) {
            throw new IllegalArgumentException("الإجابة الصحيحة لازم تكون ضمن الاختيارات");
        }

        if (request.getConceptTag()   != null) q.setConceptTag(request.getConceptTag());
        if (request.getImageUrl()     != null) q.setImageUrl(request.getImageUrl());
        if (request.getDescription()  != null) q.setDescription(request.getDescription());
        if (request.getMark()         != null) q.setMark(request.getMark());
        if (request.getOptions()      != null) q.setOptions(request.getOptions());
        if (request.getCorrectAnswer()!= null) q.setCorrectAnswer(request.getCorrectAnswer());
        if (request.getDifficulty()   != null) q.setDifficulty(request.getDifficulty());

        bankRepository.save(q);
        return toResponse(q);
    }

    // ─── حذف سؤال ───────────────────────────────────────────────

    @Transactional
    public void delete(Long id) {
        BankQuestion q = findOrThrow(id);
        q.setActive(false);
        bankRepository.save(q);
        log.info("BankQuestion deactivated: id={}", id);
    }

    // ─── جلب سؤال واحد ──────────────────────────────────────────

    @Transactional(readOnly = true)
    public BankQuestionResponse getById(Long id) {
        return toResponse(findOrThrow(id));
    }

    // ─── أسئلة درس معين ─────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<BankQuestionResponse> getByWeek(Long weekId, Pageable pageable) {
        return bankRepository.findByWeekIdAndActiveTrue(weekId, pageable)
                .map(this::toResponse);
    }

    // ─── أسئلة جزئية معينة ──────────────────────────────────────

    @Transactional(readOnly = true)
    public java.util.List<BankQuestionResponse> getByTopic(Long topicId) {
        return bankRepository.findByTopicIdAndActiveTrue(topicId).stream()
                .map(this::toResponse)
                .toList();
    }

    // ─── Helpers ─────────────────────────────────────────────────

    BankQuestion findOrThrow(Long id) {
        return bankRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("السؤال غير موجود في البنك: " + id));
    }

    BankQuestionResponse toResponse(BankQuestion q) {
        // عدد النسخ بنفس الـ conceptTag في نفس الجزئية
        int variantCount = 1;
        if (q.getConceptTag() != null && !q.getConceptTag().isBlank()) {
            variantCount = (int) bankRepository
                    .countByTopicIdAndConceptTagAndActiveTrue(q.getTopic().getId(), q.getConceptTag());
        }

        return BankQuestionResponse.builder()
                .id(q.getId())
                .topicId(q.getTopic().getId())
                .topicName(q.getTopic().getName())
                .weekId(q.getWeek() != null ? q.getWeek().getId() : null)
                .conceptTag(q.getConceptTag())
                .imageUrl(q.getImageUrl())
                .description(q.getDescription())
                .mark(q.getMark())
                .options(q.getOptions())
                .correctAnswer(q.getCorrectAnswer())
                .difficulty(q.getDifficulty())
                .variantCount(variantCount)
                .build();
    }
}
