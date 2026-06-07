package com.educore.questionbank;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuestionTopicRepository extends JpaRepository<QuestionTopic, Long> {

    /** كل الجزئيات الرئيسية لمحاضرة معينة (بدون أب) مرتبة */
    @Query("""
        SELECT t FROM QuestionTopic t
        WHERE t.session.id = :sessionId
          AND t.parentTopic IS NULL
          AND t.active = true
        ORDER BY t.orderNumber ASC NULLS LAST, t.id ASC
    """)
    List<QuestionTopic> findRootTopicsBySession(@Param("sessionId") Long sessionId);

    /** كل الجزئيات لمحاضرة معينة (على أي مستوى) */
    @Query("""
        SELECT t FROM QuestionTopic t
        WHERE t.session.id = :sessionId
          AND t.active = true
        ORDER BY t.orderNumber ASC NULLS LAST, t.id ASC
    """)
    List<QuestionTopic> findAllBySession(@Param("sessionId") Long sessionId);

    /** الجزئيات الفرعية لجزئية معينة */
    List<QuestionTopic> findByParentTopicIdAndActiveTrue(Long parentTopicId);

    /** هل الجزئية عندها أسئلة في البنك؟ */
    @Query("SELECT COUNT(q) > 0 FROM BankQuestion q WHERE q.topic.id = :topicId AND q.active = true")
    boolean hasActiveQuestions(@Param("topicId") Long topicId);

    /** عدد الأسئلة في الجزئية ومافيهاش */
    @Query("""
        SELECT t.id, COUNT(q)
        FROM QuestionTopic t
        LEFT JOIN BankQuestion q ON q.topic = t AND q.active = true
        WHERE t.session.id = :sessionId
        GROUP BY t.id
    """)
    List<Object[]> countQuestionsByTopic(@Param("sessionId") Long sessionId);
}
