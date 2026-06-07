package com.educore.question;

import com.educore.question.Question;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface QuestionRepository extends JpaRepository<Question, Long> , JpaSpecificationExecutor<Question> {
    // إضافة هذه الميثود
    @Modifying
    @Query("UPDATE Question q SET q.deleted = true WHERE q.quiz.id = :quizId")  // ✅ Soft Delete
    void deleteByQuizId(@Param("quizId") Long quizId);
    // للاستخدام العادي مع @Where
    Page<Question> findByQuizId(Long quizId, Pageable pageable);



    @Query("""
        SELECT COUNT(DISTINCT q) FROM Question q
        JOIN q.quiz qz JOIN qz.week w JOIN w.sessions s JOIN s.courses c JOIN c.categories cat
        WHERE cat.level.id = :levelId AND q.deleted = false
    """)
    long countByLevelId(@org.springframework.data.repository.query.Param("levelId") Long levelId);

}