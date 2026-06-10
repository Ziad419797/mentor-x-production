package com.educore.activitylog;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ActivityLogRepository extends JpaRepository<ActivityLog, Long> {

    Page<ActivityLog> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Page<ActivityLog> findByActorUsernameOrderByCreatedAtDesc(String actorUsername, Pageable pageable);

    Page<ActivityLog> findByEntityTypeOrderByCreatedAtDesc(String entityType, Pageable pageable);

    @org.springframework.data.jpa.repository.Query("SELECT l FROM ActivityLog l WHERE " +
        "(:actor IS NULL OR l.actorUsername LIKE %:actor% OR l.actorName LIKE %:actor%) AND " +
        "(:action IS NULL OR l.action LIKE %:action%) AND " +
        "(:from IS NULL OR l.createdAt >= :from) AND " +
        "(:to IS NULL OR l.createdAt <= :to)")
    Page<ActivityLog> search(
        @org.springframework.data.repository.query.Param("actor") String actor,
        @org.springframework.data.repository.query.Param("action") String action,
        @org.springframework.data.repository.query.Param("from") java.time.LocalDateTime from,
        @org.springframework.data.repository.query.Param("to") java.time.LocalDateTime to,
        Pageable pageable);
}
