package com.educore.unit;

import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Primary
@Repository
public interface SessionRepository extends JpaRepository<Session, Long> {

    Page<Session> findByCoursesId(Long courseId, Pageable pageable);

    boolean existsByTitle(String title);
    // 👈 لازم تضيفي دي عشان الـ Toggle يشتغل
    @Query(value = "SELECT * FROM sessions WHERE id = :id", nativeQuery = true)
    Optional<Session> findByIdIncludingInactive(@Param("id") Long id);

    /** Used by AccessService — resolves which course a session belongs to */
    @Query("SELECT c.id FROM Session s JOIN s.courses c WHERE s.id = :sessionId")
    Long findCourseIdById(@Param("sessionId") Long sessionId);

    /** أول Session مرتبطة بكورس معين */
    @Query("SELECT s FROM Session s JOIN s.courses c WHERE c.id = :courseId AND s.active = true ORDER BY s.id ASC")
    java.util.List<Session> findByCourseIdActive(@Param("courseId") Long courseId);

    /** محاضرات الصف الدراسي */
    @Query("SELECT DISTINCT s FROM Session s JOIN s.courses c JOIN c.categories cat WHERE cat.level.id = :levelId ORDER BY s.id DESC")
    java.util.List<Session> findByLevelId(@Param("levelId") Long levelId);

    @Query("SELECT COUNT(DISTINCT s) FROM Session s JOIN s.courses c JOIN c.categories cat WHERE cat.level.id = :levelId")
    long countByLevelId(@Param("levelId") Long levelId);
}
