package com.educore.questionbank;

import com.educore.exception.ResourceNotFoundException;
import com.educore.unit.Session;
import com.educore.unit.SessionRepository;
import com.educore.questionbank.dto.QuestionTopicRequest;
import com.educore.questionbank.dto.QuestionTopicResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class QuestionTopicService {

    private final QuestionTopicRepository topicRepository;
    private final SessionRepository       sessionRepository;

    // ─── إنشاء جزئية ───────────────────────────────────────────

    @Transactional
    public QuestionTopicResponse create(QuestionTopicRequest request) {
        Session session = sessionRepository.findById(request.getSessionId())
                .orElseThrow(() -> new ResourceNotFoundException("المحاضرة غير موجودة: " + request.getSessionId()));

        QuestionTopic parent = null;
        if (request.getParentTopicId() != null) {
            parent = topicRepository.findById(request.getParentTopicId())
                    .orElseThrow(() -> new ResourceNotFoundException("الجزئية الأب غير موجودة: " + request.getParentTopicId()));
        }

        QuestionTopic topic = QuestionTopic.builder()
                .name(request.getName())
                .description(request.getDescription())
                .session(session)
                .parentTopic(parent)
                .orderNumber(request.getOrderNumber())
                .active(true)
                .build();

        topicRepository.save(topic);
        log.info("QuestionTopic created: id={}, name={}, session={}", topic.getId(), topic.getName(), session.getId());

        return toResponse(topic, 0, 0);
    }

    // ─── تعديل جزئية ───────────────────────────────────────────

    @Transactional
    public QuestionTopicResponse update(Long id, QuestionTopicRequest request) {
        QuestionTopic topic = findOrThrow(id);

        topic.setName(request.getName());
        topic.setDescription(request.getDescription());
        if (request.getOrderNumber() != null) topic.setOrderNumber(request.getOrderNumber());

        if (request.getParentTopicId() != null) {
            QuestionTopic parent = topicRepository.findById(request.getParentTopicId())
                    .orElseThrow(() -> new ResourceNotFoundException("الجزئية الأب غير موجودة"));
            topic.setParentTopic(parent);
        }

        topicRepository.save(topic);
        return toResponse(topic, 0, 0);
    }

    // ─── حذف جزئية ─────────────────────────────────────────────

    @Transactional
    public void delete(Long id) {
        QuestionTopic topic = findOrThrow(id);
        topic.setActive(false);
        topicRepository.save(topic);
        log.info("QuestionTopic deactivated: id={}", id);
    }

    // ─── شجرة الجزئيات لمحاضرة معينة ───────────────────────────

    @Transactional(readOnly = true)
    public List<QuestionTopicResponse> getTopicTree(Long sessionId) {
        // نجيب الجزئيات الرئيسية فقط — الـ subTopics بتيجي nested تلقائياً
        return topicRepository.findRootTopicsBySession(sessionId).stream()
                .map(this::toResponseWithChildren)
                .toList();
    }

    // ─── قائمة مسطّحة بكل الجزئيات لمحاضرة معينة ──────────────

    @Transactional(readOnly = true)
    public List<QuestionTopicResponse> getFlatTopics(Long sessionId) {
        return topicRepository.findAllBySession(sessionId).stream()
                .map(t -> toResponse(t, 0, 0))
                .toList();
    }

    // ─── Helpers ────────────────────────────────────────────────

    private QuestionTopic findOrThrow(Long id) {
        return topicRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("الجزئية غير موجودة: " + id));
    }

    QuestionTopicResponse toResponseWithChildren(QuestionTopic t) {
        List<QuestionTopicResponse> children = t.getSubTopics() == null
                ? List.of()
                : t.getSubTopics().stream()
                    .filter(QuestionTopic::isActive)
                    .map(this::toResponseWithChildren)
                    .toList();

        return QuestionTopicResponse.builder()
                .id(t.getId())
                .name(t.getName())
                .description(t.getDescription())
                .sessionId(t.getSession() != null ? t.getSession().getId() : null)
                .sessionTitle(t.getSession() != null ? t.getSession().getTitle() : null)
                .parentTopicId(t.getParentTopic() != null ? t.getParentTopic().getId() : null)
                .parentTopicName(t.getParentTopic() != null ? t.getParentTopic().getName() : null)
                .orderNumber(t.getOrderNumber())
                .subTopics(children)
                .build();
    }

    QuestionTopicResponse toResponse(QuestionTopic t, int directCount, int totalCount) {
        return QuestionTopicResponse.builder()
                .id(t.getId())
                .name(t.getName())
                .description(t.getDescription())
                .sessionId(t.getSession() != null ? t.getSession().getId() : null)
                .sessionTitle(t.getSession() != null ? t.getSession().getTitle() : null)
                .parentTopicId(t.getParentTopic() != null ? t.getParentTopic().getId() : null)
                .parentTopicName(t.getParentTopic() != null ? t.getParentTopic().getName() : null)
                .orderNumber(t.getOrderNumber())
                .questionCount(directCount)
                .totalQuestions(totalCount)
                .subTopics(List.of())
                .build();
    }

    /** Returns all active topics across all sessions — for admin dropdowns */
    @Transactional(readOnly = true)
    public List<QuestionTopicResponse> getAllTopics() {
        return topicRepository.findAll().stream()
                .filter(t -> t.isActive())
                .map(t -> toResponse(t, 0, 0))
                .collect(java.util.stream.Collectors.toList());
    }

}
