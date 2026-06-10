package com.educore.studentactivity;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class StudentActivityLogService {

    private final StudentActivityLogRepository repo;

    // ─────────────────────────────────────────────────────────────
    // Async logging (non-blocking)
    // ─────────────────────────────────────────────────────────────

    @Async
    public void log(Long studentId, String studentName,
                    StudentEventType eventType,
                    String title, String details) {
        log(studentId, studentName, eventType, title, details, null);
    }

    @Async
    public void log(Long studentId, String studentName,
                    StudentEventType eventType,
                    String title, String details, String ipAddress) {
        try {
            repo.save(StudentActivityLog.builder()
                    .studentId(studentId)
                    .studentName(studentName)
                    .eventType(eventType)
                    .title(title)
                    .details(details)
                    .ipAddress(ipAddress)
                    .build());
        } catch (Exception e) {
            log.warn("Failed to save student activity log: {}", e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────
    // Queries
    // ─────────────────────────────────────────────────────────────

    public Page<StudentActivityLog> getByStudent(Long studentId, Pageable pageable) {
        return repo.findByStudentIdOrderByCreatedAtDesc(studentId, pageable);
    }

    public Page<StudentActivityLog> getByStudentAndType(Long studentId,
                                                         StudentEventType eventType,
                                                         Pageable pageable) {
        return repo.findByStudentIdAndEventTypeOrderByCreatedAtDesc(studentId, eventType, pageable);
    }
}
