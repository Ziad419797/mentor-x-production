package com.educore.attendance;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AttendanceRepository extends JpaRepository<AttendanceRecord, Long> {

    /** هل الطالب حضر الحصة دي قبل كده؟ */
    boolean existsByStudentIdAndWeekId(Long studentId, Long weekId);

    /** الحضور الأخير للطالب في حصة معينة */
    Optional<AttendanceRecord> findTopByStudentIdAndWeekIdOrderByAttendedAtDesc(
            Long studentId, Long weekId);

    /** كل حضور طالب معين (للـ dashboard) */
    Page<AttendanceRecord> findByStudentIdOrderByAttendedAtDesc(Long studentId, Pageable pageable);

    /** كل حضور حصة معينة (للمدرس) */
    Page<AttendanceRecord> findByWeekIdOrderByAttendedAtDesc(Long weekId, Pageable pageable);

    /** عدد حضور طالب */
    long countByStudentId(Long studentId);

    /** عدد حضور طالب نوع معين (CENTER / ONLINE) */
    long countByStudentIdAndType(Long studentId, AttendanceType type);

    /** سجل الحضور في فترة زمنية — للتقارير */
    @Query("""
        SELECT a FROM AttendanceRecord a
        WHERE a.student.id = :studentId
          AND a.attendedAt BETWEEN :from AND :to
        ORDER BY a.attendedAt DESC
    """)
    List<AttendanceRecord> findByStudentInRange(
            @Param("studentId") Long studentId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);

    /** كل حضور حصة في يوم معين */
    @Query("""
        SELECT a FROM AttendanceRecord a
        WHERE a.week.id = :weekId
          AND a.attendedAt >= :dayStart
          AND a.attendedAt < :dayEnd
        ORDER BY a.attendedAt
    """)
    List<AttendanceRecord> findByWeekAndDay(
            @Param("weekId") Long weekId,
            @Param("dayStart") LocalDateTime dayStart,
            @Param("dayEnd") LocalDateTime dayEnd);

    /** إحصائيات حضور لمجموعة من الطلاب (dashboard المدرس) */
    @Query("""
        SELECT a.student.id, COUNT(a)
        FROM AttendanceRecord a
        WHERE a.student.id IN :studentIds
        GROUP BY a.student.id
    """)
    List<Object[]> countAttendanceForStudents(@Param("studentIds") List<Long> studentIds);
}
