package com.educore.lessongate;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StudentLessonProgressRepository
        extends JpaRepository<StudentLessonProgress, Long> {

    Optional<StudentLessonProgress> findByStudentIdAndWeekId(
            Long studentId, Long weekId);

    boolean existsByStudentIdAndWeekId(Long studentId, Long weekId);

    /** كل تقدم طالب مرتبة بـ orderNumber عشان نعرف الحصة التالية */
    @Query("""
        SELECT p FROM StudentLessonProgress p
        WHERE p.student.id = :studentId
        ORDER BY p.week.orderNumber ASC
    """)
    List<StudentLessonProgress> findByStudentIdOrdered(@Param("studentId") Long studentId);

    /** الحصص اللي الطالب خلصها */
    List<StudentLessonProgress> findByStudentIdAndStatus(
            Long studentId, LessonProgressStatus status);

    /** عدد الحصص المكتملة للطالب */
    long countByStudentIdAndStatus(Long studentId, LessonProgressStatus status);

    /** الحصص المرتبطة بـ Session معين للطالب */
    @Query("""
        SELECT p FROM StudentLessonProgress p
        JOIN p.week w
        JOIN w.sessions s
        WHERE p.student.id = :studentId
          AND s.id = :sessionId
        ORDER BY w.orderNumber ASC
    """)
    List<StudentLessonProgress> findByStudentAndSession(
            @Param("studentId") Long studentId,
            @Param("sessionId") Long sessionId);

    /** الحصة التالية المقفولة بعد حصة معينة في نفس الـ Session */
    @Query("""
        SELECT p FROM StudentLessonProgress p
        JOIN p.week w
        JOIN w.sessions s
        WHERE p.student.id = :studentId
          AND s.id = :sessionId
          AND w.orderNumber > :currentOrder
          AND p.status = 'LOCKED'
        ORDER BY w.orderNumber ASC
    """)
    List<StudentLessonProgress> findNextLockedInSession(
            @Param("studentId") Long studentId,
            @Param("sessionId") Long sessionId,
            @Param("currentOrder") Integer currentOrder);

    /**
     * هل اجتاز الطالب الحصة اللي قبل الحصة المطلوبة مباشرةً؟
     *
     * المنطق: ابحث عن progress بحالة COMPLETED لأي حصة تشارك نفس الـ Session
     * مع الحصة المستهدفة وترتيبها = (targetOrder - 1).
     */
    @Query("""
        SELECT CASE WHEN COUNT(p) > 0 THEN true ELSE false END
        FROM StudentLessonProgress p
        JOIN p.week prevWeek
        JOIN prevWeek.sessions s
        JOIN s.weeks targetWeek
        WHERE p.student.id    = :studentId
          AND p.status        = 'COMPLETED'
          AND prevWeek.orderNumber = :prevOrder
          AND targetWeek.id   = :targetWeekId
    """)
    boolean isPreviousLessonCompleted(
            @Param("studentId")    Long studentId,
            @Param("targetWeekId") Long targetWeekId,
            @Param("prevOrder")    int  prevOrder);
}
