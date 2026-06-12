package com.educore.teacher;

import com.educore.attendance.AttendanceRepository;
import com.educore.attendance.group.AttendanceGroupMember;
import com.educore.attendance.group.AttendanceGroupMemberRepository;
import com.educore.attendance.group.AttendanceGroupRepository;
import com.educore.idverify.IdVerifyClient;
import com.educore.session.DatabaseSessionService;
import com.educore.dto.mapper.StudentMapper;
import com.educore.dto.response.StudentResponse;
import com.educore.exception.ResourceNotFoundException;
import com.educore.lesson.LessonRepository;
import com.educore.student.Student;
import com.educore.student.StudentRepository;
import com.educore.student.StudentStatus;
import com.educore.wallet.WalletRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Map;

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
    private final IdVerifyClient idVerifyClient;
    private final ObjectMapper objectMapper;

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

    /** يحوّل أي قيمة جايه من JSON (String/Number/Boolean/null) إلى String بأمان من غير ما يرمي ClassCastException */
    private String asString(Object value) {
        if (value == null) return null;
        if (value instanceof String s) return s.isBlank() ? null : s;
        return String.valueOf(value);
    }

    /** يحوّل أي قيمة رقمية جايه من JSON (Number أو String) إلى Long بأمان */
    private Long asLong(Object value) {
        if (value == null) return null;
        if (value instanceof Number n) return n.longValue();
        if (value instanceof String s && !s.isBlank()) {
            try { return Long.parseLong(s.trim()); } catch (NumberFormatException ignored) { return null; }
        }
        return null;
    }

    /** يحوّل أي قيمة Boolean جايه من JSON (Boolean أو String "true"/"false") إلى Boolean بأمان */
    private Boolean asBoolean(Object value) {
        if (value == null) return null;
        if (value instanceof Boolean b) return b;
        if (value instanceof String s) return Boolean.parseBoolean(s.trim());
        return null;
    }

    @Transactional
    public void updateStudent(Long studentId, java.util.Map<String, Object> updates) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("الطالب غير موجود"));

        // nullable = false fields: only update when the incoming value is non-blank
        if (updates.containsKey("firstName"))  { String v = asString(updates.get("firstName"));  if (v != null) student.setFirstName(v); }
        if (updates.containsKey("phone"))      { String v = asString(updates.get("phone"));      if (v != null) student.setPhone(v); }
        if (updates.containsKey("grade"))      { String v = asString(updates.get("grade"));      if (v != null) student.setGrade(v); }
        if (updates.containsKey("governorate")){ String v = asString(updates.get("governorate")); if (v != null) student.setGovernorate(v); }
        if (updates.containsKey("area"))       { String v = asString(updates.get("area"));       if (v != null) student.setArea(v); }
        if (updates.containsKey("schoolName")) { String v = asString(updates.get("schoolName")); if (v != null) student.setSchoolName(v); }
        if (updates.containsKey("schoolType")) { String v = asString(updates.get("schoolType")); if (v != null) student.setSchoolType(v); }
        // nullable fields: allow null
        if (updates.containsKey("secondName"))          student.setSecondName(asString(updates.get("secondName")));
        if (updates.containsKey("thirdName"))           student.setThirdName(asString(updates.get("thirdName")));
        if (updates.containsKey("fourthName"))          student.setFourthName(asString(updates.get("fourthName")));
        // parentPhone is stored in Parent entity — only update when non-blank (phone is nullable = false on Parent)
        if (updates.containsKey("parentPhone") && student.getParent() != null) {
            String newPhone = asString(updates.get("parentPhone"));
            if (newPhone != null) student.getParent().setPhone(newPhone);
        }
        if (updates.containsKey("educationDepartment")) student.setEducationDepartment(asString(updates.get("educationDepartment")));
        if (updates.containsKey("centerName"))          student.setCenterName(asString(updates.get("centerName")));
        // studyType from frontend is "ONLINE" or "CENTER" — map to boolean field `online`
        if (updates.containsKey("studyType")) {
            String st = asString(updates.get("studyType"));
            student.setOnline(st != null && "ONLINE".equalsIgnoreCase(st));
        }
        // also accept boolean `online` directly (قد تصل كـ Boolean أو كنص "true"/"false")
        if (updates.containsKey("online")) {
            Boolean onlineVal = asBoolean(updates.get("online"));
            if (onlineVal != null) student.setOnline(onlineVal);
        }
        if (updates.containsKey("nationalId"))           student.setNationalId(asString(updates.get("nationalId")));
        if (updates.containsKey("dateOfBirth")) {
            String dob = asString(updates.get("dateOfBirth"));
            student.setDateOfBirth(dob != null ? java.time.LocalDate.parse(dob) : null);
        }
        if (updates.containsKey("profileImageUrl"))     student.setProfileImageUrl(asString(updates.get("profileImageUrl")));
        if (updates.containsKey("identityDocumentUrl")) {
            String newUrl = asString(updates.get("identityDocumentUrl"));
            String oldUrl = student.getIdentityDocumentUrl();
            // لو الصورة اتغيرت أو اتمسحت → إعادة تعيين حالة التحقق
            boolean urlChanged = !java.util.Objects.equals(oldUrl, newUrl);
            student.setIdentityDocumentUrl(newUrl);
            if (urlChanged) {
                student.setIdVerificationStatus("NOT_CHECKED");
                student.setIdVerificationJson(null);
                log.info("identityDocumentUrl changed for studentId={} → verification reset to NOT_CHECKED", studentId);
            }
        }
        studentRepository.save(student);
    }

    // ─────────────────────────────────────────────────────────────
    //  ID Verification
    // ─────────────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    public StudentResponse verifyStudentId(Long studentId) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new RuntimeException("الطالب غير موجود"));

        String imageUrl = student.getIdentityDocumentUrl();
        if (imageUrl == null || imageUrl.isBlank()) {
            throw new IllegalStateException("الطالب لم يرفع صورة بطاقة الهوية");
        }

        Map<String, Object> result = idVerifyClient.verifyIdCard(imageUrl, studentId);

        // ─── مقارنة بيانات الهوية المستخرجة مع بيانات الطالب المسجلة ───────
        java.util.List<String> mismatches = new java.util.ArrayList<>();

        try {
            Object dataObj = result.get("data");
            if (dataObj instanceof Map) {
                Map<String, Object> data = (Map<String, Object>) dataObj;

                // 1. مقارنة الاسم
                String extractedName = asString(data.get("name_arabic"));
                String registeredName = student.getFullName();
                if (extractedName != null && registeredName != null) {
                    // مقارنة مبسّطة: هل الاسم الأول مطابق؟
                    String extractedFirst = extractedName.trim().split("\\s+")[0];
                    String registeredFirst = student.getFirstName() != null ? student.getFirstName().trim() : "";
                    if (!extractedFirst.isEmpty() && !registeredFirst.isEmpty()
                            && !extractedFirst.equalsIgnoreCase(registeredFirst)) {
                        mismatches.add("الاسم: المستخرج \"" + extractedFirst + "\" ≠ المسجل \"" + registeredFirst + "\"");
                    }
                }

                // 2. مقارنة الرقم القومي
                String extractedNid = asString(data.get("national_id"));
                String registeredNid = student.getNationalId();
                if (extractedNid != null && registeredNid != null && !extractedNid.isBlank() && !registeredNid.isBlank()) {
                    if (!extractedNid.replaceAll("\\s", "").equals(registeredNid.replaceAll("\\s", ""))) {
                        mismatches.add("الرقم القومي: المستخرج \"" + extractedNid + "\" ≠ المسجل \"" + registeredNid + "\"");
                    }
                }

                // 3. مقارنة تاريخ الميلاد
                String extractedDob = asString(data.get("date_of_birth"));
                java.time.LocalDate registeredDob = student.getDateOfBirth();
                if (extractedDob != null && !extractedDob.isBlank() && registeredDob != null) {
                    try {
                        java.time.LocalDate parsedDob = java.time.LocalDate.parse(extractedDob);
                        if (!parsedDob.equals(registeredDob)) {
                            mismatches.add("تاريخ الميلاد: المستخرج \"" + extractedDob + "\" ≠ المسجل \"" + registeredDob + "\"");
                        }
                    } catch (Exception ignored) { /* لو التاريخ مش في الفورمات الصح نتجاهله */ }
                }
            }
        } catch (Exception e) {
            log.warn("Could not compare ID data for studentId={}: {}", studentId, e.getMessage());
        }

        // إضافة نتيجة المقارنة للـ result
        result.put("data_mismatch", !mismatches.isEmpty());
        result.put("mismatch_details", mismatches);

        try {
            String json = objectMapper.writeValueAsString(result);
            student.setIdVerificationJson(json);
        } catch (Exception e) {
            student.setIdVerificationJson("{}");
        }

        Boolean success  = (Boolean) result.getOrDefault("success", false);
        String  cardSide = asString(result.get("card_side"));

        String verificationStatus;
        if (Boolean.TRUE.equals(success)) {
            verificationStatus = "VERIFIED";
        } else if ("back".equals(cardSide)) {
            // الصورة ظهر البطاقة — مش خطأ في الهوية، بس الصورة غلط
            verificationStatus = "NOT_CHECKED";
        } else {
            verificationStatus = "REJECTED";
        }
        student.setIdVerificationStatus(verificationStatus);
        studentRepository.save(student);

        StudentResponse response = studentMapper.toResponse(student);
        response.setIdVerificationResult(result);
        return response;
    }

    public StudentResponse getIdVerificationResult(Long studentId) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new RuntimeException("الطالب غير موجود"));

        StudentResponse response = studentMapper.toResponse(student);

        if (student.getIdVerificationJson() != null && !student.getIdVerificationJson().isBlank()) {
            try {
                Object parsed = objectMapper.readValue(student.getIdVerificationJson(), Object.class);
                response.setIdVerificationResult(parsed);
            } catch (Exception ignored) {}
        }

        return response;
    }
}
