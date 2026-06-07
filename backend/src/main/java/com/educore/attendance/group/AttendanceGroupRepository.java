package com.educore.attendance.group;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AttendanceGroupRepository extends JpaRepository<AttendanceGroup, Long> {

    List<AttendanceGroup> findByTeacherIdAndActiveTrue(Long teacherId);

    Optional<AttendanceGroup> findByIdAndTeacherIdAndActiveTrue(Long id, Long teacherId);

    @Query("""
        SELECT g FROM AttendanceGroup g
        WHERE g.teacher.id = :teacherId AND g.active = true
        ORDER BY g.createdAt DESC
    """)
    List<AttendanceGroup> findActiveByTeacher(@Param("teacherId") Long teacherId);

    @Query("""
        SELECT g FROM AttendanceGroup g
        WHERE g.teacher.id = :teacherId AND g.levelId = :levelId AND g.active = true
        ORDER BY g.createdAt DESC
    """)
    List<AttendanceGroup> findActiveByTeacherAndLevel(@Param("teacherId") Long teacherId, @Param("levelId") Long levelId);

    @Query("""
        SELECT g FROM AttendanceGroup g
        WHERE g.center.id = :centerId AND g.levelId = :levelId AND g.active = true
        ORDER BY g.createdAt DESC
    """)
    List<AttendanceGroup> findPublicByCenterAndLevel(@Param("centerId") Long centerId, @Param("levelId") Long levelId);

    @Query("""
        SELECT g FROM AttendanceGroup g
        WHERE g.levelId = :levelId AND g.active = true AND g.center IS NOT NULL
        ORDER BY g.center.name ASC
    """)
    List<AttendanceGroup> findPublicByLevel(@Param("levelId") Long levelId);

    @Query("SELECT COUNT(m) FROM AttendanceGroupMember m WHERE m.group.id = :groupId AND m.active = true")
    long countActiveMembers(@Param("groupId") Long groupId);
}