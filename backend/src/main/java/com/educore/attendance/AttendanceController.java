package com.educore.attendance;
import com.educore.common.GlobalResponse;

import com.educore.security.JwtUserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
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

@Slf4j
@RestController
@RequestMapping("/api/attendance")
@RequiredArgsConstructor
@Tag(name = "Attendance", description = "تسجيل و��تابعة حضور الطلاب — سنتر وأونلاين")
public class AttendanceController {

    private final AttendanceService attendanceService;

    // ─────────────────────────────────────────────────────────────
    // Scan QR في السنتر — الموظف يعمل Scan لكار��يه الطالب
    // ─────────────────────────────────────────────────────────────

    @Operation(
        summary     = "[TEACHER] مسح QR لتسجيل الحضور في السنتر",
        description = "الموظف يسكان QR الطالب ويحدد ID الحصة → يسجل حضور تلقائياً"
    )
    @PostMapping("/scan")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public ResponseEntity<GlobalResponse<AttendanceResponse>> scanAttendance(
            @Valid @RequestBody AttendanceScanRequest request,
            @AuthenticationPrincipal JwtUserPrincipal principal) {

        AttendanceResponse response = attendanceService.scanQrAttendance(
                request, principal.getUsername());

        return ResponseEntity.ok(GlobalResponse.<AttendanceResponse>builder()
                .success(true)
                .message("تم تسجيل الحضور بنجاح ✓")
                .data(response)
                .build());
    }

    // ─────────────────────────────────────────────────────────────
    // حضور أونلاين — الطالب يفتح الحصة
    // ─────────────────────────────────────────────────────────────

    @Operation(
        summary     = "[STUDENT] تسجيل دخول لحصة أونلاين",
        description = "يُستدعى تلقائياً ل��ا الطالب يضغط على الحصة — يتحقق من الـ Gate ويسجل الحضور"
    )
    @PostMapping("/online/{weekId}")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<GlobalResponse<AttendanceResponse>> recordOnlineAttendance(
            @PathVariable Long weekId,
            @AuthenticationPrincipal JwtUserPrincipal principal,
            HttpServletRequest request) {

        AttendanceResponse response = attendanceService.recordOnlineAttendance(
                principal.getUserId(), weekId, request);

        return ResponseEntity.ok(GlobalResponse.<AttendanceResponse>builder()
                .success(true)
                .message("تم تسجيل الدخول للحصة")
                .data(response)
                .build());
    }

    // ─────────────────────────────────────────────────────────────
    // سجل حضوري — الطالب يشوف تاريخ حضوره
    // ─────────────────────────────────────────────────────────────

    @Operation(summary = "[STUDENT] سجل حضوري")
    @GetMapping("/my")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<GlobalResponse<Page<AttendanceResponse>>> getMyAttendance(
            @AuthenticationPrincipal JwtUserPrincipal principal,
            @PageableDefault(size = 20, sort = "attendedAt", direction = Sort.Direction.DESC)
            Pageable pageable) {

        Page<AttendanceResponse> page = attendanceService.getMyAttendance(
                principal.getUserId(), pageable);

        return ResponseEntity.ok(GlobalResponse.<Page<AttendanceResponse>>builder()
                .success(true)
                .message("تم جلب سجل الحضور")
                .data(page)
                .build());
    }

    // ─────────────────────────────────────────────────────────────
    // حضور طالب معين — للمدرس والأدم��
    // ─────────────────────────────────────────────────────────────

    @Operation(summary = "[ADMIN/TEACHER] سجل حضور طالب معين")
    @GetMapping("/student/{studentId}")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public ResponseEntity<GlobalResponse<Page<AttendanceResponse>>> getStudentAttendance(
            @PathVariable Long studentId,
            @PageableDefault(size = 20, sort = "attendedAt", direction = Sort.Direction.DESC)
            Pageable pageable) {

        Page<AttendanceResponse> page = attendanceService.getStudentAttendance(studentId, pageable);

        return ResponseEntity.ok(GlobalResponse.<Page<AttendanceResponse>>builder()
                .success(true)
                .message("تم جلب سجل الحضور")
                .data(page)
                .build());
    }

    // ─────────────────────────────────────────────────────────────
    // حضور حصة معينة — من حضر هذا الدرس؟
    // ─────────────────────────────────────────────────────────────

    @Operation(summary = "[ADMIN/TEACHER] من حضر هذه الحصة؟")
    @GetMapping("/lesson/{weekId}")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public ResponseEntity<GlobalResponse<Page<AttendanceResponse>>> getLessonAttendance(
            @PathVariable Long weekId,
            @PageableDefault(size = 50, sort = "attendedAt", direction = Sort.Direction.ASC)
            Pageable pageable) {

        Page<AttendanceResponse> page = attendanceService.getLessonAttendance(weekId, pageable);

        return ResponseEntity.ok(GlobalResponse.<Page<AttendanceResponse>>builder()
                .success(true)
                .message("تم جلب حضور الحصة")
                .data(page)
                .build());
    }
}
