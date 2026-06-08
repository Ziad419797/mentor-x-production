package com.educore.assignment;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface AssignmentRepository extends JpaRepository<Assignment, Long> {

    @EntityGraph(attributePaths = {"questions"})
    Optional<Assignment> findWithQuestionsById(Long id);

    @EntityGraph(attributePaths = {"questions"})
    Page<Assignment> findByWeekId(Long weekId, Pageable pageable);

    @EntityGraph(attributePaths = {"questions"})
    Optional<Assignment> findWithQuestionsByIdAndDeletedFalse(Long id);

    /** واجبات الكورس (عبر week → session → course) — تُحسب ضمن عناصر التقدم (بعدد أسئلتها) */
    @org.springframework.data.jpa.repository.Query("""
        SELECT DISTINCT a FROM Assignment a LEFT JOIN FETCH a.questions
        JOIN a.week w JOIN w.sessions s JOIN s.courses c
        WHERE c.id = :courseId AND a.deleted = false
    """)
    java.util.List<Assignment> findByCourseId(@org.springframework.data.repository.query.Param("courseId") Long courseId);
}