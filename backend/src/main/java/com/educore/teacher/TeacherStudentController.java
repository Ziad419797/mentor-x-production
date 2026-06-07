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
@Tag(name = "Teacher - Student Management", description = "إدارة الطلاب من قبل المعلم (قبول/رفض/عرض)")
public class TeacherStudentController {

    private final TeacherStudentService teacherStudentService;
    private final StudentRepository     studentRepository;
    private final StudentMapper         studentMapper;

    @Operation(summary = "عرض الطلاب المنتظرين")
    @GetMapping("/pending")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN') or @perm.can(authentication,'VIEW_STUDENTS')")
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
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN') or @perm.can(authentication,'APPROVE_STUDENTS')")
    public GlobalResponse<Void> approve(@PathVariable Long id, Principal principal) {
        teacherStudentService.approveStudent(id, principal.getName());
        return GlobalResponse.success("تم تفعيل حساب الطالب بنجاح", null);
    }

    @Operation(summary = "رفض حساب طالب")
    @PostMapping("/{id}/reject")
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN') or @perm.can(authentication,'REJECT_STUDENTS')")
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
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN') or @perm.can(authentication,'VIEW_STUDENTS')")
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
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public ResponseEntity<GlobalResponse<Page<StudentResponse>>> getRejected(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<StudentResponse> result = teacherStudentService.getRejectedStudents(pageable);
        return ResponseEntity.ok(GlobalResponse.success("تم جلب قائمة الطلاب المرفوضين", result));
    }

    @Operation(summary = "البحث عن طالب برقم الهاتف")
    @GetMapping("/by-phone/{phone}")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public ResponseEntity<GlobalResponse<StudentResponse>> getByPhone(@PathVariable String phone) {
        StudentResponse student = studentRepository.findByPhone(phone)
                .map(studentMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("لا يوجد طالب بهذا الرقم: " + phone));
        return ResponseEntity.ok(GlobalResponse.success(student));
    }

    @Operation(summary = "البحث عن طالب بكود الطالب")
    @GetMapping("/by-code/{studentCode}")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public ResponseEntity<GlobalResponse<StudentResponse>> getByStudentCode(@PathVariable String studentCode) {
        StudentResponse student = studentRepository.findByStudentCode(studentCode)
                .map(studentMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("لا يوجد طالب بهذا الكود: " + studentCode));
        return ResponseEntity.ok(GlobalResponse.success(student));
    }
}
