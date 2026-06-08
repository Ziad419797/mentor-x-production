package com.educore.quiz;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface QuizRepository extends JpaRepository<Quiz, Long> {
    @EntityGraph(attributePaths = {"questions"})
    Optional<Quiz> findWithQuestionsById(Long id);
    @Query("SELECT DISTINCT q FROM Quiz q LEFT JOIN FETCH q.questions WHERE q.deleted = false")
    Page<Quiz> findAllByDeletedFalseWithQuestions(Pageable pageable);
    @EntityGraph(attributePaths = {"questions"})
    Page<Quiz> findByWeekId(Long weekId, Pageable pageable);
    @EntityGraph(attributePaths = {"questions"})
    Optional<Quiz> findWithQuestionsByIdAndDeletedFalse(Long id);


    @Query("""
        SELECT COUNT(DISTINCT q) FROM Quiz q
        JOIN q.week w JOIN w.sessions s JOIN s.courses c JOIN c.categories cat
        WHERE cat.level.id = :levelId AND q.deleted = false
    """)
    long countByLevelId(@org.springframework.data.repository.query.Param("levelId") Long levelId);

    /** كويزات الكورس (عبر week → session → course) — تُحسب ضمن عناصر التقدم (بعدد أسئلتها) */
    @Query("""
        SELECT DISTINCT q FROM Quiz q LEFT JOIN FETCH q.questions
        JOIN q.week w JOIN w.sessions s JOIN s.courses c
        WHERE c.id = :courseId AND q.deleted = false
    """)
    java.util.List<Quiz> findByCourseId(@Param("courseId") Long courseId);

}