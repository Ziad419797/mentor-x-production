package com.educore.staff;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StaffRepository extends JpaRepository<Staff, Long> {

    Optional<Staff> findByPhone(String phone);

    Optional<Staff> findByPhoneAndActiveTrue(String phone);

    List<Staff> findByTeacherIdOrderByCreatedAtDesc(Long teacherId);

    List<Staff> findByTeacherIdAndActiveTrueOrderByCreatedAtDesc(Long teacherId);

    boolean existsByPhone(String phone);

    boolean existsByPhoneAndIdNot(String phone, Long id);

    @Query("SELECT s FROM Staff s WHERE s.teacherId = :teacherId AND s.active = true AND :permission MEMBER OF s.permissions")
    List<Staff> findActiveByTeacherIdAndPermission(@Param("teacherId") Long teacherId,
                                                   @Param("permission") StaffPermission permission);

    long countByTeacherIdAndActiveTrue(Long teacherId);
}
