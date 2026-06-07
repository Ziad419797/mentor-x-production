package com.educore.lesson;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * Scheduled job يشتغل كل ساعة لتحديث الحصص المقفلة.
 *
 * المنطق:
 *   - أي Week بـ lockType = ON_DATE ولوحده lockDate <= today
 *     → يُعيّن globallyLocked = true
 *
 * ملاحظة: حصص AFTER_DURATION لا تحتاج job لأن الـ LessonGateService
 * يحسب التاريخ dynamically لكل طالب من StudentWeekAccess.firstAccessAt.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WeekLockScheduler {

    private final LessonRepository lessonRepository;

    @Scheduled(cron = "0 0 * * * *")  // كل ساعة ساعة
    @Transactional
    public void lockExpiredWeeks() {
        LocalDate today = LocalDate.now();

        List<Week> toLock = lessonRepository.findWeeksToGloballyLock(today);
        if (toLock.isEmpty()) {
            return;
        }

        toLock.forEach(w -> {
            w.setGloballyLocked(true);
            log.info("Week globally locked: id={}, title='{}', lockDate={}",
                     w.getId(), w.getTitle(), w.getLockDate());
        });

        lessonRepository.saveAll(toLock);
        log.info("Locked {} week(s) on {}", toLock.size(), today);
    }
}
