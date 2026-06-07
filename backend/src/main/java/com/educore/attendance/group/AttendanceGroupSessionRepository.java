package com.educore.attendance.group;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AttendanceGroupSessionRepository extends JpaRepository<AttendanceGroupSession, Long> {

    List<AttendanceGroupSession> findByGroupIdOrderBySessionDateDesc(Long groupId);

    /** الحصة المفتوحة حالياً في الجروب (لو موجودة) */
    Optional<AttendanceGroupSession> findByGroupIdAndOpenTrue(Long groupId);

    /** التحقق من أن الحصة تنتمي لجروب المدرس */
    @Query("""
        SELECT s FROM AttendanceGroupSession s
        WHERE s.id = :sessionId AND s.group.teacher.id = :teacherId
    """)
    Optional<AttendanceGroupSession> findByIdAndTeacherId(
            @Param("sessionId") Long sessionId,
            @Param("teacherId") Long teacherId);

    /** أحدث رقم حصة في الجروب */
    @Query("SELECT COALESCE(MAX(s.sessionNumber), 0) FROM AttendanceGroupSession s WHERE s.group.id = :groupId")
    int findMaxSessionNumber(@Param("groupId") Long groupId);
}
