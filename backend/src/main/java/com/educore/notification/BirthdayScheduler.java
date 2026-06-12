package com.educore.notification;

import com.educore.student.Student;
import com.educore.student.StudentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

/**
 * Scheduler يشتغل كل يوم الساعة 8 الصبح
 * ويبعت إشعار تهنئة لكل طالب عيد ميلاده النهارده.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BirthdayScheduler {

    private final StudentRepository  studentRepository;
    private final NotificationService notificationService;

    /** كل يوم الساعة 8:00 صباحاً */
    @Scheduled(cron = "0 0 8 * * *")
    public void sendBirthdayNotifications() {
        LocalDate today = LocalDate.now();
        List<Student> students = studentRepository.findTodayBirthdays(today);

        if (students.isEmpty()) {
            log.info("BirthdayScheduler: no birthdays today ({})", today);
            return;
        }

        log.info("BirthdayScheduler: {} birthday(s) today ({})", students.size(), today);

        for (Student student : students) {
            try {
                int age = today.getYear() - student.getDateOfBirth().getYear();
                String name = student.getFirstName() != null ? student.getFirstName() : "الطالب";

                notificationService.send(
                        student.getId(),
                        "STUDENT",
                        "🎂 عيد ميلاد سعيد يا " + name + "!",
                        "كل سنة وأنت طيب 🎉 نتمنالك عام مليء بالنجاح والتفوق. عمرك " + age + " سنة اليوم!",
                        NotificationType.BIRTHDAY,
                        null,
                        null,
                        student.getId()
                );

                log.info("BirthdayScheduler: sent to studentId={} name={} age={}", student.getId(), name, age);
            } catch (Exception e) {
                log.error("BirthdayScheduler: failed for studentId={}: {}", student.getId(), e.getMessage());
            }
        }
    }
}
