package com.educore.studentactivity;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StudentActivityLogRepository extends JpaRepository<StudentActivityLog, Long> {

    Page<StudentActivityLog> findByStudentIdOrderByCreatedAtDesc(Long studentId, Pageable pageable);

    Page<StudentActivityLog> findByStudentIdAndEventTypeOrderByCreatedAtDesc(
            Long studentId, StudentEventType eventType, Pageable pageable);
}
