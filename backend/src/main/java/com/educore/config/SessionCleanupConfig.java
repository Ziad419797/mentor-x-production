package com.educore.config;

import com.educore.session.DatabaseSessionService;
import com.educore.student.StudentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Configuration
@EnableScheduling
@RequiredArgsConstructor
public class SessionCleanupConfig {

    private final DatabaseSessionService sessionService;
    private final StudentRepository studentRepository;

    /**
     * تنظيف الجلسات المنتهية كل ساعة
     */
    @Scheduled(cron = "0 0 */1 * * *") // كل ساعة
    @Transactional
    public void cleanupExpiredSessions() {
        log.info("Starting scheduled session cleanup...");
        sessionService.cleanupExpiredSessions();
        log.info("Scheduled session cleanup completed");
    }

    /**
     * إغلاق الجلسات المنتهية للطلاب تلقائياً
     * يستخدم bulk UPDATE بدلاً من findAll().forEach لتجنب OOM مع آلاف الطلاب
     */
    @Scheduled(fixedRate = 300000) // كل 5 دقائق
    @Transactional
    public void autoLogoutExpiredStudentSessions() {
        log.debug("Checking for expired student sessions...");
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(30);
        int updated = studentRepository.clearExpiredSessions(cutoff);
        if (updated > 0) {
            log.info("Auto-logout: cleared {} expired student sessions", updated);
        }
    }
}