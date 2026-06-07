package com.educore.questionbank;

import com.educore.exception.ResourceNotFoundException;
import com.educore.lesson.LessonRepository;
import com.educore.lesson.Week;
import com.educore.question.Question;
import com.educore.questionbank.dto.GenerateExamRequest;
import com.educore.questionbank.dto.GeneratedExamSummary;
import com.educore.quiz.Quiz;
import com.educore.quiz.QuizRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Exam Generator — قلب نظام بنك الأسئلة.
 *
 * الخوارزمية:
 *   لكل جزئية (topic) → يجمع الأسئلة في مجموعات بالـ conceptTag
 *   من كل مجموعة → يختار N سؤال عشوائي (default 1)
 *   النتيجة → تنوع كامل: الطالب ما يشوفش فكرتين متكررتين من نفس الجزئية
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExamGeneratorService {

    private final LessonRepository       lessonRepository;
    private final QuestionTopicRepository topicRepository;
    private final BankQuestionRepository  bankRepository;
    private final QuizRepository          quizRepository;

    // ─── توليد الامتحان ───────────────────────────────────────────

    @Transactional
    public GeneratedExamSummary generate(GenerateExamRequest request) {

        // 1. الدرس
        Week week = lessonRepository.findById(request.getWeekId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "الدرس غير موجود: " + request.getWeekId()));

        // 2. الجزئيات المستهدفة (محددة أو كلها)
        List<Long> topicIds = resolveTopicIds(request);
        if (topicIds.isEmpty()) {
            throw new IllegalArgumentException("لا توجد جزئيات نشطة لهذا الدرس");
        }

        // 3. جلب الأسئلة من البنك مع فلتر الصعوبة
        String diff = request.getDifficulty() != null
                ? request.getDifficulty().name() : "ALL";
        List<BankQuestion> pool = bankRepository.findByTopicIdsAndDifficulty(topicIds, diff);

        if (pool.isEmpty()) {
            throw new IllegalArgumentException(
                    "لا توجد أسئلة في البنك للجزئيات المختارة بالمستوى المحدد");
        }

        // 4. اختيار الأسئلة: سؤال واحد (أو N) من كل variantGroup في كل جزئية
        List<BankQuestion> selected = pickQuestions(pool, request.getQuestionsPerConceptGroup());

        // 5. خلط ترتيب الأسئلة بين الطلبة
        if (request.isShuffleQuestions()) {
            Collections.shuffle(selected);
        }

        // 6. إنشاء الـ Quiz
        Quiz quiz = Quiz.builder()
                .title(request.getQuizTitle())
                .week(week)
                .durationMinutes(request.getDurationMinutes())
                .timeRestricted(request.isTimeRestricted())
                .active(true)
                .questions(new HashSet<>())
                .build();

        // 7. تحويل كل BankQuestion إلى Question مرتبط بالكويز
        int totalMarks = 0;
        for (BankQuestion bq : selected) {
            List<String> opts = new ArrayList<>(bq.getOptions());
            if (request.isShuffleOptions()) {
                // آمن — correctAnswer متحفظ كـ text مش index
                Collections.shuffle(opts);
            }

            Question q = Question.builder()
                    .imageUrl(bq.getImageUrl() != null ? bq.getImageUrl() : "")
                    .description(bq.getDescription())
                    .mark(bq.getMark())
                    .options(opts)
                    .correctAnswer(bq.getCorrectAnswer())
                    .quiz(quiz)
                    .deleted(false)
                    .build();

            quiz.getQuestions().add(q);
            totalMarks += bq.getMark();
        }

        // 8. حفظ — CascadeType.ALL بيحفظ الأسئلة تلقائياً
        Quiz saved = quizRepository.save(quiz);
        log.info("Generated exam: quizId={}, questions={}, totalMarks={}, week={}",
                saved.getId(), selected.size(), totalMarks, week.getId());

        // 9. ملخص الجزئيات المستخدمة
        List<GeneratedExamSummary.TopicSummary> topicSummaries =
                buildTopicSummaries(selected, pool);

        return GeneratedExamSummary.builder()
                .quizId(saved.getId())
                .quizTitle(saved.getTitle())
                .weekId(week.getId())
                .weekTitle(week.getTitle())
                .questionCount(selected.size())
                .totalMarks(totalMarks)
                .durationMinutes(request.getDurationMinutes())
                .shuffled(request.isShuffleQuestions())
                .topicsCovered(topicSummaries)
                .build();
    }

    // ─── Helpers ─────────────────────────────────────────────────

    /** يحل قائمة الـ topic IDs — إما المحددة أو كل جزئيات الدرس */
    private List<Long> resolveTopicIds(GenerateExamRequest request) {
        if (request.getTopicIds() != null && !request.getTopicIds().isEmpty()) {
            return request.getTopicIds();
        }
        // الجزئيات بقت مرتبطة بالمحاضرة (Session) مش بالدرس (Week) مباشرة،
        // فبنجمع جزئيات كل المحاضرات المرتبطة بالدرس ده
        Week w = lessonRepository.findById(request.getWeekId()).orElse(null);
        if (w == null || w.getSessions() == null) return List.of();
        return w.getSessions().stream()
                .flatMap(s -> topicRepository.findAllBySession(s.getId()).stream())
                .map(QuestionTopic::getId)
                .distinct()
                .toList();
    }

    /**
     * الخوارزمية الأساسية:
     *   يجمع الأسئلة بـ (topicId → variantGroup)
     *   من كل مجموعة يختار N عشوائي (default 1)
     *   → ضمان: ما في سؤالين بنفس الفكرة من نفس الجزئية
     */
    private List<BankQuestion> pickQuestions(List<BankQuestion> pool, int perGroup) {
        // topic → (variantGroup → [variants])
        Map<Long, Map<String, List<BankQuestion>>> byTopicAndGroup = pool.stream()
                .collect(Collectors.groupingBy(
                        bq -> bq.getTopic().getId(),
                        Collectors.groupingBy(BankQuestion::getVariantGroup)
                ));

        List<BankQuestion> selected = new ArrayList<>();
        Random rng = new Random();

        for (Map<String, List<BankQuestion>> groups : byTopicAndGroup.values()) {
            for (List<BankQuestion> variants : groups.values()) {
                // خلط النسخ عشان الاختيار يكون عشوائي بين الطلبة
                List<BankQuestion> shuffled = new ArrayList<>(variants);
                Collections.shuffle(shuffled, rng);
                int take = Math.min(perGroup, shuffled.size());
                selected.addAll(shuffled.subList(0, take));
            }
        }

        return selected;
    }

    /** يبني ملخص الجزئيات المستخدمة في الامتحان */
    private List<GeneratedExamSummary.TopicSummary> buildTopicSummaries(
            List<BankQuestion> selected,
            List<BankQuestion> pool) {

        // عدد الـ concept groups المتاحة في الـ pool لكل topic
        Map<Long, Long> totalGroupsByTopic = pool.stream()
                .collect(Collectors.groupingBy(
                        bq -> bq.getTopic().getId(),
                        Collectors.collectingAndThen(
                                Collectors.toList(),
                                list -> list.stream()
                                        .map(BankQuestion::getVariantGroup)
                                        .distinct()
                                        .count()
                        )
                ));

        // تجميع المختارة بالـ topic
        Map<Long, List<BankQuestion>> selectedByTopic = selected.stream()
                .collect(Collectors.groupingBy(bq -> bq.getTopic().getId()));

        return selectedByTopic.entrySet().stream()
                .map(e -> {
                    Long topicId     = e.getKey();
                    List<BankQuestion> qs = e.getValue();
                    String topicName = qs.get(0).getTopic().getName();
                    int totalConcepts = totalGroupsByTopic
                            .getOrDefault(topicId, 0L).intValue();

                    return GeneratedExamSummary.TopicSummary.builder()
                            .topicId(topicId)
                            .topicName(topicName)
                            .questionsSelected(qs.size())
                            .totalConcepts(totalConcepts)
                            .build();
                })
                .sorted(Comparator.comparing(GeneratedExamSummary.TopicSummary::getTopicId))
                .toList();
    }
}
