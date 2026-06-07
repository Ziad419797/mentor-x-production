package com.educore.attendance;

import com.educore.exception.ResourceNotFoundException;
import com.educore.lesson.LessonRepository;
import com.educore.lesson.Week;
import com.educore.lessongate.LessonGateService;
import com.educore.notification.NotificationService;
import com.educore.parent.Parent;
import com.educore.student.Student;
import com.educore.student.StudentRepository;
import com.educore.studentcard.StudentCardRepository;
import com.educore.studentcard.StudentCard;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class AttendanceService {

    private final AttendanceRepository    attendanceRepository;
    private final StudentCardRepository   cardRepository;
    private final LessonRepository        lessonRepository;
    private final StudentRepository       studentRepository;
    private final LessonGateService       lessonGateService;
    private final NotificationService     notificationService;

    // ─────────────────────────────────────────────────────────────
    // Scan QR في السنتر — يُستدعى من الموظف
    // ─────────────────────────────────────────────────────────────

    @Transactional
    public AttendanceResponse scanQrAttendance(AttendanceScanRequest request,
                                                String scannedBy) {
        // 1. تحقق من الـ QR Token
        StudentCard card = cardRepository.findByQrToken(request.getQrToken())
                .orElseThrow(() -> new ResourceNotFoundException("QR Token غير صحيح أو غير موجود"));

        if (!card.isActive()) {
            throw new IllegalStateException("كارنيه الطالب موقوف — يرجى التواصل مع الإدارة");
        }

        Student student = card.getStudent();

        // 2. تحقق من الحصة
        Week week = lessonRepository.findById(request.getWeekId())
                .orElseThrow(() -> new ResourceNotFoundException("الحصة غير موجودة: " + request.getWeekId()));

        // 3. تحقق إن الطالب ما حضرش نفس الحصة دي قبل كده اليوم
        LocalDateTime dayStart = LocalDateTime.now().toLocalDate().atStartOfDay();
        LocalDateTime dayEnd   = dayStart.plusDays(1);
        boolean alreadyToday = attendanceRepository
                .findByWeekAndDay(week.getId(), dayStart, dayEnd)
                .stream()
                .anyMatch(a -> a.getStudent().getId().equals(student.getId()));

        if (alreadyToday) {
            log.warn("Student {} already marked attendance for week {} today", student.getId(), week.getId());
            throw new IllegalStateException("الطالب سجّل حضوره في هذه الحصة اليوم بالفعل");
        }

        // 4. سجّل الحضور
        AttendanceRecord record = AttendanceRecord.builder()
                .student(student)
                .week(week)
                .attendedAt(LocalDateTime.now())
                .type(AttendanceType.CENTER)
                .source(AttendanceSource.QR_SCAN)
                .scannedBy(scannedBy)
                .notes(request.getNotes())
                .build();

        attendanceRepository.save(record);

        // 5. سجّل IN_PROGRESS على الـ LessonGate (لو الحصة السابقة اتخلصت)
        if (lessonGateService.checkAccess(student.getId(), week.getId()).isAllowed()) {
            lessonGateService.markLessonStarted(student.getId(), week.getId());
        }

        // 6. أرسل إشعار لولي الأمر (async — لا يوقف الـ transaction)
        Parent parent = student.getParent();
        if (parent != null) {
            notificationService.notifyAttendanceCenter(
                    parent.getId(),
                    student.getFullName(),
                    week.getTitle(),
                    record.getId(),
                    student.getId());
        }

        log.info("CENTER attendance recorded: student={}, week={}, by={}",
                student.getId(), week.getId(), scannedBy);

        return toResponse(record);
    }

    // ─────────────────────────────────────────────────────────────
    // تسجيل حضور أونلاين — الطالب يفتح الحصة
    // ─────────────────────────────────────────────────────────────

    @Transactional
    public AttendanceResponse recordOnlineAttendance(Long studentId, Long weekId,
                                                      HttpServletRequest httpRequest) {
        Week week = lessonRepository.findById(weekId)
                .orElseThrow(() -> new ResourceNotFoundException("الحصة غير موجودة: " + weekId));

        // تحقق إن الطالب أنهى الحصة السابقة
        LessonGateService.AccessCheckResult access = lessonGateService.checkAccess(studentId, weekId);
        if (!access.isAllowed()) {
            throw new IllegalStateException(access.getDenialReason());
        }

        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("الطالب غير موجود: " + studentId));

        // تسجيل الحضور (بدون تكرار — طالب ممكن يفتح الحصة أكتر من مرة)
        if (!attendanceRepository.existsByStudentIdAndWeekId(studentId, weekId)) {
            String ip = extractIp(httpRequest);

            AttendanceRecord att = AttendanceRecord.builder()
                    .student(student)
                    .week(week)
                    .attendedAt(LocalDateTime.now())
                    .type(AttendanceType.ONLINE)
                    .source(AttendanceSource.ONLINE_ACCESS)
                    .ipAddress(ip)
                    .build();

            attendanceRepository.save(att);
            log.info("ONLINE attendance recorded: student={}, week={}", studentId, weekId);

            // أرسل إشعار لولي الأمر (أول مرة بس)
            Parent parent = student.getParent();
            if (parent != null) {
                notificationService.notifyAttendanceOnline(
                        parent.getId(),
                        student.getFullName(),
                        week.getTitle(),
                        att.getId(),
                        studentId);
            }
        }

        // سجّل IN_PROGRESS دايماً لما الطالب يفتح الحصة
        lessonGateService.markLessonStarted(studentId, weekId);

        return attendanceRepository
                .findTopByStudentIdAndWeekIdOrderByAttendedAtDesc(studentId, weekId)
                .map(this::toResponse)
                .orElse(null);
    }

    // ─────────────────────────────────────────────────────────────
    // قوائم الحضور
    // ─────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<AttendanceResponse> getMyAttendance(Long studentId, Pageable pageable) {
        return attendanceRepository
                .findByStudentIdOrderByAttendedAtDesc(studentId, pageable)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<AttendanceResponse> getStudentAttendance(Long studentId, Pageable pageable) {
        return attendanceRepository
                .findByStudentIdOrderByAttendedAtDesc(studentId, pageable)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<AttendanceResponse> getLessonAttendance(Long weekId, Pageable pageable) {
        return attendanceRepository
                .findByWeekIdOrderByAttendedAtDesc(weekId, pageable)
                .map(this::toResponse);
    }

    // ─────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────

    private AttendanceResponse toResponse(AttendanceRecord r) {
        if (r == null) return null;
        return AttendanceResponse.builder()
                .id(r.getId())
                .studentId(r.getStudent() != null ? r.getStudent().getId() : null)
                .studentName(r.getStudent() != null ? r.getStudent().getFullName() : null)
                .studentCode(r.getStudent() != null ? r.getStudent().getStudentCode() : null)
                .weekId(r.getWeek() != null ? r.getWeek().getId() : null)
                .weekTitle(r.getWeek() != null ? r.getWeek().getTitle() : null)
                .attendedAt(r.getAttendedAt())
                .type(r.getType())
                .source(r.getSource())
                .scannedBy(r.getScannedBy())
                .notes(r.getNotes())
                .build();
    }

    private String extractIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        return (ip != null && !ip.isBlank()) ? ip.split(",")[0].trim()
                : request.getRemoteAddr();
    }
}
