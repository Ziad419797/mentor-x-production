package com.educore.admin;

import com.educore.enrollment.EnrollmentRepository;
import com.educore.exception.ResourceNotFoundException;
import com.educore.notification.NotificationService;
import com.educore.student.Student;
import com.educore.student.StudentRepository;
import com.educore.student.StudentStatus;
import com.educore.teacher.Teacher;
import com.educore.teacher.TeacherRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminManagementService {

    private final StudentRepository    studentRepository;
    private final TeacherRepository    teacherRepository;
    private final AdminRepository      adminRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final NotificationService  notificationService;

    /* ════════════════════════════════════════════════════
       STUDENTS
    ════════════════════════════════════════════════════ */

    /** List all students — filtered by status or all */
    public Page<Student> getAllStudents(StudentStatus status, Pageable pageable) {
        if (status != null) {
            return studentRepository.findByStatus(status, pageable);
        }
        return studentRepository.findAll(pageable);
    }

    public Student getStudentById(Long studentId) {
        return studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("الطالب غير موجود: " + studentId));
    }

    /** Approve a PENDING student → ACTIVE */
    @Transactional
    public Student approveStudent(Long studentId, String adminName) {
        Student student = getStudentById(studentId);
        if (student.getStatus() != StudentStatus.PENDING) {
            throw new IllegalStateException("الطالب ليس في حالة انتظار الموافقة");
        }
        student.approve(adminName);
        Student saved = studentRepository.save(student);

        // إشعار الطالب بالقبول + كوده + تفاصيل مركزه
        notificationService.notifyApproval(
                saved.getId(),
                saved.getStudentCode(),
                saved.getCenterName(),
                // اسم أول سيشن — غير متاح قبل التسجيل في كورس، فنبعت null ونكتفي بالكود والسنتر
                null
        );

        log.info("Admin {} approved student {} (code: {})", adminName, studentId, saved.getStudentCode());
        return saved;
    }

    /** Reject a PENDING student */
    @Transactional
    public Student rejectStudent(Long studentId, String adminName, String reason) {
        Student student = getStudentById(studentId);
        student.setStatus(StudentStatus.REJECTED);
        log.info("Admin {} rejected student {} — reason: {}", adminName, studentId, reason);
        return studentRepository.save(student);
    }

    /** Block an ACTIVE student */
    @Transactional
    public Student blockStudent(Long studentId, String adminName) {
        Student student = getStudentById(studentId);
        student.setStatus(StudentStatus.BLOCKED);
        log.info("Admin {} blocked student {}", adminName, studentId);
        return studentRepository.save(student);
    }

    /** Unblock → ACTIVE */
    @Transactional
    public Student unblockStudent(Long studentId, String adminName) {
        Student student = getStudentById(studentId);
        student.setStatus(StudentStatus.ACTIVE);
        log.info("Admin {} unblocked student {}", adminName, studentId);
        return studentRepository.save(student);
    }

    /* ════════════════════════════════════════════════════
       TEACHERS
    ════════════════════════════════════════════════════ */

    public Page<Teacher> getAllTeachers(Pageable pageable) {
        return teacherRepository.findAll(pageable);
    }

    public Teacher getTeacherById(Long teacherId) {
        return teacherRepository.findById(teacherId)
                .orElseThrow(() -> new ResourceNotFoundException("المعلم غير موجود: " + teacherId));
    }

    @Transactional
    public Teacher enableTeacher(Long teacherId, String adminName) {
        Teacher teacher = getTeacherById(teacherId);
        teacher.setEnabled(true);
        log.info("Admin {} enabled teacher {}", adminName, teacherId);
        return teacherRepository.save(teacher);
    }

    @Transactional
    public Teacher disableTeacher(Long teacherId, String adminName) {
        Teacher teacher = getTeacherById(teacherId);
        teacher.setEnabled(false);
        log.info("Admin {} disabled teacher {}", adminName, teacherId);
        return teacherRepository.save(teacher);
    }

    /* ════════════════════════════════════════════════════
       DASHBOARD STATS
    ════════════════════════════════════════════════════ */

    public Map<String, Long> getDashboardStats() {
        Map<String, Long> stats = new HashMap<>();
        stats.put("totalStudents",          studentRepository.count());
        stats.put("pendingStudents",        studentRepository.findByStatus(StudentStatus.PENDING, Pageable.unpaged()).getTotalElements());
        stats.put("activeStudents",         studentRepository.findByStatus(StudentStatus.ACTIVE,  Pageable.unpaged()).getTotalElements());
        stats.put("blockedStudents",        studentRepository.findByStatus(StudentStatus.BLOCKED, Pageable.unpaged()).getTotalElements());
        stats.put("totalTeachers",          teacherRepository.count());
        stats.put("totalEnrollments",       enrollmentRepository.count());
        stats.put("totalAdmins",            adminRepository.count());
        return stats;
    }
}
