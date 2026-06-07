package com.educore.attendance.group;

import com.educore.attendance.group.dto.request.*;
import com.educore.attendance.group.dto.response.*;
import com.educore.security.JwtUserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.Optional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * REST endpoints لنظام حضور السنتر.
 *
 * Teacher endpoints: /api/attendance/groups/**
 * Student endpoints: /api/attendance/student/**
 */
@RestController
@RequestMapping("/api/attendance")
@RequiredArgsConstructor
public class AttendanceGroupController {

    private final AttendanceGroupService groupService;
    private final AttendanceExportService exportService;

    // ═══════════════════════════════════════════════════════════════
    // TEACHER — GROUP CRUD
    // ═══════════════════════════════════════════════════════════════

    /**
     * POST /api/attendance/groups
     * إنشاء جروب جديد.
     */
    @PostMapping("/groups")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<GroupResponse> createGroup(
            @AuthenticationPrincipal JwtUserPrincipal principal,
            @Valid @RequestBody CreateGroupRequest req) {

        GroupResponse response = groupService.createGroup(principal.getUserId(), req);
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/attendance/groups
     * جروباتي كلها.
     */
    @GetMapping("/groups")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<List<GroupResponse>> getMyGroups(
            @AuthenticationPrincipal JwtUserPrincipal principal,
            @RequestParam(required = false) Long levelId) {

        if (levelId != null) {
            return ResponseEntity.ok(groupService.getMyGroupsByLevel(principal.getUserId(), levelId));
        }
        return ResponseEntity.ok(groupService.getMyGroups(principal.getUserId()));
    }

    /**
     * GET /api/attendance/groups/{groupId}
     * تفاصيل جروب واحد.
     */
    @GetMapping("/groups/{groupId}")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<GroupResponse> getGroup(
            @AuthenticationPrincipal JwtUserPrincipal principal,
            @PathVariable Long groupId) {

        return ResponseEntity.ok(groupService.getGroup(principal.getUserId(), groupId));
    }


    /**
     * PUT /api/attendance/groups/{groupId}
     * تعديل جروب.
     */
    @PutMapping("/groups/{groupId}")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<GroupResponse> updateGroup(
            @AuthenticationPrincipal JwtUserPrincipal principal,
            @PathVariable Long groupId,
            @Valid @RequestBody CreateGroupRequest req) {

        GroupResponse response = groupService.updateGroup(principal.getUserId(), groupId, req);
        return ResponseEntity.ok(response);
    }
    /**
     * DELETE /api/attendance/groups/{groupId}
     * حذف ناعم للجروب.
     */
    @DeleteMapping("/groups/{groupId}")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<Void> deleteGroup(
            @AuthenticationPrincipal JwtUserPrincipal principal,
            @PathVariable Long groupId) {

        groupService.deleteGroup(principal.getUserId(), groupId);
        return ResponseEntity.noContent().build();
    }

    // ═══════════════════════════════════════════════════════════════
    // TEACHER — MEMBER MANAGEMENT
    // ═══════════════════════════════════════════════════════════════

    /**
     * POST /api/attendance/groups/{groupId}/members
     * إضافة طالب للجروب.
     */
    @PostMapping("/groups/{groupId}/members")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<GroupMemberResponse> addStudent(
            @AuthenticationPrincipal JwtUserPrincipal principal,
            @PathVariable Long groupId,
            @RequestBody AddStudentRequest req) {

        return ResponseEntity.ok(groupService.addStudent(principal.getUserId(), groupId, req));
    }

    /**
     * DELETE /api/attendance/groups/{groupId}/members/{studentId}
     * إزالة طالب من الجروب.
     */
    @DeleteMapping("/groups/{groupId}/members/{studentId}")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<Void> removeStudent(
            @AuthenticationPrincipal JwtUserPrincipal principal,
            @PathVariable Long groupId,
            @PathVariable Long studentId) {

        groupService.removeStudent(principal.getUserId(), groupId, studentId);
        return ResponseEntity.noContent().build();
    }

    /**
     * GET /api/attendance/groups/{groupId}/members
     * قائمة أعضاء الجروب.
     */
    @GetMapping("/groups/{groupId}/members")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<List<GroupMemberResponse>> listMembers(
            @AuthenticationPrincipal JwtUserPrincipal principal,
            @PathVariable Long groupId) {

        return ResponseEntity.ok(groupService.listMembers(principal.getUserId(), groupId));
    }

    // ═══════════════════════════════════════════════════════════════
    // TEACHER — SESSION MANAGEMENT
    // ═══════════════════════════════════════════════════════════════

    /**
     * POST /api/attendance/groups/{groupId}/sessions
     * إنشاء حصة جديدة.
     */
    @PostMapping("/groups/{groupId}/sessions")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<SessionResponse> createSession(
            @AuthenticationPrincipal JwtUserPrincipal principal,
            @PathVariable Long groupId,
            @Valid @RequestBody CreateSessionRequest req) {

        return ResponseEntity.ok(groupService.createSession(principal.getUserId(), groupId, req));
    }

    /**
     * GET /api/attendance/groups/{groupId}/sessions
     * قائمة الحصص في جروب.
     */
    @GetMapping("/groups/{groupId}/sessions")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<List<SessionResponse>> listSessions(
            @AuthenticationPrincipal JwtUserPrincipal principal,
            @PathVariable Long groupId) {

        return ResponseEntity.ok(groupService.listSessions(principal.getUserId(), groupId));
    }

    /**
     * POST /api/attendance/sessions/{sessionId}/open
     * فتح الحصة لتسجيل الحضور.
     */
    @PostMapping("/sessions/{sessionId}/open")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<SessionResponse> openSession(
            @AuthenticationPrincipal JwtUserPrincipal principal,
            @PathVariable Long sessionId) {

        return ResponseEntity.ok(groupService.openSession(principal.getUserId(), sessionId));
    }

    /**
     * POST /api/attendance/sessions/{sessionId}/close
     * إغلاق الحصة + تسجيل غياب تلقائي.
     */
    @PostMapping("/sessions/{sessionId}/close")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<SessionResponse> closeSession(
            @AuthenticationPrincipal JwtUserPrincipal principal,
            @PathVariable Long sessionId) {

        return ResponseEntity.ok(groupService.closeSession(principal.getUserId(), sessionId));
    }

    // ═══════════════════════════════════════════════════════════════
    // TEACHER — MARK ATTENDANCE
    // ═══════════════════════════════════════════════════════════════

    /**
     * POST /api/attendance/sessions/{sessionId}/mark
     * تسجيل حضور طالب (QR scan أو ID يدوي).
     */
    @PostMapping("/sessions/{sessionId}/mark")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<MarkResult> markAttendance(
            @AuthenticationPrincipal JwtUserPrincipal principal,
            @PathVariable Long sessionId,
            @RequestBody MarkAttendanceRequest req) {

        return ResponseEntity.ok(groupService.markAttendance(principal.getUserId(), sessionId, req));
    }

    /**
     * GET /api/attendance/sessions/{sessionId}/records
     * كل سجلات حصة معينة.
     */
    @GetMapping("/sessions/{sessionId}/records")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<List<RecordResponse>> getSessionRecords(
            @AuthenticationPrincipal JwtUserPrincipal principal,
            @PathVariable Long sessionId) {

        return ResponseEntity.ok(groupService.getSessionRecords(principal.getUserId(), sessionId));
    }

    /**
     * PATCH /api/attendance/records/{recordId}/status
     * تعديل حالة طالب (تصحيح خطأ).
     * Body: { "status": "LATE" }
     */
    @PatchMapping("/records/{recordId}/status")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<RecordResponse> updateRecordStatus(
            @AuthenticationPrincipal JwtUserPrincipal principal,
            @PathVariable Long recordId,
            @RequestParam AttendanceStatus status) {

        return ResponseEntity.ok(
                groupService.updateRecordStatus(principal.getUserId(), recordId, status));
    }

    // ═══════════════════════════════════════════════════════════════
    // TEACHER — COMMENTS
    // ═══════════════════════════════════════════════════════════════

    /**
     * POST /api/attendance/records/{recordId}/comment
     * إضافة / تعديل تعليق المدرس.
     */
    @PostMapping("/records/{recordId}/comment")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<RecordResponse> addComment(
            @AuthenticationPrincipal JwtUserPrincipal principal,
            @PathVariable Long recordId,
            @Valid @RequestBody CommentRequest req) {

        return ResponseEntity.ok(groupService.addComment(principal.getUserId(), recordId, req));
    }

    // ═══════════════════════════════════════════════════════════════
    // TEACHER — EXPORT
    // ═══════════════════════════════════════════════════════════════

    /**
     * GET /api/attendance/sessions/{sessionId}/export?filter=ALL
     * تصدير حصة معينة كـ Excel.
     * filter: ALL | PRESENT | ABSENT | LATE | EXCUSED
     */
    @GetMapping("/sessions/{sessionId}/export")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<byte[]> exportSession(
            @AuthenticationPrincipal JwtUserPrincipal principal,
            @PathVariable Long sessionId,
            @RequestParam(defaultValue = "ALL") String filter) {

        // تحقق أن الحصة تابعة للمدرس ده عبر جلب السجلات (الـ service يتحقق)
        groupService.getSessionRecords(principal.getUserId(), sessionId);

        byte[] bytes = exportService.exportSession(sessionId, filter);

        String filename = "attendance_session_" + sessionId + "_" + filter.toLowerCase()
                + "_" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
                + ".xlsx";

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(filename).build().toString())
                .body(bytes);
    }

    /**
     * GET /api/attendance/groups/{groupId}/export
     * تصدير كامل الجروب — ورقة لكل حصة.
     */
    @GetMapping("/groups/{groupId}/export")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<byte[]> exportGroup(
            @AuthenticationPrincipal JwtUserPrincipal principal,
            @PathVariable Long groupId) {

        // تحقق من الملكية
        groupService.getGroup(principal.getUserId(), groupId);

        byte[] bytes = exportService.exportGroup(groupId);

        String filename = "attendance_group_" + groupId
                + "_" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
                + ".xlsx";

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(filename).build().toString())
                .body(bytes);
    }

    // ═══════════════════════════════════════════════════════════════
    // STUDENT — BRIEF & MY GROUPS
    // ═══════════════════════════════════════════════════════════════

    /**
     * GET /api/attendance/student/groups
     * كل جروبات الطالب + ملخص حضور لكل جروب.
     */
    @GetMapping("/student/groups")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<List<StudentBriefResponse>> getMyGroupsBrief(
            @AuthenticationPrincipal JwtUserPrincipal principal) {

        return ResponseEntity.ok(groupService.getMyGroupsBrief(principal.getUserId()));
    }

    /**
     * GET /api/attendance/student/groups/{groupId}
     * ملخص الطالب في جروب معين (حضور + غياب + تعليقات + درجات).
     */
    @GetMapping("/student/groups/{groupId}")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<StudentBriefResponse> getMyGroupBrief(
            @AuthenticationPrincipal JwtUserPrincipal principal,
            @PathVariable Long groupId) {

        return ResponseEntity.ok(
                groupService.getStudentBrief(principal.getUserId(), groupId));
    }

    // ═══════════════════════════════════════════════════════════════
    // PARENT — STUDENT BRIEF
    // ═══════════════════════════════════════════════════════════════

    /**
     * GET /api/attendance/parent/students/{studentId}/groups
     * كل جروبات ابن ولي الأمر.
     * ملاحظة: التحقق من ملكية ولي الأمر للطالب يتم في الـ SecurityConfig أو يُضاف هنا.
     */
    @GetMapping("/parent/students/{studentId}/groups")
    @PreAuthorize("hasRole('PARENT')")
    public ResponseEntity<List<StudentBriefResponse>> getStudentGroupsBriefForParent(
            @AuthenticationPrincipal JwtUserPrincipal principal,
            @PathVariable Long studentId) {

        // TODO: أضف تحقق أن الطالب تابع لولي الأمر ده لو موجود في الـ service
        return ResponseEntity.ok(groupService.getMyGroupsBrief(studentId));
    }

    /**
     * GET /api/attendance/parent/students/{studentId}/groups/{groupId}
     * ملخص الطالب في جروب معين من منظور ولي الأمر.
     */
    @GetMapping("/parent/students/{studentId}/groups/{groupId}")
    @PreAuthorize("hasRole('PARENT')")
    public ResponseEntity<StudentBriefResponse> getStudentGroupBriefForParent(
            @AuthenticationPrincipal JwtUserPrincipal principal,
            @PathVariable Long studentId,
            @PathVariable Long groupId) {

        return ResponseEntity.ok(groupService.getStudentBrief(studentId, groupId));
    }
}
