package com.educore.course;

import com.educore.category.Category;
import com.educore.category.CategoryRepository;
import com.educore.common.CacheNames;
import com.educore.common.FileUploadService;
import com.educore.common.SortFields;
import com.educore.common.SortValidator;
import com.educore.dtocourse.mapper.CourseMapper;
import com.educore.dtocourse.request.CreateCourseRequest;
import com.educore.dtocourse.request.UpdateCourseRequest;
import com.educore.dtocourse.response.CourseResponse;
import com.educore.dtocourse.response.LatestCourseResponse;
import com.educore.enrollment.EnrollmentRepository;
import com.educore.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

@CacheConfig(cacheNames = CacheNames.COURSES)
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class CourseService {

    private final CourseRepository courseRepository;
    private final CategoryRepository categoryRepository;
    private final EnrollmentRepository enrollmentRepository;  // ← أضف
    private final CourseMapper courseMapper;
    private final SortValidator sortValidator;
    private final FileUploadService fileUploadService;  // ← أضف

    // ================= CREATE =================
    @Caching(evict = {
            @CacheEvict(value = CacheNames.COURSES_PAGES, allEntries = true),
            @CacheEvict(value = CacheNames.COURSES_BY_CATEGORY, allEntries = true),
            @CacheEvict(value = CacheNames.CATEGORIES_PAGES, allEntries = true),
            @CacheEvict(value = CacheNames.SESSIONS_BY_COURSE, allEntries = true)
    })
    public CourseResponse createCourse(CreateCourseRequest request, MultipartFile image) {

        log.info("Creating course '{}' and linking to categories {}",
                request.getTitle(), request.getEffectiveCategoryIds());

        if (image != null && !image.isEmpty()) {
            try {
                String imageUrl = fileUploadService.uploadImage(image, "courses");
                request.setImageUrl(imageUrl);
                log.info("Image uploaded successfully: {}", imageUrl);
            } catch (Exception e) {
                log.error("Failed to upload image: {}", e.getMessage());
                throw new RuntimeException("فشل في رفع الصورة: " + e.getMessage());
            }
        }



        // تحميل الكاتيجوريز — يستخدم getEffectiveCategoryIds لدعم categoryId (مفرد) و categoryIds (جمع)
        Set<Long> effectiveIds = request.getEffectiveCategoryIds();
        Set<Category> categories = new HashSet<>();
        if (!effectiveIds.isEmpty()) {
            categories = new HashSet<>(categoryRepository.findAllById(effectiveIds));
            if (categories.size() != effectiveIds.size()) {
                throw new ResourceNotFoundException("One or more categories not found");
            }
        }

        // default orderNumber إذا لم يُحدد
        if (request.getOrderNumber() == null) {
            request.setOrderNumber((int) courseRepository.count() + 1);
        }

        var course = courseMapper.toEntity(request);

        // ✅ Ensure active = true for new courses (Lombok @Builder defaults primitive boolean to false)
        course.setActive(true);

        if (request.getImageUrl() != null && !request.getImageUrl().isEmpty()) {
            course.setImageUrl(request.getImageUrl());
        }

        // ربط العلاقات ManyToMany
        course.setCategories(categories);

        categories.forEach(category ->
                category.getCourses().add(course)
        );

        courseRepository.save(course);

        return courseMapper.toResponse(course);
    }

    // ================= UPDATE =================
    @Caching(evict = {
            @CacheEvict(value = CacheNames.COURSES, key = "#id"),
            @CacheEvict(value = CacheNames.COURSES_PAGES, allEntries = true),
            @CacheEvict(value = CacheNames.COURSES_BY_CATEGORY, allEntries = true),
            @CacheEvict(value = CacheNames.CATEGORIES_PAGES, allEntries = true),
            @CacheEvict(value = CacheNames.SESSIONS_BY_COURSE, allEntries = true)
    })
    public CourseResponse updateCourse(Long id, UpdateCourseRequest request, MultipartFile image) {

        log.info("Updating course id {}", id);

        var course = courseRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Course not found with id " + id));
        if (image != null && !image.isEmpty()) {
            try {
                // حذف الصورة القديمة إذا كانت موجودة
                if (course.getImageUrl() != null) {
                    fileUploadService.deleteFile(course.getImageUrl());
                    log.info("Old image deleted: {}", course.getImageUrl());
                }

                String imageUrl = fileUploadService.uploadImage(image, "courses");
                request.setImageUrl(imageUrl);
                log.info("New image uploaded successfully: {}", imageUrl);
            } catch (Exception e) {
                log.error("Failed to upload image: {}", e.getMessage());
                throw new RuntimeException("فشل في رفع الصورة: " + e.getMessage());
            }
        }
        courseMapper.updateEntityFromRequest(request, course);

        return courseMapper.toResponse(course);
    }

    // ================= DELETE =================
    @Caching(evict = {
            @CacheEvict(value = CacheNames.COURSES, key = "#id"),
            @CacheEvict(value = CacheNames.COURSES_PAGES, allEntries = true),
            @CacheEvict(value = CacheNames.COURSES_BY_CATEGORY, allEntries = true),
            @CacheEvict(value = CacheNames.CATEGORIES_PAGES, allEntries = true),
            @CacheEvict(value = CacheNames.SESSIONS_BY_COURSE, allEntries = true),
            @CacheEvict(value = CacheNames.SESSIONS_PAGES, allEntries = true),
            @CacheEvict(value = CacheNames.LESSONS_PAGES, allEntries = true),
            @CacheEvict(value = CacheNames.LESSONS_BY_SESSION, allEntries = true)
    })
    public void deleteCourse(Long id) {

        log.info("Deleting course id {}", id);

        var course = courseRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Course not found with id " + id));
        // حذف الصورة من Cloudinary إذا وجدت
        if (course.getImageUrl() != null) {
            fileUploadService.deleteFile(course.getImageUrl());
        }
        // تنظيف العلاقات قبل الحذف
        course.getCategories().forEach(category ->
                category.getCourses().remove(course)
        );

        courseRepository.delete(course);
    }

    // ================= GET =================
    @Cacheable(value = CacheNames.COURSES, key = "#id")
    @Transactional(readOnly = true)
    public CourseResponse getCourseById(Long id) {

        var course = courseRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Course not found with id " + id));

        CourseResponse response = courseMapper.toResponse(course);

        // ✅ جلب عدد الطلاب المشتركين في هذا الكورس
        List<Long> courseIds = List.of(id);
        Map<Long, Long> enrolledCountMap = getEnrolledStudentsCount(courseIds);
        response.setEnrolledStudentsCount(enrolledCountMap.getOrDefault(id, 0L));

        return response;
    }


    @Transactional(readOnly = true)
    @Cacheable(value = CacheNames.COURSES_PAGES, key = "'all-' + #pageable.pageNumber")
    public Page<CourseResponse> getAllCourses(Pageable pageable) {

        sortValidator.validate(pageable, SortFields.COURSE);

        Page<Course> courses = courseRepository.findAll(pageable);

        // ✅ جلب IDs الكورسات في الصفحة الحالية
        List<Long> courseIds = courses.getContent().stream()
                .map(Course::getId)
                .toList();

        // ✅ جلب عدد الطلاب المشتركين لكل كورس
        Map<Long, Long> enrolledCountMap = getEnrolledStudentsCount(courseIds);

        // ✅ تحويل الكورسات إلى Response مع إضافة عدد الطلاب
        return courses.map(course -> {
            CourseResponse response = courseMapper.toResponse(course);
            Long count = enrolledCountMap.getOrDefault(course.getId(), 0L);
            response.setEnrolledStudentsCount(count);
            return response;
        });
    }
    // ✅ أضف هذه الدالة المساعدة
    private Map<Long, Long> getEnrolledStudentsCount(List<Long> courseIds) {
        if (courseIds == null || courseIds.isEmpty()) {
            return new HashMap<>();
        }

        List<Object[]> results = courseRepository.countEnrolledStudentsByCourseIds(courseIds);
        Map<Long, Long> countMap = new HashMap<>();

        for (Object[] result : results) {
            Long courseId = ((Number) result[0]).longValue();
            Long count = ((Number) result[1]).longValue();
            countMap.put(courseId, count);
        }

        return countMap;
    }

    @Transactional(readOnly = true)
    @Cacheable(value = CacheNames.COURSES_BY_CATEGORY, key = "#categoryId + '-' + #pageable.pageNumber")
    public Page<CourseResponse> getCoursesByCategory(Long categoryId, Pageable pageable) {

        if (!categoryRepository.existsById(categoryId)) {
            throw new ResourceNotFoundException("Category not found with id " + categoryId);
        }

        sortValidator.validate(pageable, SortFields.COURSE);

        Page<Course> courses = courseRepository.findByCategoriesId(categoryId, pageable);

        // ✅ جلب IDs الكورسات في الصفحة الحالية
        List<Long> courseIds = courses.getContent().stream()
                .map(Course::getId)
                .toList();

        // ✅ جلب عدد الطلاب المشتركين لكل كورس
        Map<Long, Long> enrolledCountMap = getEnrolledStudentsCount(courseIds);

        // ✅ تحويل الكورسات إلى Response مع إضافة عدد الطلاب
        return courses.map(course -> {
            CourseResponse response = courseMapper.toResponse(course);
            Long count = enrolledCountMap.getOrDefault(course.getId(), 0L);
            response.setEnrolledStudentsCount(count);
            return response;
        });
    }

    // ================= change category =================

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = CacheNames.COURSES, key = "#courseId"),
            @CacheEvict(value = CacheNames.COURSES_BY_CATEGORY, allEntries = true),
            @CacheEvict(value = CacheNames.CATEGORIES_PAGES, allEntries = true)
    })
    public void changeCourseCategory(Long courseId, Long newCategoryId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found with id " + courseId));

        Category newCategory = categoryRepository.findById(newCategoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id " + newCategoryId));

        // إزالة الكاتيجوريز القديمة وربط الجديدة
        course.getCategories().forEach(cat -> cat.getCourses().remove(course));
        course.getCategories().clear();
        course.getCategories().add(newCategory);
        newCategory.getCourses().add(course);

        log.info("Course '{}' moved to category '{}'", course.getTitle(), newCategory.getName());
        courseRepository.save(course);
    }

    // ================= active =================

// CourseService.java

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = CacheNames.COURSES, key = "#id"),
            @CacheEvict(value = CacheNames.COURSES_PAGES, allEntries = true),
            @CacheEvict(value = CacheNames.COURSES_BY_CATEGORY, allEntries = true),
            @CacheEvict(value = CacheNames.CATEGORIES_PAGES, allEntries = true),
            @CacheEvict(value = CacheNames.SESSIONS_BY_COURSE, allEntries = true),
            @CacheEvict(value = CacheNames.SESSIONS_PAGES, allEntries = true),
            @CacheEvict(value = CacheNames.LESSONS_PAGES, allEntries = true),
            @CacheEvict(value = CacheNames.LESSONS_BY_SESSION, allEntries = true)
    })
    public void toggleCourseStatus(Long id) {
        // ملحوظة: بما أننا نستخدم @Where(clause = "active = true")
        // الـ Repository العادي لن يجد الكورس إذا كان active = false
        // لذلك نحتاج لعمل Query مخصصة في الـ Repository تجلب العنصر بغض النظر عن حالته

        Course course = courseRepository.findByIdIncludingInactive(id)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found"));

        boolean newStatus = !course.isActive();
        course.setActive(newStatus);

        log.info("Course '{}' is now {}", course.getTitle(), newStatus ? "ACTIVE" : "INACTIVE");
        courseRepository.save(course);
    }
    // في CourseService.java

    /**
     * الحصول على آخر 10 كورسات مضافة (للمعلمين والأدمن)
     */
    @Transactional(readOnly = true)
    public List<LatestCourseResponse> getLatestCourses(int limit) {
        log.info("Fetching latest {} courses", limit);

        Pageable pageable = Pageable.ofSize(limit);
        List<Course> courses = courseRepository.findLatestCourses(pageable);

        return courses.stream()
                .map(this::mapToLatestResponse)
                .toList();
    }

    /**
     * الكورسات المميزة — للطالب (featured = true)
     */
    @Transactional(readOnly = true)
    public List<LatestCourseResponse> getFeaturedCourses(Long studentId, int limit) {
        log.info("Fetching featured courses for student {}", studentId);
        Pageable pageable = Pageable.ofSize(limit);
        List<Course> courses = courseRepository.findFeaturedCourses(pageable);
        Set<Long> enrolledIds = studentId != null ? getEnrolledCourseIds(studentId) : java.util.Collections.emptySet();
        return courses.stream()
                .map(c -> mapToLatestResponseWithEnrollment(c, enrolledIds.contains(c.getId())))
                .toList();
    }

    /**
     * الحصول على آخر كورسات للطالب (التي هو مشترك فيها)
     */
    @Transactional(readOnly = true)
    public List<LatestCourseResponse> getLatestCoursesForStudent(Long studentId, int limit) {
        log.info("Fetching latest {} courses for student {}", limit, studentId);

        Pageable pageable = Pageable.ofSize(limit);
        List<Course> courses = courseRepository.findLatestCoursesForStudent(studentId, pageable);

        return courses.stream()
                .map(course -> mapToLatestResponseWithEnrollment(course, true))
                .toList();
    }

    /**
     * الحصول على آخر كورسات مضافة مع إمكانية التحقق من اشتراك الطالب
     */
    @Transactional(readOnly = true)
    public List<LatestCourseResponse> getLatestCoursesWithEnrollmentStatus(Long studentId, int limit) {
        log.info("Fetching latest {} courses with enrollment status for student {}", limit, studentId);

        Pageable pageable = Pageable.ofSize(limit);
        List<Course> courses = courseRepository.findLatestCourses(pageable);

        // الحصول على IDs الكورسات التي الطالب مشترك فيها
        Set<Long> enrolledCourseIds = getEnrolledCourseIds(studentId);

        return courses.stream()
                .map(course -> mapToLatestResponseWithEnrollment(course, enrolledCourseIds.contains(course.getId())))
                .toList();
    }

    // Helper methods
    private LatestCourseResponse mapToLatestResponse(Course course) {
        Long enrolledCount = getEnrolledStudentsCount(List.of(course.getId())).getOrDefault(course.getId(), 0L);
        return LatestCourseResponse.builder()
                .id(course.getId())
                .title(course.getTitle())
                .description(course.getDescription())
                .imageUrl(course.getImageUrl())
                .price(course.getPrice())
                .createdAt(course.getCreatedAt())
                .totalSessions(course.getSessions() != null ? course.getSessions().size() : 0)
                .totalWeeks(0)
                .enrolledStudentsCount(enrolledCount)
                .build();
    }

    private LatestCourseResponse mapToLatestResponseWithEnrollment(Course course, boolean enrolled) {
        Long enrolledCount = getEnrolledStudentsCount(List.of(course.getId())).getOrDefault(course.getId(), 0L);
        return LatestCourseResponse.builder()
                .id(course.getId())
                .title(course.getTitle())
                .description(course.getDescription())
                .imageUrl(course.getImageUrl())
                .price(course.getPrice())
                .createdAt(course.getCreatedAt())
                .totalSessions(course.getSessions() != null ? course.getSessions().size() : 0)
                .totalWeeks(0)
                .isEnrolled(enrolled)
                .enrolledStudentsCount(enrolledCount)
                .build();
    }

    private Set<Long> getEnrolledCourseIds(Long studentId) {
        if (studentId == null) return Set.of();
        try {
            return enrollmentRepository.findByStudentIdAndActiveTrue(studentId)
                    .stream()
                    .map(e -> e.getCourse() != null ? e.getCourse().getId() : null)
                    .filter(java.util.Objects::nonNull)
                    .collect(java.util.stream.Collectors.toSet());
        } catch (Exception e) {
            log.warn("Could not fetch enrolled course ids for student {}: {}", studentId, e.getMessage());
            return Set.of();
        }
    }

    @Transactional(readOnly = true)
    public Course findRawById(Long id) {
        return courseRepository.findById(id)
                .orElseThrow(() -> new com.educore.exception.ResourceNotFoundException("Course not found: " + id));
    }
}
