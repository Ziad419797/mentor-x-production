package com.educore.teacher;

import com.educore.common.GlobalResponse;
import com.educore.dto.mapper.StudentMapper;
import com.educore.dto.request.RejectRequest;
import com.educore.dto.response.StudentResponse;
import com.educore.exception.ResourceNotFoundException;
import com.educore.student.StudentRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.Map;

@RestController
@RequestMapping("/api/teacher/students")
@RequiredArgsConstructor
@lombok.extern.slf4j.Slf4j
@Tag(name = "Teacher - Student Management", description = "إدارة الطلاب من قبل المعلم (قبول/رفض/عرض)")
public class TeacherStudentController {

    private final TeacherStudentService teacherStudentService;
    private final StudentRepository     studentRepository;
    private final StudentMapper         studentMapper;

    @Operation(summary = "عرض الطلاب المنتظرين")
    @GetMapping("/pending")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN') or @perm.can(authentication,'STUDENTS_MANAGE')")
    public ResponseEntity<GlobalResponse<Page<StudentResponse>>> getPending(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String grade) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<StudentResponse> result = grade != null && !grade.isBlank()
                ? teacherStudentService.getPendingStudentsByGrade(grade, pageable)
                : teacherStudentService.getPendingStudents(pageable);
        return ResponseEntity.ok(GlobalResponse.success("تم جلب قائمة الطلاب المنتظرين", result));
    }

    @Operation(summary = "قبول تفعيل حساب طالب")
    @PostMapping("/{id}/approve")
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN') or @perm.can(authentication,'NEW_REQUESTS')")
    public GlobalResponse<Void> approve(@PathVariable Long id, Principal principal) {
        teacherStudentService.approveStudent(id, principal.getName());
        return GlobalResponse.success("تم تفعيل حساب الطالب بنجاح", null);
    }

    @Operation(summary = "تعديل بيانات طالب", description = "تحديث بيانات الطالب (الاسم، الهاتف، المحافظة، الصف، السنتر، الجروب، الصور، كلمة المرور...الخ) من لوحة المعلم")
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN') or @perm.can(authentication,'EDIT_STUDENTS')")
    public ResponseEntity<GlobalResponse<Void>> update(@PathVariable Long id, @RequestBody Map<String, Object> updates) {
        try {
            teacherStudentService.updateStudent(id, updates);
            return ResponseEntity.ok(GlobalResponse.success("تم تحديث بيانات الطالب بنجاح", null));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(GlobalResponse.<Void>builder().success(false).message(e.getMessage()).build());
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            // غالباً رقم هاتف الطالب أو ولي الأمر مستخدم بالفعل لطالب آخر
            log.warn("Duplicate/constraint violation while updating student {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(GlobalResponse.<Void>builder().success(false)
                            .message("تعذر حفظ التعديلات: رقم الهاتف مستخدم بالفعل لحساب آخر")
                            .build());
        } catch (Exception e) {
            log.error("Failed to update student {}", id, e);
            return ResponseEntity.internalServerError()
                    .body(GlobalResponse.<Void>builder().success(false)
                            .message("حدث خطأ غير متوقع أثناء حفظ التعديلات: " + e.getMessage())
                            .build());
        }
    }

    @Operation(summary = "رفض حساب طالب")
    @PostMapping("/{id}/reject")
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN') or @perm.can(authentication,'STUDENTS_MANAGE')")
    public GlobalResponse<Void> reject(
            @PathVariable Long id,
            @Valid @RequestBody RejectRequest request,
            Principal principal) {
        String reason = request.getReason() != null ? request.getReason() : "لم يتم ذكر سبب";
        teacherStudentService.rejectStudent(id, reason, principal.getName());
        return GlobalResponse.success("تم رفض الحساب وإخطار الطالب", null);
    }

    @Operation(summary = "عرض الطلاب النشطين")
    @GetMapping("/active")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN') or @perm.can(authentication,'STUDENTS_MANAGE')")
    public ResponseEntity<GlobalResponse<Page<StudentResponse>>> getActive(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String grade) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<StudentResponse> result = grade != null && !grade.isBlank()
                ? teacherStudentService.getActiveStudentsByGrade(grade, pageable)
                : teacherStudentService.getActiveStudents(pageable);
        return ResponseEntity.ok(GlobalResponse.success("تم جلب قائمة الطلاب النشطين", result));
    }

    @Operation(summary = "حظر حساب طالب")
    @PostMapping("/{id}/block")
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public GlobalResponse<Void> blockStudent(
            @PathVariable Long id,
            @RequestBody Map<String, String> request,
            Principal principal) {
        String reason = request.getOrDefault("reason", "لم يتم ذكر سبب");
        teacherStudentService.blockStudent(id, reason, principal.getName());
        return GlobalResponse.success("تم حظر حساب الطالب بنجاح", null);
    }

    @Operation(summary = "إلغاء حظر حساب طالب")
    @PostMapping("/{id}/unblock")
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public GlobalResponse<Void> unblockStudent(@PathVariable Long id, Principal principal) {
        teacherStudentService.unblockStudent(id, principal.getName());
        return GlobalResponse.success("تم إلغاء حظر حساب الطالب بنجاح", null);
    }

    @Operation(summary = "عرض الطلاب المحظورين")
    @GetMapping("/blocked")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public ResponseEntity<GlobalResponse<Page<StudentResponse>>> getBlocked(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String grade) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<StudentResponse> result = grade != null && !grade.isBlank()
                ? teacherStudentService.getBlockedStudentsByGrade(grade, pageable)
                : teacherStudentService.getBlockedStudents(pageable);
        return ResponseEntity.ok(GlobalResponse.success("تم جلب قائمة الطلاب المحظورين", result));
    }

    @Operation(summary = "عرض الطلاب المرفوضين")
    @GetMapping("/rejected")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN') or @perm.can(authentication,'STUDENTS_MANAGE')")
    public ResponseEntity<GlobalResponse<Page<StudentResponse>>> getRejected(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(GlobalResponse.success("تم جلب قائمة الطلاب المرفوضين",
                teacherStudentService.getRejectedStudents(pageable)));
    }

    @Operation(summary = "مسح الجهاز المسجل للطالب")
    @PostMapping("/{id}/clear-device")
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public GlobalResponse<Void> clearDevice(@PathVariable Long id) {
        teacherStudentService.clearStudentDevice(id);
        return GlobalResponse.success("تم مسح الجهاز المسجل للطالب بنجاح", null);
    }

    // ─────────────────────────────────────────────────────────────
    //  ID Verification Endpoints
    // ─────────────────────────────────────────────────────────────

    @Operation(summary = "تحقق من هوية الطالب عبر موديل الـ AI",
               description = "بيبعت صورة بطاقة الطالب للـ ID-Verify service ويرجع نتيجة التحليل والتحقق")
    @PostMapping("/{id}/verify-id")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN') or @perm.can(authentication,'NEW_REQUESTS')")
    public ResponseEntity<GlobalResponse<StudentResponse>> verifyStudentId(@PathVariable Long id) {
        try {
            StudentResponse result = teacherStudentService.verifyStudentId(id);
            return ResponseEntity.ok(GlobalResponse.success("تم التحقق من الهوية بنجاح", result));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest()
                    .body(GlobalResponse.error(e.getMessage()));
        }
    }

    @Operation(summary = "جلب نتيجة التحقق المخزنة للطالب")
    @GetMapping("/{id}/verify-id")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN') or @perm.can(authentication,'NEW_REQUESTS')")
    public ResponseEntity<GlobalResponse<StudentResponse>> getIdVerificationResult(@PathVariable Long id) {
        StudentResponse result = teacherStudentService.getIdVerificationResult(id);
        return ResponseEntity.ok(GlobalResponse.success("تم جلب نتيجة التحقق", result));
    }
}
