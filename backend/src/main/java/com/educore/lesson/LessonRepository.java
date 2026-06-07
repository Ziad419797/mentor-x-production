package com.educore.lesson;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface LessonRepository extends JpaRepository<Week, Long> {
    // ✅ استخدام @Query بدلاً من method naming
    @Query("""
SELECT DISTINCT w 
FROM Week w 
JOIN w.sessions s 
WHERE s.id = :sessionId 
AND w.active = true
""")
    Page<Week> findBySessionId(Long sessionId, Pageable pageable);


    @Query("SELECT w FROM Week w JOIN w.sessions s WHERE s.id = :sessionId AND w.active = true")
    Page<Week> findBySessionIdAndActiveTrue(Long sessionId, Pageable pageable);
    // LessonRepository.java
    @Query(value = "SELECT * FROM weeks WHERE id = :id", nativeQuery = true)
    Optional<Week> findByIdIncludingInactive(@Param("id") Long id);

    /** Used by AccessService — resolves which course a week belongs to via its sessions */
    @Query("SELECT DISTINCT c.id FROM Week w JOIN w.sessions s JOIN s.courses c WHERE w.id = :weekId")
    Long findCourseIdById(@Param("weekId") Long weekId);

    /** Used by LessonGateService — get contentOrder of the course this week belongs to */
    @Query("SELECT DISTINCT c.contentOrder FROM Week w JOIN w.sessions s JOIN s.courses c WHERE w.id = :weekId")
    String findContentOrderByWeekId(@Param("weekId") Long weekId);

    /**
     * يجيب الحصة السابقة مباشرة (أعلى orderNumber أقل من currentOrder)
     * في نفس الـ sessions المرتبطة بالحصة الحالية.
     * مستخدمة في LessonGate لتحقق: "هل الطالب خلّص الحصة السابقة؟"
     */
    @Query("""
        SELECT w FROM Week w
        JOIN w.sessions s
        WHERE s IN (SELECT s2 FROM Week w2 JOIN w2.sessions s2 WHERE w2.id = :weekId)
          AND w.orderNumber < :currentOrder
          AND w.active = true
        ORDER BY w.orderNumber DESC
    """)
    List<Week> findPreviousWeeksInSameSessions(
            @Param("weekId") Long weekId,
            @Param("currentOrder") Integer currentOrder);

    /**
     * هل الحصة دي هي الأولى في أي من الـ sessions المرتبطة بيها؟
     * (لو ما فيش حصة تانية بـ orderNumber أصغر في نفس الـ session)
     */
    @Query("""
        SELECT COUNT(w) = 0 FROM Week w
        JOIN w.sessions s
        WHERE s IN (SELECT s2 FROM Week w2 JOIN w2.sessions s2 WHERE w2.id = :weekId)
          AND w.orderNumber < :currentOrder
          AND w.active = true
    """)
    boolean isFirstInSessions(
            @Param("weekId") Long weekId,
            @Param("currentOrder") Integer currentOrder);

    /**
     * حصص من نوع ON_DATE وتاريخها أصبح <= اليوم ولم تُقفل بعد.
     * تُستدعى من WeekLockScheduler.
     */
    @Query("""
        SELECT w FROM Week w
        WHERE w.lockType = com.educore.lesson.WeekLockType.ON_DATE
          AND w.lockDate <= :today
          AND w.globallyLocked = false
          AND w.active = true
    """)
    List<Week> findWeeksToGloballyLock(@Param("today") LocalDate today);
}
