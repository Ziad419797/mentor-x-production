package com.educore.attendance.group;

import com.educore.attendance.group.dto.request.*;
import com.educore.attendance.group.dto.response.*;
import com.educore.center.Center;
import com.educore.center.CenterRepository;
import com.educore.student.Student;
import com.educore.student.StudentRepository;
import com.educore.studentcard.StudentCard;
import com.educore.studentcard.StudentCardRepository;
import com.educore.teacher.Teacher;
import com.educore.teacher.TeacherRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AttendanceGroupService {

    private final AttendanceGroupRepository         groupRepo;
    private final AttendanceGroupMemberRepository   memberRepo;
    private final AttendanceGroupSessionRepository  sessionRepo;
    private final AttendanceGroupRecordRepository   recordRepo;
    private final TeacherRepository                 teacherRepo;
    private final StudentRepository                 studentRepo;
    private final StudentCardRepository             cardRepo;
    private final CenterRepository                  centerRepo;

    // ═══════════════════════════════════════════════════════════════
    // GROUP CRUD
    // ═══════════════════════════════════════════════════════════════

    /** إنشاء جروب جديد */
    @Transactional
    public GroupResponse createGroup(Long teacherId, CreateGroupRequest req) {
        Teacher teacher = teacherRepo.findById(teacherId)
                .orElseThrow(() -> notFound("المدرس غير موجود"));

        String resolvedCenterName = req.getCenterName();
        Center center = null;

        if (req.getCenterId() != null) {
            center = centerRepo.findById(req.getCenterId())
                    .orElseThrow(() -> notFound("السنتر غير موجود"));
            resolvedCenterName = center.getName();
        }

        String title = buildTitle(req.getDayOfWeek(), req.getMeetingTime());

        AttendanceGroup group = AttendanceGroup.builder()
                .teacher(teacher)
                .center(center)
                .centerName(resolvedCenterName)
                .title(title)
                .dayOfWeek(req.getDayOfWeek())
                .meetingTime(req.getMeetingTime())
                .description(req.getDescription())
                .levelId(req.getLevelId())
                .maxCapacity(req.getMaxCapacity())
                .active(true)
                .build();

        group = groupRepo.save(group);
        return toGroupResponse(group);
    }

    /** قائمة الجروبات النشطة للمدرس */
    public List<GroupResponse> getMyGroups(Long teacherId) {
        return groupRepo.findActiveByTeacher(teacherId)
                .stream()
                .map(this::toGroupResponse)
                .collect(Collectors.toList());
    }

    /** قائمة الجروبات النشطة للمدرس حسب الصف الدراسي */
    public List<GroupResponse> getMyGroupsByLevel(Long teacherId, Long levelId) {
        return groupRepo.findActiveByTeacherAndLevel(teacherId, levelId)
                .stream()
                .map(this::toGroupResponse)
                .collect(Collectors.toList());
    }

    /** تفاصيل جروب واحد (للمدرس صاحبه) */
    public GroupResponse getGroup(Long teacherId, Long groupId) {
        AttendanceGroup group = requireGroupOfTeacher(teacherId, groupId);
        return toGroupResponse(group);
    }

    /** حذف ناعم للجروب */
    @Transactional
    public void deleteGroup(Long teacherId, Long groupId) {
        AttendanceGroup group = requireGroupOfTeacher(teacherId, groupId);
        group.setActive(false);
        groupRepo.save(group);
    }

    // ═══════════════════════════════════════════════════════════════
    // MEMBER MANAGEMENT
    // ═══════════════════════════════════════════════════════════════

    /** إضافة طالب للجروب بـ studentId أو studentCode أو qrToken */
    @Transactional
    public GroupMemberResponse addStudent(Long teacherId, Long groupId, AddStudentRequest req) {
        AttendanceGroup group = requireGroupOfTeacher(teacherId, groupId);

        Student student = resolveStudent(req);

        if (memberRepo.existsByGroupIdAndStudentIdAndActiveTrue(groupId, student.getId())) {
            throw conflict("الطالب موجود بالفعل في الجروب");
        }

        AttendanceGroupMember member = AttendanceGroupMember.builder()
                .group(group)
                .student(student)
                .active(true)
                .build();

        memberRepo.save(member);
        return toMemberResponse(member);
    }

    /** إزالة طالب من الجروب (soft) */
    @Transactional
    public void removeStudent(Long teacherId, Long groupId, Long studentId) {
        requireGroupOfTeacher(teacherId, groupId);

        AttendanceGroupMember member = memberRepo
                .findByGroupIdAndStudentIdAndActiveTrue(groupId, studentId)
                .orElseThrow(() -> notFound("الطالب غير موجود في هذا الجروب"));

        member.setActive(false);
        memberRepo.save(member);
    }

    /** قائمة أعضاء الجروب */
    public List<GroupMemberResponse> listMembers(Long teacherId, Long groupId) {
        requireGroupOfTeacher(teacherId, groupId);
        return memberRepo.findByGroupIdAndActiveTrue(groupId)
                .stream()
                .map(this::toMemberResponse)
                .collect(Collectors.toList());
    }

    // ═══════════════════════════════════════════════════════════════
    // SESSION MANAGEMENT
    // ═══════════════════════════════════════════════════════════════

    /** إنشاء حصة جديدة (لا تُفتح تلقائياً) */
    @Transactional
    public SessionResponse createSession(Long teacherId, Long groupId, CreateSessionRequest req) {
        AttendanceGroup group = requireGroupOfTeacher(teacherId, groupId);

        int sessionNumber = req.getSessionNumber() != null
                ? req.getSessionNumber()
                : sessionRepo.findMaxSessionNumber(groupId) + 1;

        LocalDate date = req.getSessionDate() != null ? req.getSessionDate() : LocalDate.now();

        String title = req.getTitle() != null && !req.getTitle().isBlank()
                ? req.getTitle()
                : "الحصة " + sessionNumber;

        AttendanceGroupSession session = AttendanceGroupSession.builder()
                .group(group)
                .sessionDate(date)
                .sessionNumber(sessionNumber)
                .title(title)
                .open(false)
                .build();

        session = sessionRepo.save(session);
        return toSessionResponse(session);
    }

    /** فتح الحصة لتسجيل الحضور */
    @Transactional
    public SessionResponse openSession(Long teacherId, Long sessionId) {
        AttendanceGroupSession session = requireSessionOfTeacher(teacherId, sessionId);

        // التأكد من أنه مفيش حصة تانية مفتوحة في نفس الجروب
        sessionRepo.findByGroupIdAndOpenTrue(session.getGroup().getId())
                .ifPresent(other -> {
                    if (!other.getId().equals(sessionId)) {
                        throw conflict("يوجد حصة مفتوحة بالفعل في هذا الجروب. أغلقها أولاً.");
                    }
                });

        if (session.isOpen()) {
            return toSessionResponse(session); // مفتوحة أصلاً
        }

        session.setOpen(true);
        session.setOpenedAt(LocalDateTime.now());
        sessionRepo.save(session);
        return toSessionResponse(session);
    }

    /**
     * إغلاق الحصة + تسجيل غياب تلقائي لكل من لم يُسجَّل.
     */
    @Transactional
    public SessionResponse closeSession(Long teacherId, Long sessionId) {
        AttendanceGroupSession session = requireSessionOfTeacher(teacherId, sessionId);

        if (!session.isOpen()) {
            throw conflict("الحصة مغلقة بالفعل");
        }

        // IDs الطلاب الذين سُجِّلوا بالفعل
        List<Long> markedIds = recordRepo.findMarkedStudentIds(sessionId);

        // كل أعضاء الجروب
        List<AttendanceGroupMember> allMembers =
                memberRepo.findByGroupIdAndActiveTrue(session.getGroup().getId());

        List<AttendanceGroupRecord> absentRecords = new ArrayList<>();

        for (AttendanceGroupMember member : allMembers) {
            if (!markedIds.contains(member.getStudent().getId())) {
                absentRecords.add(AttendanceGroupRecord.builder()
                        .session(session)
                        .student(member.getStudent())
                        .status(AttendanceStatus.ABSENT)
                        .scanMethod(ScanMethod.AUTO_ABSENT)
                        .scannedAt(LocalDateTime.now())
                        .build());
            }
        }

        if (!absentRecords.isEmpty()) {
            recordRepo.saveAll(absentRecords);
        }

        session.setOpen(false);
        session.setClosedAt(LocalDateTime.now());
        sessionRepo.save(session);

        return toSessionResponse(session);
    }

    /** قائمة الحصص في جروب */
    public List<SessionResponse> listSessions(Long teacherId, Long groupId) {
        requireGroupOfTeacher(teacherId, groupId);
        return sessionRepo.findByGroupIdOrderBySessionDateDesc(groupId)
                .stream()
                .map(this::toSessionResponse)
                .collect(Collectors.toList());
    }

    // ═══════════════════════════════════════════════════════════════
    // MARK ATTENDANCE
    // ═══════════════════════════════════════════════════════════════

    /**
     * تسجيل حضور طالب.
     * يقبل qrToken (من الـ scan) أو studentId (إدخال يدوي).
     * يكتشف تلقائياً:
     *   - WRONG_CENTER: لو السنتر اللي سجّل فيه الطالب غير السنتر ده
     *   - ONLINE_TO_CENTER: لو الطالب أونلاين وجاء حضر في السنتر
     */
    @Transactional
    public MarkResult markAttendance(Long teacherId, Long sessionId, MarkAttendanceRequest req) {
        AttendanceGroupSession session = requireSessionOfTeacher(teacherId, sessionId);

        if (!session.isOpen()) {
            throw conflict("الحصة مغلقة — لا يمكن تسجيل الحضور");
        }

        // ── 1. استخراج الطالب ──────────────────────────────────────
        Student student = resolveStudentFromMarkRequest(req);

        // ── 2. التحقق من العضوية ───────────────────────────────────
        if (!memberRepo.existsByGroupIdAndStudentIdAndActiveTrue(
                session.getGroup().getId(), student.getId())) {
            throw badRequest("الطالب ليس عضواً في هذا الجروب");
        }

        // ── 3. منع التسجيل المكرر ────────────────────────────────
        if (recordRepo.existsBySessionIdAndStudentId(sessionId, student.getId())) {
            // أعد إرجاع السجل الموجود
            AttendanceGroupRecord existing =
                    recordRepo.findBySessionIdAndStudentId(sessionId, student.getId()).get();
            return MarkResult.builder()
                    .record(toRecordResponse(existing))
                    .hasAlert(existing.getAlertType() != null)
                    .alertType(existing.getAlertType())
                    .alertMessage(existing.getAlertMessage())
                    .build();
        }

        // ── 4. كشف الـ Alert ──────────────────────────────────────
        GroupAlertType alertType    = null;
        String         alertMessage = null;
        boolean        hasAlert     = false;

        String groupCenterName   = session.getGroup().getCenterName();
        String studentCenterName = student.getCenterName();

        // ONLINE_TO_CENTER: الطالب مسجّل أونلاين وجاء في السنتر
        if (Boolean.TRUE.equals(student.getOnline())) {
            alertType    = GroupAlertType.ONLINE_TO_CENTER;
            alertMessage = "الطالب " + student.getFirstName() + " مسجّل أونلاين وظهر في السنتر";
            hasAlert     = true;
        }
        // WRONG_CENTER: سنتر الطالب مختلف عن سنتر الجروب
        else if (groupCenterName != null && studentCenterName != null
                && !groupCenterName.equalsIgnoreCase(studentCenterName)) {
            alertType    = GroupAlertType.WRONG_CENTER;
            alertMessage = "الطالب " + student.getFirstName()
                    + " مسجّل في سنتر «" + studentCenterName
                    + "» وليس «" + groupCenterName + "»";
            hasAlert     = true;
        }

        // ── 5. تحديد طريقة الـ scan ──────────────────────────────
        ScanMethod method = (req.getQrToken() != null && !req.getQrToken().isBlank())
                ? ScanMethod.QR_SCAN
                : ScanMethod.MANUAL_ID;

        // ── 6. حفظ السجل ─────────────────────────────────────────
        AttendanceGroupRecord record = AttendanceGroupRecord.builder()
                .session(session)
                .student(student)
                .status(AttendanceStatus.PRESENT)
                .scanMethod(method)
                .alertType(alertType)
                .alertMessage(alertMessage)
                .teacherComment(req.getComment())
                .scannedAt(LocalDateTime.now())
                .build();

        record = recordRepo.save(record);

        return MarkResult.builder()
                .record(toRecordResponse(record))
                .hasAlert(hasAlert)
                .alertType(alertType)
                .alertMessage(alertMessage)
                .build();
    }

    /** تعديل حالة طالب بعد التسجيل (تصحيح خطأ) */
    @Transactional
    public RecordResponse updateRecordStatus(Long teacherId, Long recordId,
                                             AttendanceStatus newStatus) {
        AttendanceGroupRecord record = recordRepo.findById(recordId)
                .orElseThrow(() -> notFound("السجل غير موجود"));

        // التأكد أن الحصة تابعة لهذا المدرس
        if (!record.getSession().getGroup().getTeacher().getId().equals(teacherId)) {
            throw forbidden("ليس لديك صلاحية تعديل هذا السجل");
        }

        record.setStatus(newStatus);
        record = recordRepo.save(record);
        return toRecordResponse(record);
    }

    // ═══════════════════════════════════════════════════════════════
    // COMMENTS
    // ═══════════════════════════════════════════════════════════════

    /** إضافة أو تعديل تعليق المدرس على سجل طالب */
    @Transactional
    public RecordResponse addComment(Long teacherId, Long recordId, CommentRequest req) {
        AttendanceGroupRecord record = recordRepo.findById(recordId)
                .orElseThrow(() -> notFound("السجل غير موجود"));

        if (!record.getSession().getGroup().getTeacher().getId().equals(teacherId)) {
            throw forbidden("ليس لديك صلاحية التعليق على هذا السجل");
        }

        record.setTeacherComment(req.getComment());
        record = recordRepo.save(record);
        return toRecordResponse(record);
    }

    // ═══════════════════════════════════════════════════════════════
    // SESSION RECORDS
    // ═══════════════════════════════════════════════════════════════

    /** كل سجلات حصة معينة */
    public List<RecordResponse> getSessionRecords(Long teacherId, Long sessionId) {
        requireSessionOfTeacher(teacherId, sessionId);
        return recordRepo.findBySessionIdOrderByScannedAtAsc(sessionId)
                .stream()
                .map(this::toRecordResponse)
                .collect(Collectors.toList());
    }

    // ═══════════════════════════════════════════════════════════════
    // STUDENT BRIEF  (للطالب وولي الأمر)
    // ═══════════════════════════════════════════════════════════════

    /**
     * ملخص الطالب في جروب معين.
     * يُعيد إحصائيات الحضور + تاريخ كامل بالحصص والتعليقات.
     */
    public StudentBriefResponse getStudentBrief(Long studentId, Long groupId) {
        // التحقق من وجود الطالب كعضو
        memberRepo.findByGroupIdAndStudentIdAndActiveTrue(groupId, studentId)
                .orElseThrow(() -> notFound("الطالب غير عضو في هذا الجروب"));

        AttendanceGroup group = groupRepo.findById(groupId)
                .orElseThrow(() -> notFound("الجروب غير موجود"));

        // سجلات الطالب في الجروب
        List<AttendanceGroupRecord> records =
                recordRepo.findByStudentAndGroup(studentId, groupId);

        // إحصاء الحالات
        int present = 0, absent = 0, late = 0, excused = 0;
        for (AttendanceGroupRecord r : records) {
            switch (r.getStatus()) {
                case PRESENT  -> present++;
                case ABSENT   -> absent++;
                case LATE     -> late++;
                case EXCUSED  -> excused++;
            }
        }

        int total = records.size();
        double percentage = total > 0
                ? Math.round(((double)(present + late) / total) * 1000.0) / 10.0
                : 0.0;

        // بناء قائمة الحصص
        List<StudentBriefResponse.SessionBrief> sessionBriefs = records.stream()
                .map(r -> StudentBriefResponse.SessionBrief.builder()
                        .sessionId(r.getSession().getId())
                        .sessionDate(r.getSession().getSessionDate())
                        .sessionNumber(r.getSession().getSessionNumber())
                        .sessionTitle(r.getSession().getTitle())
                        .status(r.getStatus())
                        .teacherComment(r.getTeacherComment())
                        .scannedAt(r.getScannedAt())
                        .build())
                .collect(Collectors.toList());

        return StudentBriefResponse.builder()
                .groupId(groupId)
                .groupTitle(group.getTitle())
                .centerName(group.getCenterName())
                .totalSessions(total)
                .presentCount(present)
                .absentCount(absent)
                .lateCount(late)
                .excusedCount(excused)
                .attendancePercentage(percentage)
                .sessions(sessionBriefs)
                .build();
    }

    /**
     * قائمة جروبات طالب + ملخص سريع لكل جروب.
     */
    public List<StudentBriefResponse> getMyGroupsBrief(Long studentId) {
        return memberRepo.findActiveByStudent(studentId)
                .stream()
                .map(m -> getStudentBrief(studentId, m.getGroup().getId()))
                .collect(Collectors.toList());
    }

    // ═══════════════════════════════════════════════════════════════
    // PRIVATE HELPERS
    // ═══════════════════════════════════════════════════════════════

    private AttendanceGroup requireGroupOfTeacher(Long teacherId, Long groupId) {
        return groupRepo.findByIdAndTeacherIdAndActiveTrue(groupId, teacherId)
                .orElseThrow(() -> notFound("الجروب غير موجود أو ليس تابعاً لك"));
    }

    private AttendanceGroupSession requireSessionOfTeacher(Long teacherId, Long sessionId) {
        return sessionRepo.findByIdAndTeacherId(sessionId, teacherId)
                .orElseThrow(() -> notFound("الحصة غير موجودة أو ليست تابعة لك"));
    }

    /** يحل بيانات الطالب من AddStudentRequest */
    private Student resolveStudent(AddStudentRequest req) {
        if (req.getStudentId() != null) {
            return studentRepo.findById(req.getStudentId())
                    .orElseThrow(() -> notFound("الطالب غير موجود"));
        }
        if (req.getStudentCode() != null && !req.getStudentCode().isBlank()) {
            return studentRepo.findByStudentCode(req.getStudentCode())
                    .orElseThrow(() -> notFound("الطالب غير موجود بهذا الكود"));
        }
        if (req.getQrToken() != null && !req.getQrToken().isBlank()) {
            StudentCard card = cardRepo.findByQrToken(req.getQrToken())
                    .orElseThrow(() -> notFound("QR Code غير صالح"));
            return card.getStudent();
        }
        throw badRequest("يجب تقديم studentId أو studentCode أو qrToken");
    }

    /** يحل بيانات الطالب من MarkAttendanceRequest */
    private Student resolveStudentFromMarkRequest(MarkAttendanceRequest req) {
        if (req.getQrToken() != null && !req.getQrToken().isBlank()) {
            StudentCard card = cardRepo.findByQrToken(req.getQrToken())
                    .orElseThrow(() -> notFound("QR Code غير صالح"));
            return card.getStudent();
        }
        if (req.getStudentId() != null) {
            return studentRepo.findById(req.getStudentId())
                    .orElseThrow(() -> notFound("الطالب غير موجود"));
        }
        throw badRequest("يجب تقديم qrToken أو studentId");
    }

    // ── Mappers ─────────────────────────────────────────────────

    private GroupResponse toGroupResponse(AttendanceGroup g) {
        var openSession = sessionRepo.findByGroupIdAndOpenTrue(g.getId());
        long count = groupRepo.countActiveMembers(g.getId());
        boolean full = g.getMaxCapacity() != null && count >= g.getMaxCapacity();

        return GroupResponse.builder()
                .id(g.getId())
                .title(g.getTitle())
                .dayOfWeek(g.getDayOfWeek())
                .meetingTime(g.getMeetingTime())
                .description(g.getDescription())
                .centerId(g.getCenter() != null ? g.getCenter().getId() : null)
                .centerName(g.getCenterName())
                .levelId(g.getLevelId())
                .membersCount(count)
                .maxCapacity(g.getMaxCapacity())
                .isFull(full)
                .openSessionId(openSession.map(AttendanceGroupSession::getId).orElse(null))
                .openSessionTitle(openSession.map(AttendanceGroupSession::getTitle).orElse(null))
                .active(g.isActive())
                .createdAt(g.getCreatedAt())
                .build();
    }

    private GroupMemberResponse toMemberResponse(AttendanceGroupMember m) {
        Student s = m.getStudent();
        return GroupMemberResponse.builder()
                .studentId(s.getId())
                .studentName(s.getFullName())
                .studentCode(s.getStudentCode())
                .phone(s.getPhone())
                .centerName(s.getCenterName())
                .online(Boolean.TRUE.equals(s.getOnline()))
                .joinedAt(m.getJoinedAt())
                .build();
    }

    private SessionResponse toSessionResponse(AttendanceGroupSession s) {
        long present  = recordRepo.countBySessionIdAndStatus(s.getId(), AttendanceStatus.PRESENT);
        long absent   = recordRepo.countBySessionIdAndStatus(s.getId(), AttendanceStatus.ABSENT);
        long late     = recordRepo.countBySessionIdAndStatus(s.getId(), AttendanceStatus.LATE);
        long total    = memberRepo.findByGroupIdAndActiveTrue(s.getGroup().getId()).size();

        return SessionResponse.builder()
                .id(s.getId())
                .groupId(s.getGroup().getId())
                .groupTitle(s.getGroup().getTitle())
                .sessionDate(s.getSessionDate())
                .sessionNumber(s.getSessionNumber())
                .title(s.getTitle())
                .open(s.isOpen())
                .presentCount(present)
                .absentCount(absent)
                .lateCount(late)
                .totalMembers(total)
                .openedAt(s.getOpenedAt())
                .closedAt(s.getClosedAt())
                .createdAt(s.getCreatedAt())
                .build();
    }

    private RecordResponse toRecordResponse(AttendanceGroupRecord r) {
        Student s = r.getStudent();
        return RecordResponse.builder()
                .id(r.getId())
                .studentId(s.getId())
                .studentName(s.getFullName())
                .studentCode(s.getStudentCode())
                .studentPhone(s.getPhone())
                .studentCenter(s.getCenterName())
                .status(r.getStatus())
                .scanMethod(r.getScanMethod())
                .hasAlert(r.getAlertType() != null)
                .alertType(r.getAlertType())
                .alertMessage(r.getAlertMessage())
                .teacherComment(r.getTeacherComment())
                .scannedAt(r.getScannedAt())
                .build();
    }

    // ── Exception helpers ────────────────────────────────────────

    private ResponseStatusException notFound(String msg) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, msg);
    }

    private ResponseStatusException badRequest(String msg) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, msg);
    }

    private ResponseStatusException conflict(String msg) {
        return new ResponseStatusException(HttpStatus.CONFLICT, msg);
    }

    private ResponseStatusException forbidden(String msg) {
        return new ResponseStatusException(HttpStatus.FORBIDDEN, msg);
    }

    @Transactional
    public GroupResponse updateGroup(Long teacherId, Long groupId, CreateGroupRequest req) {
        AttendanceGroup group = requireGroupOfTeacher(teacherId, groupId);

        String resolvedCenterName = req.getCenterName();
        Center center = group.getCenter();

        if (req.getCenterId() != null) {
            center = centerRepo.findById(req.getCenterId())
                    .orElseThrow(() -> notFound("السنتر غير موجود"));
            resolvedCenterName = center.getName();
        }

        group.setDayOfWeek(req.getDayOfWeek());
        group.setMeetingTime(req.getMeetingTime());
        group.setTitle(buildTitle(req.getDayOfWeek(), req.getMeetingTime()));
        group.setDescription(req.getDescription());
        group.setCenter(center);
        group.setCenterName(resolvedCenterName);
        if (req.getLevelId() != null) group.setLevelId(req.getLevelId());
        group.setMaxCapacity(req.getMaxCapacity());

        return toGroupResponse(groupRepo.save(group));
    }

    // ── Helper ───────────────────────────────────────────────────

    /**
     * يبني عنوان الميعاد من اليوم والوقت
     * مثال: "السبت - 10:30 م"
     */
    private String buildTitle(String dayOfWeek, String meetingTime) {
        if (dayOfWeek == null || meetingTime == null) return "";
        try {
            String[] parts = meetingTime.split(":");
            int hour = Integer.parseInt(parts[0]);
            int minute = Integer.parseInt(parts[1]);
            String period = hour < 12 ? "ص" : "م";
            int displayHour = hour == 0 ? 12 : (hour > 12 ? hour - 12 : hour);
            String formattedTime = String.format("%d:%02d %s", displayHour, minute, period);
            return dayOfWeek + " - " + formattedTime;
        } catch (Exception e) {
            return dayOfWeek + " - " + meetingTime;
        }
    }

}