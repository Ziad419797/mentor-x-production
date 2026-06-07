package com.educore.activitylog;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ActivityLogService {

    private final ActivityLogRepository repo;

    /** تسجيل نشاط بشكل غير متزامن حتى لا يؤثر على أداء الـ request */
    @Async
    public void log(String actorName, String actorUsername,
                    String action, String entityType,
                    String entityId, String details,
                    String ipAddress) {
        try {
            ActivityLog entry = ActivityLog.builder()
                    .actorName(actorName)
                    .actorUsername(actorUsername)
                    .action(action)
                    .entityType(entityType)
                    .entityId(entityId)
                    .details(details)
                    .ipAddress(ipAddress)
                    .build();
            repo.save(entry);
        } catch (Exception e) {
            log.warn("Failed to save activity log: {}", e.getMessage());
        }
    }

    /** اختصار بدون IP */
    @Async
    public void log(String actorName, String actorUsername,
                    String action, String entityType,
                    String entityId, String details) {
        log(actorName, actorUsername, action, entityType, entityId, details, null);
    }

    public Page<ActivityLog> getAll(Pageable pageable) {
        return repo.findAllByOrderByCreatedAtDesc(pageable);
    }

    public Page<ActivityLog> getByActor(String actorUsername, Pageable pageable) {
        return repo.findByActorUsernameOrderByCreatedAtDesc(actorUsername, pageable);
    }
}
