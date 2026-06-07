package com.educore.attendance.group;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AttendanceGroupMemberRepository extends JpaRepository<AttendanceGroupMember, Long> {

    List<AttendanceGroupMember> findByGroupIdAndActiveTrue(Long groupId);

    Optional<AttendanceGroupMember> findByGroupIdAndStudentIdAndActiveTrue(Long groupId, Long studentId);

    boolean existsByGroupIdAndStudentIdAndActiveTrue(Long groupId, Long studentId);

    /** كل الجروبات اللي الطالب عضو فيها */
    @Query("SELECT m FROM AttendanceGroupMember m WHERE m.student.id = :studentId AND m.active = true")
    List<AttendanceGroupMember> findActiveByStudent(@Param("studentId") Long studentId);

    /** أول جروب نشط للطالب (للعرض في StudentResponse) */
    Optional<AttendanceGroupMember> findFirstByStudentIdAndActiveTrue(Long studentId);

    /** كل عضويات الطالب النشطة (لإلغائها عند تغيير الجروب) */
    List<AttendanceGroupMember> findByStudentIdAndActiveTrue(Long studentId);

    /** IDs الطلاب النشطين في الجروب */
    @Query("SELECT m.student.id FROM AttendanceGroupMember m WHERE m.group.id = :groupId AND m.active = true")
    List<Long> findActiveStudentIds(@Param("groupId") Long groupId);
}
