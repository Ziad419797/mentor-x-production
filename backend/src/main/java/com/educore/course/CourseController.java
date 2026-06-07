package com.educore.course;

import com.educore.common.FileUploadService;
import com.educore.common.GlobalResponse;
import com.educore.dtocourse.request.CreateCourseRequest;
import com.educore.dtocourse.request.UpdateCourseRequest;
import com.educore.dtocourse.response.CourseResponse;
import com.educore.dtocourse.response.LatestCourseResponse;
import com.educore.security.JwtUserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/courses")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Courses", description = "Course management APIs")
public class CourseController {

    private final CourseService courseService;

    @Operation(summary = "Create new course")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Course created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input"),
            @ApiResponse(responseCode = "404", description = "Category not found")
    })
    // CreateCourseRequest مع multipart
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CourseResponse> createCourseWithImage(
            @ModelAttribute CreateCourseRequest request,
            @RequestPart(value = "image", required = false) MultipartFile image) {

        log.info("POST /api/courses with title: {}", request.getTitle());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(courseService.createCourse(request, image));
    }

    @Operation(summary = "Update existing course")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Course updated successfully"),
            @ApiResponse(responseCode = "404", description = "Course not found")
    })
    @PutMapping(value = "/{id}/with-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CourseResponse> updateCourseWithImage(
            @PathVariable Long id,
            @ModelAttribute @Valid UpdateCourseRequest request,
            @RequestPart(value = "image", required = false) MultipartFile image) {

        log.info("PUT /api/courses/{}/with-image", id);

        return ResponseEntity.ok(courseService.updateCourse(id, request, image));
    }

    @Operation(summary = "Delete course")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Course deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Course not found")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCourse(@PathVariable Long id) {

        log.info("DELETE /api/courses/{}", id);

        courseService.deleteCourse(id);

        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Get course by id")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Course found"),
            @ApiResponse(responseCode = "404", description = "Course not found")
    })
    @GetMapping("/{id}")
    public ResponseEntity<CourseResponse> getCourseById(@PathVariable Long id) {

        log.info("GET /api/courses/{}", id);

        return ResponseEntity.ok(courseService.getCourseById(id));
    }

    @Operation(summary = "Get all courses with pagination & sorting")
    @GetMapping
    public ResponseEntity<Page<CourseResponse>> getAllCourses(Pageable pageable) {

        log.info("GET /api/courses");

        return ResponseEntity.ok(courseService.getAllCourses(pageable));
    }

    @Operation(summary = "Get courses by category with pagination & sorting")
    @GetMapping("/category/{categoryId}")
    public ResponseEntity<Page<CourseResponse>> getCoursesByCategory(
            @PathVariable Long categoryId,
            Pageable pageable) {

        log.info("GET /api/courses/category/{}", categoryId);

        return ResponseEntity.ok(
                courseService.getCoursesByCategory(categoryId, pageable)
        );
    }


    // ================= active =================

    // TeacherCourseController.java

    @Operation(summary = "Change course category", description = "نقل الكورس من كاتيجوري لكاتيجوري أخرى")
    @PatchMapping("/{id}/change-category")
    public ResponseEntity<GlobalResponse<Void>> changeCourseCategory(
            @PathVariable Long id,
            @RequestBody java.util.Map<String, Long> body) {

        log.info("PATCH /api/courses/{}/change-category", id);
        courseService.changeCourseCategory(id, body.get("newCategoryId"));
        return ResponseEntity.ok(GlobalResponse.<Void>builder()
                .success(true)
                .message("تم نقل الكورس بنجاح")
                .build());
    }

    @Operation(summary = "Toggle course activation status", description = "إغلاق أو فتح الكورس للطلاب")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Status toggled successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Course not found")
    })
    @PatchMapping("/{id}/toggle-status")
    public ResponseEntity<GlobalResponse<Void>> toggleStatus(@PathVariable Long id) {

        log.info("PATCH /api/courses/{}/toggle-status", id);

        courseService.toggleCourseStatus(id);

        return ResponseEntity.ok(GlobalResponse.<Void>builder()
                .success(true)
                .message("تم تغيير حالة النشاط بنجاح")
                .build());
    }
    /**
     * للمعلمين والأدمن: يرجع آخر الكورسات المضافة
     */
    @Operation(summary = "آخر الكورسات المضافة (للمعلمين والأدمن)")
    @GetMapping("/latest")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN', 'STAFF')")
    public ResponseEntity<GlobalResponse<List<LatestCourseResponse>>> getLatestCourses(
            @RequestParam(defaultValue = "10") int limit
    ) {
        log.info("API [GET] /api/courses/latest?limit={}", limit);

        // تحد من الـ limit
        limit = Math.min(limit, 50);

        List<LatestCourseResponse> courses = courseService.getLatestCourses(limit);

        return ResponseEntity.ok(GlobalResponse.<List<LatestCourseResponse>>builder()
                .success(true)
                .message("تم جلب آخر الكورسات بنجاح")
                .data(courses)
                .build());
    }

    /**
     * للطلاب: يرجع آخر الكورسات التي هو مشترك فيها
     */
    @Operation(summary = "آخر الكورسات المشترك فيها (للطلاب)")
    @GetMapping("/my-latest")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<GlobalResponse<List<LatestCourseResponse>>> getMyLatestCourses(
            @AuthenticationPrincipal JwtUserPrincipal principal,
            @RequestParam(defaultValue = "10") int limit
    ) {
        log.info("API [GET] /api/courses/my-latest?limit={} for student {}", limit, principal.getUserId());

        limit = Math.min(limit, 50);

        List<LatestCourseResponse> courses = courseService.getLatestCoursesForStudent(principal.getUserId(), limit);

        return ResponseEntity.ok(GlobalResponse.<List<LatestCourseResponse>>builder()
                .success(true)
                .message("تم جلب آخر كورساتك بنجاح")
                .data(courses)
                .build());
    }

    /**
     * للطلاب: يرجع الكورسات المميزة (featured = true)
     */
    @Operation(summary = "الكورسات المميزة (للطلاب)")
    @GetMapping("/featured")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<GlobalResponse<List<LatestCourseResponse>>> getFeaturedCourses(
            @AuthenticationPrincipal JwtUserPrincipal principal,
            @RequestParam(defaultValue = "6") int limit
    ) {
        log.info("API [GET] /api/courses/featured?limit={} for student {}", limit, principal.getUserId());
        limit = Math.min(limit, 20);
        List<LatestCourseResponse> courses = courseService.getFeaturedCourses(principal.getUserId(), limit);
        return ResponseEntity.ok(GlobalResponse.<List<LatestCourseResponse>>builder()
                .success(true)
                .message("تم جلب الكورسات المميزة بنجاح")
                .data(courses)
                .build());
    }

    @Operation(summary = "Get the level ID of a course (via its first category)")
    @GetMapping("/{id}/level-id")
    public ResponseEntity<java.util.Map<String, Object>> getCourseLevelId(@PathVariable Long id) {
        log.info("GET /api/courses/{}/level-id", id);
        com.educore.course.Course course = courseService.findRawById(id);
        Long levelId = course.getCategories().stream()
                .filter(cat -> cat.getLevel() != null)
                .map(cat -> cat.getLevel().getId())
                .findFirst()
                .orElse(null);
        return ResponseEntity.ok(java.util.Map.of("levelId", levelId != null ? levelId : 0));
    }
}
