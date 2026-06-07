package com.educore.quiz;

import com.educore.student.StudentAnswer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StudentAnswerRepository extends JpaRepository<StudentAnswer, Long> {

    List<StudentAnswer> findByAttemptId(Long attemptId);

    void deleteByAttemptId(Long attemptId);

    /**
     * إحصائيات الأخطاء لكل topic من خلال إجابات الكويزات
     * يرجع: [topicId, topicName, totalAnswers, wrongAnswers, wrongPct]
     */
    @Query("""
        SELECT
            t.id,
            t.name,
            COUNT(a.id),
            SUM(CASE WHEN a.selectedAnswer <> a.question.correctAnswer THEN 1 ELSE 0 END),
            ROUND(
                SUM(CASE WHEN a.selectedAnswer <> a.question.correctAnswer THEN 1.0 ELSE 0.0 END)
                / NULLIF(COUNT(a.id), 0) * 100, 1
            )
        FROM StudentAnswer a
        JOIN a.question q
        JOIN q.topic t
        WHERE t.active = true
        GROUP BY t.id, t.name
        ORDER BY 5 DESC
    """)
    List<Object[]> getTopicErrorStats();
}