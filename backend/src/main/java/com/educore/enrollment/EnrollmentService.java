package com.educore.enrollment;

import com.educore.DtoEnroll.mapper.EnrollmentMapper;
import com.educore.DtoEnroll.request.EnrollmentProgressRequest;
import com.educore.DtoEnroll.response.EnrollmentResponse;
import com.educore.DtoEnroll.response.EnrollmentStatsResponse;
import com.educore.category.Category;
import com.educore.category.CategoryRepository;
import com.educore.course.Course;
import com.educore.course.CourseRepository;
import com.educore.exception.ResourceNotFoundException;
import com.educore.lessongate.LessonGateService;
import com.educore.security.JwtUserPrincipal;
import com.educore.student.Student;
import com.educore.student.StudentRepository;
import com.educore.unit.SessionRepository;
import jakarta.validation.ValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.*;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class EnrollmentService {

    private final EnrollmentRepository enrollmentRepository;
    private final StudentRepository    studentRepository;
    private final CourseRepository     courseRepository;
    private final CategoryRepository   categoryRepository;
    private final EnrollmentMapper     enrollmentMapper;
    private final SessionRepository    sessionRepository;
    @Lazy
    private final LessonGateService    lessonGateService;

    /* ════════════════════════════════════════════════════
       CREATE — يُستدعى فقط من PaymentService أو Admin
    ════════════════════════════════════════════════════ */

    /**
     * يُستدعى من PaymentService بعد نجاح الدفع — ليس من الـ Controller مباشرة.
     * لو التسجيل موجود مسبقاً يتجاهله بدون exception.
     */
    @Transactional
    @CacheEvict(value = {"studentEnrollments", "courseEnrollments",
            "courseAccess", "accessibleCourses"}, allEntries = true)
    public Optional<EnrollmentResponse> enrollAfterPayment(
            Long studentId, Long courseId, Long categoryId,
            EnrollmentType type, String createdBy) {

        if (enrollmentRepository.existsByStudentIdAndCourseIdAndActiveTrue(studentId, courseId)) {
            log.debug("Student {} already enrolled in course {} — skipping", studentId, courseId);
            return Optional.empty();
        }

        Student student = studentRepository.getReferenceById(studentId);
        Course  course  = courseRepository.getReferenceById(courseId);
        Category category = (categoryId != null)
                ? categoryRepository.getReferenceById(categoryId)
                : null;

        Enrollment enrollment = Enrollment.builder()
                .student(student)
                .course(course)
                .category(category)
                .enrollmentType(type)
                .status(EnrollmentStatus.ACTIVE)
                .progress(0.0)
                .enrolledAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusYears(1))
                .createdBy(createdBy)
                .build();

        Enrollment saved = enrollmentRepository.save(enrollment);
        log.info("Enrollment created: student={} course={} type={}", studentId, courseId, type);

        // ─── فتح أول حصة في كل Session مرتبطة بالكورس ────────────
        try {
            sessionRepository.findByCourseIdActive(courseId).forEach(session ->
                lessonGateService.unlockFirstLesson(studentId, session.getId())
            );
        } catch (Exception e) {
            // لا نوقف التسجيل لو فيه مشكلة في الـ Gate
            log.warn("LessonGate unlock failed for student={} course={}: {}", studentId, courseId, e.getMessage());
        }

        return Optional.of(enrollmentMapper.toResponse(saved));
    }

    /**
     * الأدمن يضيف طالب يدوياً (مثلاً دفع كاش وتم التأكيد).
     */
    @Transactional
    @CacheEvict(value = {"studentEnrollments", "courseEnrollments",
            "courseAccess", "accessibleCourses"}, allEntries = true)
    public EnrollmentResponse adminGrantEnrollment(
            Long studentId, Long courseId, String adminUsername) {

        log.info("Admin {} granting enrollment: student={} course={}",
                adminUsername, studentId, courseId);

        if (enrollmentRepository.existsByStudentIdAndCourseIdAndActiveTrue(studentId, courseId)) {
            throw new ValidationException("الطالب مسجل في هذا الكورس بالفعل");
        }

        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("الطالب غير موجود"));
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("الكورس غير موجود"));

        Enrollment enrollment = Enrollment.builder()
                .student(student)
                .course(course)
                .enrollmentType(EnrollmentType.ADMIN_GRANT)
                .status(EnrollmentStatus.ACTIVE)
                .progress(0.0)
                .enrolledAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusYears(1))
                .createdBy(adminUsername)
                .build();

        Enrollment saved = enrollmentRepository.save(enrollment);

        // فتح أول حصة بعد المنح اليدوي
        try {
            sessionRepository.findByCourseIdActive(courseId).forEach(session ->
                lessonGateService.unlockFirstLesson(studentId, session.getId())
            );
        } catch (Exception e) {
            log.warn("LessonGate unlock failed after admin grant: {}", e.getMessage());
        }

        return enrollmentMapper.toResponse(saved);
    }

    /* ════════════════════════════════════════════════════
       READ
    ════════════════════════════════════════════════════ */

    @Cacheable(value = "enrollment", key = "#enrollmentId")
    @Transactional(readOnly = true)
    public EnrollmentResponse getEnrollmentById(Long enrollmentId) {
        return enrollmentMapper.toResponse(findEnrollmentEntity(enrollmentId));
    }

    public Enrollment getEnrollmentEntityById(Long enrollmentId) {
        return findEnrollmentEntity(enrollmentId);
    }

    @Cacheable(value = "studentEnrollments", key = "#studentId + '-' + #pageable.pageNumber")
    @Transactional(readOnly = true)
    public Page<EnrollmentResponse> getStudentEnrollments(Long studentId, Pageable pageable) {
        return enrollmentRepository
                .findByStudentIdAndActiveTrue(studentId, pageable)
                .map(enrollmentMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public List<EnrollmentResponse> getStudentActiveEnrollments(Long studentId) {
        return enrollmentMapper.toResponseList(
                enrollmentRepository.findByStudentIdAndStatusAndActiveTrue(studentId, EnrollmentStatus.ACTIVE));
    }

    @Transactional(readOnly = true)
    public Page<EnrollmentResponse> getStudentEnrollmentsByStatus(
            Long studentId, EnrollmentStatus status, Pageable pageable) {
        return enrollmentRepository
                .findByStudentIdAndStatusAndActiveTrue(studentId, status, pageable)
                .map(enrollmentMapper::toResponse);
    }

    @Cacheable(value = "courseEnrollments", key = "#courseId + '-' + #pageable.pageNumber")
    @Transactional(readOnly = true)
    public Page<EnrollmentResponse> getCourseEnrollments(Long courseId, Pageable pageable) {
        return enrollmentRepository
                .findByCourseIdAndActiveTrue(courseId, pageable)
                .map(enrollmentMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public EnrollmentResponse getStudentCourseEnrollment(Long studentId, Long courseId) {
        return enrollmentRepository
                .findByStudentIdAndCourseIdAndActiveTrue(studentId, courseId)
                .map(enrollmentMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("الطالب غير مسجل في هذا الكورس"));
    }

    /* ════════════════════════════════════════════════════
       UPDATE — Progress
    ════════════════════════════════════════════════════ */

    @Transactional
    @CachePut(value = "enrollment", key = "#enrollmentId")
    public EnrollmentResponse updateProgress(
            Long enrollmentId, EnrollmentProgressRequest request, String updatedBy) {

        Enrollment enrollment = findEnrollmentEntity(enrollmentId);
        enrollment.updateProgress(request.getProgress());

        if (request.getWatchTimeSeconds() != null)
            enrollment.addWatchTime(request.getWatchTimeSeconds());

        if (request.getCompletedLessons() != null && request.getTotalLessons() != null)
            enrollment.updateLessonProgress(request.getCompletedLessons(), request.getTotalLessons());

        enrollment.setUpdatedBy(updatedBy);
        return enrollmentMapper.toResponse(enrollmentRepository.save(enrollment));
    }

    @Transactional
    @CachePut(value = "enrollment", key = "#enrollmentId")
    @CacheEvict(value = {"studentEnrollments", "courseEnrollments"}, allEntries = true)
    public EnrollmentResponse completeEnrollment(Long enrollmentId, String updatedBy) {
        Enrollment enrollment = findEnrollmentEntity(enrollmentId);
        enrollment.completeEnrollment();
        enrollment.setUpdatedBy(updatedBy);
        return enrollmentMapper.toResponse(enrollmentRepository.save(enrollment));
    }

    @Transactional
    @CachePut(value = "enrollment", key = "#enrollmentId")
    @CacheEvict(value = {"studentEnrollments", "courseEnrollments"}, allEntries = true)
    public EnrollmentResponse extendEnrollment(
            Long enrollmentId, LocalDateTime newExpiry, String updatedBy) {
        Enrollment enrollment = findEnrollmentEntity(enrollmentId);
        enrollment.extendExpiry(newExpiry);
        enrollment.setUpdatedBy(updatedBy);
        return enrollmentMapper.toResponse(enrollmentRepository.save(enrollment));
    }

    @Transactional
    @CacheEvict(value = {"enrollment", "studentEnrollments", "courseEnrollments",
            "courseAccess", "accessibleCourses"}, allEntries = true)
    public void recordAccess(Long enrollmentId) {
        enrollmentRepository.recordAccess(enrollmentId);
    }

    /* ════════════════════════════════════════════════════
       DELETE / CANCEL
    ════════════════════════════════════════════════════ */

    @Transactional
    @CacheEvict(value = {"enrollment", "studentEnrollments", "courseEnrollments",
            "courseAccess", "accessibleCourses"}, allEntries = true)
    public void cancelEnrollment(Long enrollmentId, String cancelledBy) {
        Enrollment enrollment = findEnrollmentEntity(enrollmentId);
        enrollment.cancel(cancelledBy);
        enrollmentRepository.save(enrollment);
        log.info("Enrollment {} cancelled by {}", enrollmentId, cancelledBy);
    }

    @Transactional
    @CacheEvict(value = {"studentEnrollments", "courseEnrollments",
            "courseAccess", "accessibleCourses"}, allEntries = true)
    public void cancelAllStudentEnrollments(Long studentId, String cancelledBy) {
        int count = enrollmentRepository.cancelAllStudentEnrollments(studentId, cancelledBy);
        log.info("Cancelled {} enrollments for student {} by {}", count, studentId, cancelledBy);
    }

    /* ════════════════════════════════════════════════════
       BATCH
    ════════════════════════════════════════════════════ */

    @Transactional
    public int expireOldEnrollments() {
        int count = enrollmentRepository.expireEnrollments(LocalDateTime.now());
        if (count > 0) log.info("Expired {} enrollments", count);
        return count;
    }

    /* ════════════════════════════════════════════════════
       STATISTICS
    ════════════════════════════════════════════════════ */

    @Transactional(readOnly = true)
    public EnrollmentStatsResponse getStudentStats(Long studentId) {
        Long totalEnrollments = enrollmentRepository.countActiveEnrollmentsByStudent(studentId);
        List<Enrollment> active = enrollmentRepository
                .findActiveByStudentAndStatus(studentId, EnrollmentStatus.ACTIVE);

        Map<String, Long>   enrollmentsByCourse = active.stream()
                .collect(Collectors.groupingBy(e -> e.getCourse().getTitle(), Collectors.counting()));
        Map<String, Double> progressByCourse = active.stream()
                .collect(Collectors.toMap(e -> e.getCourse().getTitle(), Enrollment::getProgress,
                        (a, b) -> a));  // merge function لو في تكرار

        String mostActive = active.stream()
                .max(Comparator.comparing(Enrollment::getAccessCount))
                .map(e -> e.getCourse().getTitle()).orElse(null);
        String bestScore  = active.stream()
                .max(Comparator.comparing(Enrollment::getAverageQuizScore))
                .map(e -> e.getCourse().getTitle()).orElse(null);

        return enrollmentMapper.toStatsResponse(
                totalEnrollments,
                (long) active.size(),
                0L, 0L,
                active.stream().mapToDouble(Enrollment::getProgress).average().orElse(0.0),
                active.stream().mapToLong(Enrollment::getTotalWatchTimeSeconds).sum(),
                enrollmentsByCourse, progressByCourse,
                active.stream().mapToInt(Enrollment::getQuizzesTaken).sum(),
                active.stream().mapToInt(Enrollment::getAssignmentsSubmitted).sum(),
                mostActive, bestScore
        );
    }

    /* ════════════════════════════════════════════════════
       VALIDATION
    ════════════════════════════════════════════════════ */

    public void validateEnrollmentAccess(Long enrollmentId, JwtUserPrincipal principal) {
        Enrollment enrollment = findEnrollmentEntity(enrollmentId);

        if (!enrollment.getStudent().getId().equals(principal.getUserId()))
            throw new ValidationException("ليس لديك صلاحية الوصول لهذا التسجيل");

        if (!enrollment.isValidAccess())
            throw new ValidationException("التسجيل غير نشط أو منتهي الصلاحية");
    }

    /* ════════════════════════════════════════════════════
       PRIVATE HELPERS
    ════════════════════════════════════════════════════ */

    private Enrollment findEnrollmentEntity(Long id) {
        return enrollmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("التسجيل غير موجود: " + id));
    }

    private Integer calculateTotalLessons(Course course) {
        return 0; // TODO: ربطه بـ LessonRepository
    }
}