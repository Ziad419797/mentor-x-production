package com.educore.lessonmaterial;

import com.educore.common.GlobalResponse;
import com.educore.dtocourse.request.LessonMaterialCreateRequest;
import com.educore.dtocourse.request.LessonMaterialUpdateRequest;
import com.educore.dtocourse.request.ReorderRequest;
import com.educore.dtocourse.response.LessonMaterialResponse;
import com.educore.enrollment.AccessService;
import com.educore.attendance.AttendanceService;
import com.educore.common.GlobalResponse;
import com.educore.lessongate.LessonGateService;
import com.educore.security.JwtUserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/materials")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Lesson Materials", description = "إدارة ملفات الدروس (فيديوهات - PDF - الخ)")
public class LessonMaterialController {

    private final LessonMaterialService materialService;
    private final AccessService         accessService;
    private final LessonGateService     lessonGateService;
    private final LessonMaterialRepository materialRepository;
    private final AttendanceService     attendanceService;

    /** يسجّل حضور الطالب أونلاين لأسبوع المادة دي — بدون ما يفشل الطلب لو الحضور اتسجل بالفعل أو حصل خطأ بسيط */
    private void registerOnlineAttendanceForMaterial(Long studentId, Long materialId, HttpServletRequest httpRequest) {
        try {
            Long weekId = materialRepository.findWeekIdByMaterialId(materialId);
            if (weekId != null) {
                attendanceService.recordOnlineAttendance(studentId, weekId, httpRequest);
            }
        } catch (Exception e) {
            log.debug("Could not auto-register online attendance for student={}, material={}: {}",
                    studentId, materialId, e.getMessage());
        }
    }

    @Operation(summary = "إضافة ملف جديد للدرس", description = "يستخدم لإضافة فيديو أو PDF أو أي مادة تعليمية داخل درس")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "تم إنشاء المادة بنجاح"),
            @ApiResponse(responseCode = "404", description = "الدرس غير موجود"),
            @ApiResponse(responseCode = "400", description = "بيانات غير صحيحة")
    })
    @PostMapping
    public ResponseEntity<LessonMaterialResponse> createMaterial(
            @Validated @RequestBody LessonMaterialCreateRequest request) {
        log.info("API /api/materials [POST] called");
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(materialService.createMaterial(request));
    }

    @Operation(summary = "تحديث مادة تعليمية", description = "تحديث بيانات المادة (الرابط – الاسم – المعاينة – المدة)")
    @PutMapping("/{id}")
    public ResponseEntity<LessonMaterialResponse> updateMaterial(
            @PathVariable Long id,
            @Validated @RequestBody LessonMaterialUpdateRequest request) {
        log.info("API /api/materials/{} [PUT] called", id);
        return ResponseEntity.ok(materialService.updateMaterial(id, request));
    }

    @Operation(summary = "حذف مادة تعليمية", description = "حذف مادة تعليمية نهائيًا")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public ResponseEntity<Void> deleteMaterial(@PathVariable Long id) {
        log.info("API /api/materials/{} [DELETE] called", id);
        materialService.deleteMaterial(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "جلب مادة تعليمية", description = "إرجاع تفاصيل مادة تعليمية واحدة")
    @GetMapping("/{id}")
    public ResponseEntity<LessonMaterialResponse> getMaterialById(
            @PathVariable Long id,
            @AuthenticationPrincipal JwtUserPrincipal principal,
            HttpServletRequest httpRequest) {
        log.info("API /api/materials/{} [GET] called", id);

        // فحص الاشتراك للطلاب: المادة تتحقق عبر AccessService من خلال lessonId → courseId
        if (principal != null && "STUDENT".equals(principal.getRole())) {
            if (!accessService.canAccessMaterial(principal.getUserId(), id)) {
                return ResponseEntity.status(403).build();
            }
            // فحص LOCK_BY_ELEMENT: هل المادة السابقة اتشيّفت؟
            LessonGateService.AccessCheckResult gateResult =
                    lessonGateService.checkMaterialAccess(principal.getUserId(), id);
            if (!gateResult.isAllowed()) {
                return ResponseEntity.status(403).build();
            }
            // سجّل المشاهدة لفتح العنصر التالي + يحدّث نسبة التقدم في الكورس
            lessonGateService.markMaterialViewed(principal.getUserId(), id);
            // سجّل حضور أونلاين تلقائياً لأسبوع المادة دي — كان مش بيحصل لما الطالب يفتح فيديو مباشرة
            registerOnlineAttendanceForMaterial(principal.getUserId(), id, httpRequest);
        }

        return ResponseEntity.ok(materialService.getMaterialById(id));
    }

    @Operation(summary = "تسجيل تقدّم مشاهدة فيديو", description = "يستخدمها تطبيق الطالب لتحديث مدة مشاهدة الفيديو دورياً، فيُعاد احتساب التقدم في الكورس بناءً على طول الفيديو ومدة المشاهدة (الملفات لا تُحتسب)")
    @PostMapping("/{id}/watch-progress")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<GlobalResponse<Void>> recordWatchProgress(
            @PathVariable Long id,
            @RequestParam Long watchedSeconds,
            @AuthenticationPrincipal JwtUserPrincipal principal,
            HttpServletRequest httpRequest) {

        if (!accessService.canAccessMaterial(principal.getUserId(), id)) {
            return ResponseEntity.status(403).build();
        }

        lessonGateService.markMaterialViewed(principal.getUserId(), id, watchedSeconds);
        registerOnlineAttendanceForMaterial(principal.getUserId(), id, httpRequest);

        return ResponseEntity.ok(GlobalResponse.<Void>builder()
                .success(true)
                .message("تم تحديث تقدّم المشاهدة")
                .build());
    }

    @Operation(summary = "جلب مواد درس", description = "إرجاع كل المواد التابعة لدرس معين مع Pagination")
    @GetMapping("/lesson/{lessonId}")
    public ResponseEntity<Page<LessonMaterialResponse>> getMaterialsByWeek(
            @PathVariable Long lessonId,
            Pageable pageable,
            @AuthenticationPrincipal JwtUserPrincipal principal) {
        log.info("API /api/materials/lesson/{} [GET] called with pagination", lessonId);

        return ResponseEntity.ok(materialService.getMaterialsByWeek(lessonId, pageable));
    }

// ================= REORDER MATERIALS =================

    @Operation(summary = "إعادة ترتيب المواد", description = "تحديث orderNumber لقائمة من المواد")
    @PutMapping("/reorder")
    public ResponseEntity<GlobalResponse<Void>> reorderMaterials(@RequestBody ReorderRequest request) {
        log.info("PUT /api/materials/reorder — {} items", request.getItems() != null ? request.getItems().size() : 0);
        materialService.reorderMaterials(request);
        return ResponseEntity.ok(GlobalResponse.<Void>builder()
                .success(true)
                .message("تم حفظ الترتيب بنجاح")
                .build());
    }

// ================= TOGGLE STATUS (The new method) =================

    @Operation(summary = "Toggle material activation", description = "إظهار أو إخفاء ملف/فيديو معين للطلاب")
    @PatchMapping("/{id}/toggle-status")
    public ResponseEntity<GlobalResponse<Void>> toggleStatus(@PathVariable Long id) {
        log.info("PATCH /api/materials/{}/toggle-status", id);

        materialService.toggleMaterialStatus(id);

        return ResponseEntity.ok(GlobalResponse.<Void>builder()
                .success(true)
                .message("تم تغيير حالة الملف بنجاح")
                .build());
    }
}
