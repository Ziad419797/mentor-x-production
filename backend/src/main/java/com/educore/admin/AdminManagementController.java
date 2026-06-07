package com.educore.admin;

import com.educore.common.GlobalResponse;
import com.educore.dto.request.RejectRequest;
import com.educore.security.JwtUserPrincipal;
import com.educore.student.Student;
import com.educore.student.StudentStatus;
import com.educore.teacher.Teacher;
import io.swagger.v3.oas.annotations.Operation;
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

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin Management", description = "لوحة تحكم الأدمن — إدارة الطلاب والمعلمين")
public class AdminManagementController {

    private final AdminManagementService adminManagementService;

    /* ════════════════════════════════════════════════════
       DASHBOARD
    ════════════════════════════════════════════════════ */

    @Operation(summary = "إحصائيات لوحة التحكم")
    @GetMapping("/stats")
    public ResponseEntity<GlobalResponse<Map<String, Long>>> getStats() {
        return ResponseEntity.ok(GlobalResponse.<Map<String, Long>>builder()
                .success(true)
                .message("تم جلب الإحصائيات")
                .data(adminManagementService.getDashboardStats())
                .build());
    }

    /* ════════════════════════════════════════════════════
       STUDENTS
    ════════════════════════════════════════════════════ */

    @Operation(summary = "قائمة الطلاب", description = "يمكن الفلترة بـ status=PENDING/ACTIVE/BLOCKED/REJECTED")
    @GetMapping("/students")
    public ResponseEntity<GlobalResponse<Page<StudentAdminDto>>> getStudents(
            @RequestParam(required = false) StudentStatus status,
            @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {

        Page<StudentAdminDto> students = adminManagementService.getAllStudents(status, pageable)
                .map(StudentAdminDto::from);
        return ResponseEntity.ok(GlobalResponse.<Page<StudentAdminDto>>builder()
                .success(true).message("تم جلب الطلاب").data(students).build());
    }

    @Operation(summary = "تفاصيل طالب بالـ ID")
    @GetMapping("/students/{id}")
    public ResponseEntity<GlobalResponse<StudentAdminDto>> getStudent(@PathVariable Long id) {
        StudentAdminDto dto = StudentAdminDto.from(adminManagementService.getStudentById(id));
        return ResponseEntity.ok(GlobalResponse.<StudentAdminDto>builder()
                .success(true).message("تم جلب بيانات الطالب").data(dto).build());
    }

    @Operation(summary = "الموافقة على طالب PENDING")
    @PostMapping("/students/{id}/approve")
    public ResponseEntity<GlobalResponse<StudentAdminDto>> approveStudent(
            @PathVariable Long id,
            @AuthenticationPrincipal JwtUserPrincipal principal) {

        Student student = adminManagementService.approveStudent(id, "Admin#" + principal.getUserId());
        return ResponseEntity.ok(GlobalResponse.<StudentAdminDto>builder()
                .success(true).message("تمت الموافقة على الطالب")
                .data(StudentAdminDto.from(student)).build());
    }

    @Operation(summary = "رفض طالب")
    @PostMapping("/students/{id}/reject")
    public ResponseEntity<GlobalResponse<StudentAdminDto>> rejectStudent(
            @PathVariable Long id,
            @RequestBody @Valid RejectRequest rejectRequest,
            @AuthenticationPrincipal JwtUserPrincipal principal) {

        Student student = adminManagementService.rejectStudent(
                id, "Admin#" + principal.getUserId(), rejectRequest.getReason());
        return ResponseEntity.ok(GlobalResponse.<StudentAdminDto>builder()
                .success(true).message("تم رفض الطالب")
                .data(StudentAdminDto.from(student)).build());
    }

    @Operation(summary = "حظر طالب")
    @PostMapping("/students/{id}/block")
    public ResponseEntity<GlobalResponse<StudentAdminDto>> blockStudent(
            @PathVariable Long id,
            @AuthenticationPrincipal JwtUserPrincipal principal) {

        Student student = adminManagementService.blockStudent(id, "Admin#" + principal.getUserId());
        return ResponseEntity.ok(GlobalResponse.<StudentAdminDto>builder()
                .success(true).message("تم حظر الطالب")
                .data(StudentAdminDto.from(student)).build());
    }

    @Operation(summary = "رفع الحظر عن طالب")
    @PostMapping("/students/{id}/unblock")
    public ResponseEntity<GlobalResponse<StudentAdminDto>> unblockStudent(
            @PathVariable Long id,
            @AuthenticationPrincipal JwtUserPrincipal principal) {

        Student student = adminManagementService.unblockStudent(id, "Admin#" + principal.getUserId());
        return ResponseEntity.ok(GlobalResponse.<StudentAdminDto>builder()
                .success(true).message("تم رفع الحظر عن الطالب")
                .data(StudentAdminDto.from(student)).build());
    }

    /* ════════════════════════════════════════════════════
       TEACHERS
    ════════════════════════════════════════════════════ */

    @Operation(summary = "قائمة المعلمين")
    @GetMapping("/teachers")
    public ResponseEntity<GlobalResponse<Page<Teacher>>> getTeachers(
            @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {

        Page<Teacher> teachers = adminManagementService.getAllTeachers(pageable);
        return ResponseEntity.ok(GlobalResponse.<Page<Teacher>>builder()
                .success(true).message("تم جلب المعلمين").data(teachers).build());
    }

    @Operation(summary = "تفاصيل معلم بالـ ID")
    @GetMapping("/teachers/{id}")
    public ResponseEntity<GlobalResponse<Teacher>> getTeacher(@PathVariable Long id) {
        return ResponseEntity.ok(GlobalResponse.<Teacher>builder()
                .success(true).message("تم جلب بيانات المعلم")
                .data(adminManagementService.getTeacherById(id)).build());
    }

    @Operation(summary = "تفعيل حساب معلم")
    @PostMapping("/teachers/{id}/enable")
    public ResponseEntity<GlobalResponse<Teacher>> enableTeacher(
            @PathVariable Long id,
            @AuthenticationPrincipal JwtUserPrincipal principal) {

        Teacher teacher = adminManagementService.enableTeacher(id, "Admin#" + principal.getUserId());
        return ResponseEntity.ok(GlobalResponse.<Teacher>builder()
                .success(true).message("تم تفعيل حساب المعلم").data(teacher).build());
    }

    @Operation(summary = "تعطيل حساب معلم")
    @PostMapping("/teachers/{id}/disable")
    public ResponseEntity<GlobalResponse<Teacher>> disableTeacher(
            @PathVariable Long id,
            @AuthenticationPrincipal JwtUserPrincipal principal) {

        Teacher teacher = adminManagementService.disableTeacher(id, "Admin#" + principal.getUserId());
        return ResponseEntity.ok(GlobalResponse.<Teacher>builder()
                .success(true).message("تم تعطيل حساب المعلم").data(teacher).build());
    }
}
