package com.educore.attendance.group;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AttendanceGroupRecordRepository extends JpaRepository<AttendanceGroupRecord, Long> {

    List<AttendanceGroupRecord> findBySessionIdOrderByScannedAtAsc(Long sessionId);

    Optional<AttendanceGroupRecord> findBySessionIdAndStudentId(Long sessionId, Long studentId);

    boolean existsBySessionIdAndStudentId(Long sessionId, Long studentId);

    long countBySessionIdAndStatus(Long sessionId, AttendanceStatus status);

    /** كل سجلات طالب في جروب معين (مرتبة بالتاريخ) */
    @Query("""
        SELECT r FROM AttendanceGroupRecord r
        WHERE r.student.id = :studentId
          AND r.session.group.id = :groupId
        ORDER BY r.session.sessionDate DESC
    """)
    List<AttendanceGroupRecord> findByStudentAndGroup(
            @Param("studentId") Long studentId,
            @Param("groupId") Long groupId);

    /** آخر تعليق كتبه المدرس لطالب في الجروب */
    @Query("""
        SELECT r FROM AttendanceGroupRecord r
        WHERE r.student.id = :studentId
          AND r.session.group.id = :groupId
          AND r.teacherComment IS NOT NULL
        ORDER BY r.session.sessionDate DESC
    """)
    List<AttendanceGroupRecord> findCommentsForStudentInGroup(
            @Param("studentId") Long studentId,
            @Param("groupId") Long groupId);

    /** عدد حصص الحضور والغياب للطالب في الجروب */
    @Query("""
        SELECT r.status, COUNT(r)
        FROM AttendanceGroupRecord r
        WHERE r.student.id = :studentId
          AND r.session.group.id = :groupId
        GROUP BY r.status
    """)
    List<Object[]> countByStatusForStudentInGroup(
            @Param("studentId") Long studentId,
            @Param("groupId") Long groupId);

    /** IDs الطلاب اللي اتسجلوا في الحصة */
    @Query("SELECT r.student.id FROM AttendanceGroupRecord r WHERE r.session.id = :sessionId")
    List<Long> findMarkedStudentIds(@Param("sessionId") Long sessionId);
}
