package com.educore.parent.dashboard;

import com.educore.attendance.AttendanceRecord;
import com.educore.attendance.AttendanceRepository;
import com.educore.attendance.AttendanceResponse;
import com.educore.attendance.AttendanceType;
import com.educore.attendance.group.AttendanceGroup;
import com.educore.attendance.group.AttendanceGroupMember;
import com.educore.attendance.group.AttendanceGroupMemberRepository;
import com.educore.enrollment.Enrollment;
import com.educore.enrollment.EnrollmentRepository;
import com.educore.exception.ResourceNotFoundException;
import com.educore.lessongate.LessonProgressStatus;
import com.educore.lessongate.StudentLessonProgress;
import com.educore.lessongate.StudentLessonProgressRepository;
import com.educore.notification.NotificationRepository;
import com.educore.parent.Parent;
import com.educore.parent.ParentRepository;
import com.educore.student.Student;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ParentDashboardService {

    private final ParentRepository               parentRepository;
    private final AttendanceRepository           attendanceRepository;
    private final EnrollmentRepository           enrollmentRepository;
    private final StudentLessonProgressRepository progressRepository;
    private final NotificationRepository         notificationRepository;
    private final AttendanceGroupMemberRepository groupMemberRepository;

    // ─────────────────────────────────────────────────────────────
    // ملخص الـ Dashboard الرئيسي
    // ─────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public ParentDashboardSummary getSummary(Long parentId) {
        Parent parent = findParent(parentId);

        long totalUnread = notificationRepository
                .countByRecipientIdAndRecipientRoleAndIsReadFalse(parentId, "PARENT");

        List<ChildSummaryCard> cards = parent.getStudents().stream()
                .map(s -> buildChildCard(s, parentId))
                .toList();

        return ParentDashboardSummary.builder()
                .parentId(parentId)
                .parentName(parent.getName())
                .childrenCount(cards.size())
                .totalUnreadNotifications(totalUnread)
                .children(cards)
                .build();
    }

    // ─────────────────────────────────────────────────────────────
    // نظرة عامة على ابن محدد
    // ─────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public ChildOverview getChildOverview(Long parentId, Long studentId) {
        Student student = validateOwnership(parentId, studentId);

        long totalAtt  = attendanceRepository.countByStudentId(studentId);
        long centerAtt = attendanceRepository.countByStudentIdAndType(studentId, AttendanceType.CENTER);
        long onlineAtt = attendanceRepository.countByStudentIdAndType(studentId, AttendanceType.ONLINE);

        long completed   = progressRepository.countByStudentIdAndStatus(studentId, LessonProgressStatus.COMPLETED);
        long inProgress  = progressRepository.countByStudentIdAndStatus(studentId, LessonProgressStatus.IN_PROGRESS);
        long locked      = progressRepository.countByStudentIdAndStatus(studentId, LessonProgressStatus.LOCKED);

        long activeEnrollments = enrollmentRepository.countActiveEnrollmentsByStudent(studentId);

        return ChildOverview.builder()
                .studentId(studentId)
                .studentName(student.getFullName())
                .studentCode(student.getStudentCode())
                .grade(student.getGrade())
                .governorate(student.getGovernorate())
                .schoolName(student.getSchoolName())
                .studyType(student.getStudyType())
                .centerName(student.getCenterName())
                .profileImageUrl(student.getProfileImageUrl())
                .totalAttendance(totalAtt)
                .centerAttendance(centerAtt)
                .onlineAttendance(onlineAtt)
                .activeEnrollments(activeEnrollments)
                .completedLessons(completed)
                .inProgressLessons(inProgress)
                .lockedLessons(locked)
                .build();
    }

    // ─────────────────────────────────────────────────────────────
    // سجل حضور ابن محدد
    // ─────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<AttendanceResponse> getChildAttendance(Long parentId, Long studentId,
                                                        Pageable pageable) {
        validateOwnership(parentId, studentId);
        return attendanceRepository
                .findByStudentIdOrderByAttendedAtDesc(studentId, pageable)
                .map(this::toAttendanceResponse);
    }

    // ─────────────────────────────────────────────────────────────
    // كورسات (اشتراكات) ابن محدد
    // ─────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<ChildEnrollmentInfo> getChildEnrollments(Long parentId, Long studentId) {
        validateOwnership(parentId, studentId);
        return enrollmentRepository
                .findByStudentIdAndActiveTrue(studentId)
                .stream()
                .map(this::toEnrollmentInfo)
                .toList();
    }

    // ─────────────────────────────────────────────────────────────
    // تقدم ابن محدد في سيشن معين
    // ─────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<ChildProgressInfo> getChildProgress(Long parentId, Long studentId, Long sessionId) {
        validateOwnership(parentId, studentId);
        return progressRepository
                .findByStudentAndSession(studentId, sessionId)
                .stream()
                .map(this::toProgressInfo)
                .toList();
    }

    // ─────────────────────────────────────────────────────────────
    // Helpers — تحقق من ملكية الطالب وجلب الأب
    // ─────────────────────────────────────────────────────────────

    /** يتحقق أن الطالب ينتمي لهذا الأب — يرجع الطالب أو يرمي استثناء */
    private Student validateOwnership(Long parentId, Long studentId) {
        Parent parent = findParent(parentId);
        return parent.getStudents().stream()
                .filter(s -> s.getId().equals(studentId))
                .findFirst()
                .orElseThrow(() -> new SecurityException(
                        "هذا الطالب غير مرتبط بحسابك"));
    }

    private Parent findParent(Long parentId) {
        return parentRepository.findById(parentId)
                .orElseThrow(() -> new ResourceNotFoundException("ولي الأمر غير موجود: " + parentId));
    }

    private ChildSummaryCard buildChildCard(Student s, Long parentId) {
        Long studentId = s.getId();

        long totalAtt    = attendanceRepository.countByStudentId(studentId);
        long completed   = progressRepository.countByStudentIdAndStatus(studentId, LessonProgressStatus.COMPLETED);
        long activeEnrol = enrollmentRepository.countActiveEnrollmentsByStudent(studentId);

        // الإشعارات الغير مقروءة المرتبطة بهذا الطالب
        long unreadForChild = notificationRepository
                .countUnreadByRecipientAndStudent(parentId, "PARENT", studentId);

        return ChildSummaryCard.builder()
                .studentId(studentId)
                .studentName(s.getFullName())
                .studentCode(s.getStudentCode())
                .grade(s.getGrade())
                .studyType(s.getStudyType())
                .centerName(s.getCenterName())
                .phone(s.getPhone())
                .profileImageUrl(s.getProfileImageUrl())
                .activeEnrollments(activeEnrol)
                .completedLessons(completed)
                .totalAttendance(totalAtt)
                .unreadNotifications(unreadForChild)
                .build();
    }

    // ─────────────────────────────────────────────────────────────
    // Mappers
    // ─────────────────────────────────────────────────────────────

    private AttendanceResponse toAttendanceResponse(AttendanceRecord r) {
        // Resolve group/center from the student's active group membership
        String groupName  = null;
        String centerName = null;
        if (r.getStudent() != null) {
            List<AttendanceGroupMember> memberships =
                    groupMemberRepository.findByStudentIdAndActiveTrue(r.getStudent().getId());
            if (!memberships.isEmpty()) {
                AttendanceGroup g = memberships.get(0).getGroup();
                groupName  = g.getTitle();
                centerName = g.getCenterName();
            }
        }

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
                .groupName(groupName)
                .centerName(centerName)
                .build();
    }

    private ChildEnrollmentInfo toEnrollmentInfo(Enrollment e) {
        return ChildEnrollmentInfo.builder()
                .enrollmentId(e.getId())
                .courseId(e.getCourse() != null ? e.getCourse().getId() : null)
                .courseTitle(e.getCourse() != null ? e.getCourse().getTitle() : null)
                .status(e.getStatus())
                .enrollmentType(e.getEnrollmentType())
                .progress(e.getProgress() != null ? e.getProgress() : 0.0)
                .enrolledAt(e.getEnrolledAt())
                .expiresAt(e.getExpiresAt())
                .completedAt(e.getCompletedAt())
                .build();
    }

    private ChildProgressInfo toProgressInfo(StudentLessonProgress p) {
        return ChildProgressInfo.builder()
                .weekId(p.getWeek() != null ? p.getWeek().getId() : null)
                .weekTitle(p.getWeek() != null ? p.getWeek().getTitle() : null)
                .orderNumber(p.getWeek() != null ? p.getWeek().getOrderNumber() : null)
                .status(p.getStatus())
                .quizScore(p.getQuizScore() != null ? p.getQuizScore().intValue() : null)
                .quizPassed(p.isQuizPassed())
                .unlockedAt(p.getUnlockedAt())
                .startedAt(p.getStartedAt())
                .completedAt(p.getCompletedAt())
                .build();
    }
}
