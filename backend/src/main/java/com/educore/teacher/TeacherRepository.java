package com.educore.teacher;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TeacherRepository extends JpaRepository<Teacher, Long> {

    Optional<Teacher> findByPhone(String phone);

    Optional<Teacher> findFirstByEnabledTrue();

    boolean existsByPhone(String phone);

    boolean existsByEmail(String email);
}
