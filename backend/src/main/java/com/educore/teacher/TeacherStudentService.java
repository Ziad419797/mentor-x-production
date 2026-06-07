package com.educore.teacher;

import com.educore.attendance.AttendanceRepository;
import com.educore.attendance.group.AttendanceGroupMember;
import com.educore.attendance.group.AttendanceGroupMemberRepository;
import com.educore.attendance.group.AttendanceGroupRepository;
import com.educore.session.DatabaseSessionService;
import com.educore.dto.mapper.StudentMapper;
import com.educore.dto.response.StudentResponse;
import com.educore.exception.ResourceNotFoundException;
import com.educore.lesson.LessonRepository;
import com.educore.student.Student;
import com.educore.student.StudentRepository;
import com.educore.student.StudentStatus;
import com.educore.wallet.WalletRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class TeacherStudentService {

    private final StudentRepository studentRepository;
    private final StudentMapper studentMapper;
    private final WalletRepository walletRepository;
    private final AttendanceRepository attendanceRepository;
    private final LessonRepository lessonRepository;
    private final PasswordEncoder passwordEncoder;
    private final AttendanceGroupMemberRepository groupMemberRepository;
    private final AttendanceGroupRepository groupRepository;
    private final DatabaseSessionService sessionService;

    /**
     * يحوّل الطالب لـ StudentResponse ويُضيف رصيد المحفظة ونسبة الحضور.
     * يُستخدم بدلاً من studentMapper::toResponse مباشرةً.
     */
    private StudentResponse toEnrichedResponse(Student student) {
        StudentResponse resp = studentMapper.toResponse(student);

        // ── رصيد المحفظة ────────────────────────────────────────────
        walletRepository.findByStudentId(student.getId())
                .ifPresent(w -> resp.setWalletBalance(w.getBalance()));

        // ── نسبة الحضور ─────────────────────────────────────────────
        long attended   = attendanceRepository.countByStudentId(student.getId());
        long totalWeeks = lessonRepository.count();
        resp.setAttendanceCount(attended);
        if (totalWeeks > 0) {
            resp.setAttendanceRate(Math.round((attended * 100.0 / totalWeeks) * 10) / 10.0);
        } else {
            resp.setAttendanceRate(0.0);
        }

        // ── الجروب ──────────────────────────────────────────────────
        groupMemberRepository.findFirstByStudentIdAndActiveTrue(student.getId())
                .ifPresent(m -> {
                    resp.setGroupId(m.getGroup().getId());
                    resp.setGroupName(m.getGroup().getTitle());
                });

        return resp;
    }

    // 1. عرض كل الطلاب المنتظرين (Pending) بتحويلهم لـ Response
    public Page<StudentResponse> getPendingStudents(Pageable pageable) {
        log.debug("Fetching pending students with pagination");

        // جلب الداتا كـ Entities ثم تحويلها باستخدام المابر
        return studentRepository.findByStatus(StudentStatus.PENDING, pageable)
                .map(this::toEnrichedResponse); // 👈 دي اللي بتشيل الباسورد وتظبط الـ FullName
    }

    // 2. قبول الطالب
    @Transactional
    public void approveStudent(Long studentId, String teacherName) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("الطالب غير موجود برقم: " + studentId));

        log.info("Teacher [{}] is approving student code: {}", teacherName, student.getStudentCode());

        student.approve(teacherName);
        studentRepository.save(student);

        log.info("Student [{}] is now ACTIVE", student.getStudentCode());
    }

    // 3. رفض الطالب
    @Transactional
    public void rejectStudent(Long studentId, String reason, String teacherName) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("الطالب غير موجود"));

        log.info("Teacher [{}] rejected student [{}] for reason: {}", teacherName, student.getStudentCode(), reason);

        student.reject(teacherName, reason);
        studentRepository.save(student);
    }

    public Page<StudentResponse> getActiveStudents(Pageable pageable) {
        log.debug("Fetching active students with pagination");

        // جلب الطلاب النشطين فقط
        return studentRepository.findByStatus(StudentStatus.ACTIVE, pageable)
                .map(this::toEnrichedResponse);

    }

    // 4. حظر الطالب (Block)
    @Transactional
    public void blockStudent(Long studentId, String reason, String teacherName) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("الطالب غير موجود برقم: " + studentId));

        log.info("Teacher [{}] is blocking student code: {} for reason: {}",
                teacherName, student.getStudentCode(), reason);

        // تغيير حالة الطالب إلى محظور
        student.setStatus(StudentStatus.BLOCKED);

        // تسجيل سبب الحظر (يمكن إعادة استخدام حقل rejectionReason أو إضافة حقل جديد)
        student.setRejectionReason("تم الحظر بواسطة: " + teacherName + " - السبب: " + reason);

        // اختياري: تعطيل الحساب أيضاً
        student.setEnabled(false);

        // اختياري: إنهاء الجلسات النشطة للطالب
        student.clearActiveSession();

        studentRepository.save(student);

        log.info("Student [{}] is now BLOCKED", student.getStudentCode());
    }

    // 5. إلغاء حظر الطالب (Unblock)
    @Transactional
    public void unblockStudent(Long studentId, String teacherName) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("الطالب غير موجود برقم: " + studentId));

        log.info("Teacher [{}] is unblocking student code: {}", teacherName, student.getStudentCode());

        student.setStatus(StudentStatus.ACTIVE);
        student.setEnabled(true);
        student.setRejectionReason(null);

        studentRepository.save(student);
        log.info("Student [{}] is now ACTIVE again", student.getStudentCode());
    }

    public Page<StudentResponse> getBlockedStudents(Pageable pageable) {
        return studentRepository.findByStatus(StudentStatus.BLOCKED, pageable)
                .map(this::toEnrichedResponse);
    }

    @Transactional
    public void transferStudentToCenter(Long studentId, Long groupId, String centerName) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("الطالب غير موجود"));
        student.setOnline(false);
        if (centerName != null && !centerName.isBlank()) {
            student.setCenterName(centerName);
        }
        studentRepository.save(student);

        if (groupId != null) {
            groupRepository.findById(groupId).ifPresent(group -> {
                // إلغاء العضوية القديمة
                groupMemberRepository.findByStudentIdAndActiveTrue(student.getId())
                        .forEach(m -> { m.setActive(false); groupMemberRepository.save(m); });
                // إضافة عضوية جديدة
                boolean alreadyMember = groupMemberRepository
                        .existsByGroupIdAndStudentIdAndActiveTrue(groupId, student.getId());
                if (!alreadyMember) {
                    AttendanceGroupMember member = AttendanceGroupMember.builder()
                            .group(group).student(student).active(true).build();
                    groupMemberRepository.save(member);
                }
            });
        }
        log.info("Student [{}] transferred to center: {}, groupId: {}", student.getStudentCode(), student.getCenterName(), groupId);
    }

    public Page<StudentResponse> getFutureCenterStudents(Pageable pageable) {
        return studentRepository.findFutureCenterStudents(pageable)
                .map(this::toEnrichedResponse);
    }

    public Page<StudentResponse> getRejectedStudents(Pageable pageable) {
        return studentRepository.findByStatus(StudentStatus.REJECTED, pageable)
                .map(this::toEnrichedResponse);
    }

    // --- Grade (Level) filtered variants ---

    public Page<StudentResponse> getPendingStudentsByGrade(String grade, Pageable pageable) {
        return studentRepository.findByStatusAndGrade(StudentStatus.PENDING, grade, pageable)
                .map(this::toEnrichedResponse);
    }

    public Page<StudentResponse> getActiveStudentsByGrade(String grade, Pageable pageable) {
        return studentRepository.findByStatusAndGrade(StudentStatus.ACTIVE, grade, pageable)
                .map(this::toEnrichedResponse);
    }

    public Page<StudentResponse> getBlockedStudentsByGrade(String grade, Pageable pageable) {
        return studentRepository.findByStatusAndGrade(StudentStatus.BLOCKED, grade, pageable)
                .map(this::toEnrichedResponse);
    }

    @Transactional
    public void clearStudentDevice(Long studentId) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("الطالب غير موجود"));
        // امسح الـ deviceId من الـ Student entity
        student.clearActiveSession();
        studentRepository.save(student);
        // Blacklist كل الـ sessions النشطة في جدول user_session
        sessionService.forceLogoutAll(studentId, "STUDENT");
        log.info("Device cleared + all sessions revoked for student: id={}, phone={}", studentId, student.getPhone());
    }

    @Transactional
    public void deleteStudent(Long studentId) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("الطالب غير موجود"));
        studentRepository.delete(student);
        log.info("Student deleted: id={}, phone={}", studentId, student.getPhone());
    }

    @Transactional
    public void updateStudent(Long studentId, java.util.Map<String, Object> updates) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("الطالب غير موجود"));

        if (updates.containsKey("firstName"))           student.setFirstName((String) updates.get("firstName"));
        if (updates.containsKey("secondName"))          student.setSecondName((String) updates.get("secondName"));
        if (updates.containsKey("thirdName"))           student.setThirdName((String) updates.get("thirdName"));
        if (updates.containsKey("fourthName"))          student.setFourthName((String) updates.get("fourthName"));
        if (updates.containsKey("phone"))               student.setPhone((String) updates.get("phone"));
        // parentPhone is stored in Parent entity — update via parent if present
        if (updates.containsKey("parentPhone") && student.getParent() != null) {
            student.getParent().setPhone((String) updates.get("parentPhone"));
        }
        if (updates.containsKey("governorate"))         student.setGovernorate((String) updates.get("governorate"));
        if (updates.containsKey("area"))                student.setArea((String) updates.get("area"));
        if (updates.containsKey("grade"))               student.setGrade((String) updates.get("grade"));
        if (updates.containsKey("schoolName"))          student.setSchoolName((String) updates.get("schoolName"));
        if (updates.containsKey("centerName"))          student.setCenterName((String) updates.get("centerName"));
        if (updates.containsKey("educationDepartment")) student.setEducationDepartment((String) updates.get("educationDepartment"));
        // studyType from frontend is "ONLINE" or "CENTER" — map to boolean field `online`
        if (updates.containsKey("studyType")) {
            String st = (String) updates.get("studyType");
            student.setOnline("ONLINE".equalsIgnoreCase(st));
        }
        // also accept boolean `online` directly
        if (updates.containsKey("online") && updates.get("online") instanceof Boolean) {
            student.setOnline((Boolean) updates.get("online"));
        }
        if (updates.containsKey("profileImageUrl"))     student.setProfileImageUrl((String) updates.get("profileImageUrl"));
        if (updates.containsKey("identityDocumentUrl")) student.setIdentityDocumentUrl((String) updates.get("identityDocumentUrl"));
        // fullName is computed from name parts via getFullName() — no setter needed
        // تعيين الجروب
        if (updates.containsKey("groupId") && updates.get("groupId") != null) {
            Long groupId = ((Number) updates.get("groupId")).longValue();
            groupRepository.findById(groupId).ifPresent(group -> {
                // إلغاء العضوية القديمة
                groupMemberRepository.findByStudentIdAndActiveTrue(student.getId())
                        .forEach(m -> { m.setActive(false); groupMemberRepository.save(m); });
                // إضافة عضوية جديدة إن لم تكن موجودة
                boolean alreadyMember = groupMemberRepository
                        .existsByGroupIdAndStudentIdAndActiveTrue(groupId, student.getId());
                if (!alreadyMember) {
                    AttendanceGroupMember member = AttendanceGroupMember.builder()
                            .group(group).student(student).active(true).build();
                    groupMemberRepository.save(member);
                    log.info("Student [{}] added to group [{}]", student.getStudentCode(), group.getTitle());
                }
            });
        }

        // password change — only if provided and non-blank
        if (updates.containsKey("password")) {
            String rawPass = (String) updates.get("password");
            if (rawPass != null && !rawPass.isBlank() && rawPass.length() >= 6) {
                student.setPassword(passwordEncoder.encode(rawPass));
                log.info("Password updated for student [{}]", student.getStudentCode());
            }
        }

        studentRepository.save(student);
        log.info("Student [{}] updated", student.getStudentCode());
    }
}
