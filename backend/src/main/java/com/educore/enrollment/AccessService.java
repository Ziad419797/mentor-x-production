package com.educore.enrollment;

import com.educore.lesson.LessonRepository;
import com.educore.lessonmaterial.LessonMaterialRepository;
import com.educore.unit.SessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Slf4j
@Service("accessService")
@RequiredArgsConstructor
public class AccessService {

    private final EnrollmentRepository enrollmentRepository;
    private final LessonMaterialRepository materialRepository;
    private final SessionRepository sessionRepository;
    private final LessonRepository weekRepository;
    /**
     * التحقق من وصول الطالب لكورس معين
     */
//    @Cacheable(value = "courseAccess", key = "#studentId + '-' + #courseId")
//    public boolean canAccessCourse(Long studentId, Long courseId) {
//        log.debug("Checking access for student {} to course {}", studentId, courseId);
//
//        // 1. هل اشترى الكورس ده منفرد؟
//        boolean boughtIndividually = enrollmentRepository
//                .existsByStudentIdAndCourseIdAndEnrollmentType(
//                        studentId, courseId, EnrollmentType.COURSE_PURCHASE
//                );
//
//        if (boughtIndividually) {
//            log.debug("Student {} has individual purchase for course {}", studentId, courseId);
//            return true;
//        }
//
//        // 2. هل اشترى Category فيها الكورس ده؟
//        boolean fromCategory = enrollmentRepository
//                .existsByStudentIdAndCourseIdAndEnrollmentType(
//                        studentId, courseId, EnrollmentType.CATEGORY_PURCHASE
//                );
//
//        if (fromCategory) {
//            log.debug("Student {} has category purchase for course {}", studentId, courseId);
//            return true;
//        }
//
//        log.debug("Student {} has NO access to course {}", studentId, courseId);
//        return false;
//    }
//
//    /**
//     * جلب كل الكورسات المتاحة للطالب
//     */
//    @Cacheable(value = "accessibleCourses", key = "#studentId")
//    public List<Course> getAllAccessibleCourses(Long studentId) {
//        log.debug("Getting all accessible courses for student {}", studentId);
//
//        List<Enrollment> enrollments = enrollmentRepository
//                .findByStudentIdAndActiveTrue(studentId);
//
//        return enrollments.stream()
//                .map(Enrollment::getCourse)
//                .distinct()
//                .collect(Collectors.toList());
//    }
//
//    /**
//     * جلب كل الـ Enrollments النشطة للطالب
//     */
//    public List<Enrollment> getActiveEnrollments(Long studentId) {
//        return enrollmentRepository.findByStudentIdAndActiveTrue(studentId);
//    }
//
//    /**
//     * جلب كل الكورسات المتاحة من اشتراكات Category
//     */
//    public List<Course> getCoursesFromCategoryPurchases(Long studentId) {
//        List<Enrollment> categoryEnrollments = enrollmentRepository
//                .findByStudentIdAndEnrollmentType(studentId, EnrollmentType.CATEGORY_PURCHASE);
//
//        return categoryEnrollments.stream()
//                .map(Enrollment::getCourse)
//                .distinct()
//                .collect(Collectors.toList());
//    }
//
//    /**
//     * جلب كل الكورسات المتاحة من اشتراكات فردية
//     */
//    public List<Course> getCoursesFromIndividualPurchases(Long studentId) {
//        List<Enrollment> individualEnrollments = enrollmentRepository
//                .findByStudentIdAndEnrollmentType(studentId, EnrollmentType.COURSE_PURCHASE);
//
//        return individualEnrollments.stream()
//                .map(Enrollment::getCourse)
//                .distinct()
//                .collect(Collectors.toList());
//    }
//
//    /**
//     * التحقق من وصول الطالب لدرس (Week) معين
//     */
//    public boolean canAccessWeek(Long studentId, Long weekId) {
//        // جيب الكورس بتاع الـ week
//        // ثم تحقق من الوصول للكورس
//        // Week -> Session -> Course
//        return true; // هتكتب المنطق كامل
//    }
//
//    /**
//     * التحقق من وصول الطالب لمادة تعليمية (LessonMaterial)
//     */
//    public boolean canAccessMaterial(Long studentId, Long materialId) {
//        // جيب الكورس بتاع المادة
//        // ثم تحقق من الوصول للكورس
//        return true; // هتكتب المنطق كامل
//    }
//
//    /**
//     * إخلاء كاش الصلاحيات لطالب (بعد تغيير الاشتراك)
//     */
//    public void evictAccessCache(Long studentId) {
//        log.debug("Evicting access cache for student {}", studentId);
//        // هتستخدم CacheManager عشان تمسح الكاش
//    }


    // ✅ CORE: التحقق من الكورس
    @Cacheable(value = "courseAccess", key = "#studentId + '-' + #courseId")
    public boolean canAccessCourse(Long studentId, Long courseId) {
        return enrollmentRepository.hasActiveEnrollment(studentId, courseId);
    }

    // ✅ التحقق من المادة
    @Cacheable(value = "materialAccess", key = "#studentId + '-' + #materialId")
    public boolean canAccessMaterial(Long studentId, Long materialId) {
        Long courseId = materialRepository.findCourseIdById(materialId);
        if (courseId == null) return false;
        return canAccessCourse(studentId, courseId);
    }

    // ✅ التحقق من الدرس
    @Cacheable(value = "weekAccess", key = "#studentId + '-' + #weekId")
    public boolean canAccessWeek(Long studentId, Long weekId) {
        Long courseId = weekRepository.findCourseIdById(weekId);
        if (courseId == null) return false;
        return canAccessCourse(studentId, courseId);
    }

    // ✅ التحقق من الجلسة
    @Cacheable(value = "sessionAccess", key = "#studentId + '-' + #sessionId")
    public boolean canAccessSession(Long studentId, Long sessionId) {
        Long courseId = sessionRepository.findCourseIdById(sessionId);
        if (courseId == null) return false;
        return canAccessCourse(studentId, courseId);
    }
}