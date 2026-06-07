package com.educore.questionbank;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BankQuestionRepository extends JpaRepository<BankQuestion, Long> {

    /** كل أسئلة جزئية معينة */
    List<BankQuestion> findByTopicIdAndActiveTrue(Long topicId);

    /** أسئلة جزئية بمستوى صعوبة معين */
    @Query("""
        SELECT q FROM BankQuestion q
        WHERE q.topic.id = :topicId
          AND q.active = true
          AND (:difficulty = 'ALL' OR q.difficulty = :difficulty)
        ORDER BY q.id
    """)
    List<BankQuestion> findByTopicAndDifficulty(
            @Param("topicId") Long topicId,
            @Param("difficulty") String difficulty);

    /** كل أسئلة درس معين (عبر الـ topic) */
    Page<BankQuestion> findByWeekIdAndActiveTrue(Long weekId, Pageable pageable);

    /** كل أسئلة درس معين (list) مع filter اختياري */
    @Query("""
        SELECT q FROM BankQuestion q
        WHERE q.week.id = :weekId
          AND q.active = true
          AND (:difficulty = 'ALL' OR q.difficulty = :difficulty)
        ORDER BY q.topic.orderNumber ASC NULLS LAST, q.id ASC
    """)
    List<BankQuestion> findByWeekAndDifficulty(
            @Param("weekId") Long weekId,
            @Param("difficulty") String difficulty);

    /** أسئلة الدرس في جزئيات محددة بالـ IDs */
    @Query("""
        SELECT q FROM BankQuestion q
        WHERE q.topic.id IN :topicIds
          AND q.active = true
          AND (:difficulty = 'ALL' OR q.difficulty = :difficulty)
        ORDER BY q.topic.id, q.id
    """)
    List<BankQuestion> findByTopicIdsAndDifficulty(
            @Param("topicIds") List<Long> topicIds,
            @Param("difficulty") String difficulty);

    /** عدد الأسئلة النشطة بـ conceptTag معين في جزئية معينة */
    long countByTopicIdAndConceptTagAndActiveTrue(Long topicId, String conceptTag);

    /** كل الـ concept tags الموجودة في الدرس */
    @Query("""
        SELECT DISTINCT q.conceptTag FROM BankQuestion q
        WHERE q.week.id = :weekId
          AND q.conceptTag IS NOT NULL
          AND q.active = true
    """)
    List<String> findDistinctConceptTagsByWeek(@Param("weekId") Long weekId);

    /** إحصائيات: عدد الأسئلة بكل مستوى لكل درس */
    @Query("""
        SELECT q.difficulty, COUNT(q)
        FROM BankQuestion q
        WHERE q.week.id = :weekId AND q.active = true
        GROUP BY q.difficulty
    """)
    List<Object[]> countByDifficultyForWeek(@Param("weekId") Long weekId);
}
