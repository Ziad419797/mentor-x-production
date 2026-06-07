package com.educore.enrollment;

import com.educore.DtoEnroll.request.EnrollmentProgressRequest;
import com.educore.DtoEnroll.response.EnrollmentResponse;
import com.educore.DtoEnroll.response.EnrollmentStatsResponse;
import com.educore.course.CourseRepository;
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
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/enrollments")
@RequiredArgsConstructor
@Tag(name = "Enrollments", description = "إدارة تسجيل الطلاب — التسجيل يتم تلقائياً بعد الدفع فقط")
public class EnrollmentController {

    private final EnrollmentService enrollmentService;
    private final AccessService     accessService;
    private final CourseRepository  courseRepository;

    // ==================== Student: Free Enrollment ====================

    @Operation(summary = "اشتراك مجاني في كورس بسعر صفر")
    @PostMapping
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<Map<String, Object>> enrollFree(
            @RequestBody Map<String, Long> body,
            @AuthenticationPrincipal JwtUserPrincipal principal) {

        Long courseId = body.get("courseId");
        if (courseId == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "courseId مطلوب"));
        }

        // تحقق إن الكورس مجاني فعلاً
        com.educore.course.Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new com.educore.exception.ResourceNotFoundException("الكورس غير موجود"));
        java.math.BigDecimal price = course.getPrice();
        if (price != null && price.compareTo(java.math.BigDecimal.ZERO) > 0) {
            return ResponseEntity.badRequest().body(Map.of("message", "الكورس ليس مجانياً"));
        }

        enrollmentService.enrollAfterPayment(
                principal.getUserId(), courseId, null,
                EnrollmentType.COURSE_PURCHASE, "FREE:" + principal.getUsername());

        return ResponseEntity.ok(Map.of("success", true, "message", "تم الاشتراك في الكورس المجاني بنجاح"));
    }

    // ==================== Read: Student ====================

    @Operation(summary = "تسجيلاتي النشطة")
    @GetMapping("/my-enrollments")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<Page<EnrollmentResponse>> getMyEnrollments(
            @AuthenticationPrincipal JwtUserPrincipal principal,
            @RequestHeader(value = "X-Device-Id", required = false) String deviceId,

            @PageableDefault(size = 10, sort = "enrolledAt", direction = Sort.Direction.DESC)
            Pageable pageable
    ) {
        return ResponseEntity.ok(
                enrollmentService.getStudentEnrollments(principal.getUserId(), pageable));
    }

    @Operation(summary = "هل أنا مسجل في هذا الكورس؟")
    @GetMapping("/check/{courseId}")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<Map<String, Object>> checkEnrollment(
            @PathVariable Long courseId,
            @AuthenticationPrincipal JwtUserPrincipal principal,
            @RequestHeader(value = "X-Device-Id", required = false) String deviceId

    ) {
        boolean canAccess = accessService.canAccessCourse(principal.getUserId(), courseId);

        Map<String, Object> response = new HashMap<>();
        response.put("enrolled", canAccess);

        if (canAccess) {
            try {
                response.put("enrollment",
                        enrollmentService.getStudentCourseEnrollment(principal.getUserId(), courseId));
            } catch (Exception ignored) { /* enrollment يمكن ينجو لو جه من category */ }
        }

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "إحصائياتي")
    @GetMapping("/stats/my-stats")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<EnrollmentStatsResponse> getMyStats(
            @AuthenticationPrincipal JwtUserPrincipal principal,
            @RequestHeader(value = "X-Device-Id", required = false) String deviceId

    ) {
        return ResponseEntity.ok(enrollmentService.getStudentStats(principal.getUserId()));
    }

    // ==================== Progress ====================

    @Operation(summary = "تحديث تقدمي في كورس")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "تم التحديث"),
            @ApiResponse(responseCode = "403", description = "لا تملك وصولاً لهذا الكورس")
    })
    @PatchMapping("/{enrollmentId}/progress")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<EnrollmentResponse> updateProgress(
            @PathVariable Long enrollmentId,
            @Valid @RequestBody EnrollmentProgressRequest request,
            @AuthenticationPrincipal JwtUserPrincipal principal,
            @RequestHeader(value = "X-Device-Id", required = false) String deviceId

    ) {
        // تحقق إن التسجيل فعلاً للطالب ده وإن الوصول سليم
        enrollmentService.validateEnrollmentAccess(enrollmentId, principal);

        return ResponseEntity.ok(
                enrollmentService.updateProgress(enrollmentId, request, principal.getUsername()));
    }

    @Operation(summary = "تسجيل دخول لكورس (يزيد عداد الدخول)")
    @PostMapping("/{enrollmentId}/access")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<Void> recordAccess(
            @PathVariable Long enrollmentId,
            @AuthenticationPrincipal JwtUserPrincipal principal,
            @RequestHeader(value = "X-Device-Id", required = false) String deviceId

    ) {
        enrollmentService.validateEnrollmentAccess(enrollmentId, principal);
        enrollmentService.recordAccess(enrollmentId);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "إكمال كورس")
    @PostMapping("/{enrollmentId}/complete")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<EnrollmentResponse> completeEnrollment(
            @PathVariable Long enrollmentId,
            @AuthenticationPrincipal JwtUserPrincipal principal,
            @RequestHeader(value = "X-Device-Id", required = false) String deviceId

    ) {
        enrollmentService.validateEnrollmentAccess(enrollmentId, principal);
        return ResponseEntity.ok(
                enrollmentService.completeEnrollment(enrollmentId, principal.getUsername()));
    }

    // ==================== Read: Admin / Teacher ====================

    @Operation(summary = "[ADMIN/TEACHER] تسجيلات طالب محدد")
    @GetMapping("/admin/student/{studentId}")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public ResponseEntity<Page<EnrollmentResponse>> getStudentEnrollments(
            @PathVariable Long studentId,
            @PageableDefault(size = 10) Pageable pageable
    ) {
        return ResponseEntity.ok(enrollmentService.getStudentEnrollments(studentId, pageable));
    }

    @Operation(summary = "[ADMIN/TEACHER] تسجيلات كورس محدد")
    @GetMapping("/admin/course/{courseId}")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public ResponseEntity<Page<EnrollmentResponse>> getCourseEnrollments(
            @PathVariable Long courseId,
            @PageableDefault(size = 10) Pageable pageable
    ) {
        return ResponseEntity.ok(enrollmentService.getCourseEnrollments(courseId, pageable));
    }

    @Operation(summary = "[ADMIN] منح تسجيل يدوي لطالب (مثلاً: دفع كاش)")
    @PostMapping("/admin/grant")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> adminGrantEnrollment(
            @RequestParam Long studentId,
            @RequestParam Long courseId,
            @AuthenticationPrincipal JwtUserPrincipal principal
    ) {
        EnrollmentResponse enrollment = enrollmentService.adminGrantEnrollment(
                studentId, courseId, "Admin#" + principal.getUserId());
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "تم تسجيل الطالب بنجاح",
                "enrollment", enrollment
        ));
    }

    @Operation(summary = "[ADMIN] تمديد صلاحية تسجيل")
    @PatchMapping("/admin/{enrollmentId}/extend")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public ResponseEntity<EnrollmentResponse> extendEnrollment(
            @PathVariable Long enrollmentId,
            @RequestParam LocalDateTime newExpiry,
            @AuthenticationPrincipal JwtUserPrincipal principal
    ) {
        return ResponseEntity.ok(
                enrollmentService.extendEnrollment(enrollmentId, newExpiry, principal.getUsername()));
    }

    @Operation(summary = "[ADMIN] إلغاء تسجيل")
    @DeleteMapping("/admin/{enrollmentId}")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public ResponseEntity<Void> cancelEnrollment(
            @PathVariable Long enrollmentId,
            @AuthenticationPrincipal JwtUserPrincipal principal
    ) {
        enrollmentService.cancelEnrollment(enrollmentId, principal.getUsername());
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "[ADMIN] إلغاء كل تسجيلات طالب")
    @DeleteMapping("/admin/student/{studentId}/cancel-all")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public ResponseEntity<Map<String, Object>> cancelAllStudentEnrollments(
            @PathVariable Long studentId,
            @AuthenticationPrincipal JwtUserPrincipal principal
    ) {
        enrollmentService.cancelAllStudentEnrollments(studentId, principal.getUsername());
        return ResponseEntity.ok(Map.of("message", "تم إلغاء جميع التسجيلات"));
    }

    @Operation(summary = "[ADMIN] إنهاء التسجيلات منتهية الصلاحية")
    @PostMapping("/admin/expire-old")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public ResponseEntity<Map<String, Object>> expireOldEnrollments() {
        int count = enrollmentService.expireOldEnrollments();
        return ResponseEntity.ok(Map.of(
                "expiredCount", count,
                "message", "تم إنهاء " + count + " تسجيل"
        ));
    }
}