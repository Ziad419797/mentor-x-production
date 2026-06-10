package com.educore.activitylog;

import com.educore.course.CourseRepository;
import com.educore.security.JwtUserPrincipal;
import com.educore.student.StudentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class ActivityLogAspect {

    private final ActivityLogService  logService;
    private final StudentRepository   studentRepo;
    private final CourseRepository    courseRepo;

    // ─── helper: get student code from student ID ─────────────────
    private String studentCode(Long studentId) {
        if (studentId == null) return "?";
        try {
            return studentRepo.findById(studentId)
                    .map(s -> s.getStudentCode() != null ? s.getStudentCode() : "ID:" + studentId)
                    .orElse("ID:" + studentId);
        } catch (Exception e) { return "?"; }
    }

    private String courseTitle(Long courseId) {
        if (courseId == null) return "?";
        try {
            return courseRepo.findById(courseId)
                    .map(c -> c.getTitle() != null ? c.getTitle() : "كورس#" + courseId)
                    .orElse("كورس#" + courseId);
        } catch (Exception e) { return "كورس#" + courseId; }
    }

    // ─── شحن محفظة ────────────────────────────────────────────────
    @AfterReturning("execution(* com.educore.wallet.WalletController.topUp(..))")
    public void onWalletTopUp(JoinPoint jp) {
        try {
            Object req = jp.getArgs()[0];
            Long studentId = (Long) req.getClass().getMethod("getStudentId").invoke(req);
            String amount  = req.getClass().getMethod("getAmount").invoke(req).toString();
            String code    = studentCode(studentId);
            record("شحن محفظة", "WALLET", code, "شحن " + amount + " جنيه للطالب كود: " + code);
        } catch (Exception e) { log.debug("onWalletTopUp log err: {}", e.getMessage()); }
    }

    // ─── قبول طالب ───────────────────────────────────────────────
    @AfterReturning("execution(* com.educore.teacher.TeacherStudentController.approve(..))")
    public void onStudentApprove(JoinPoint jp) {
        try {
            Long studentId = (Long) jp.getArgs()[0];
            String code = studentCode(studentId);
            record("قبول طالب", "STUDENT", code, "تم قبول الطالب كود: " + code);
        } catch (Exception e) { log.debug("onStudentApprove log err: {}", e.getMessage()); }
    }

    // ─── رفض طالب ────────────────────────────────────────────────
    @AfterReturning("execution(* com.educore.teacher.TeacherStudentController.reject(..))")
    public void onStudentReject(JoinPoint jp) {
        try {
            Object[] args = jp.getArgs();
            Long studentId = (Long) args[0];
            String reason  = args.length > 1 && args[1] != null ? args[1].toString() : "";
            String code    = studentCode(studentId);
            record("رفض طالب", "STUDENT", code,
                "تم رفض الطالب كود: " + code + (reason.isBlank() ? "" : " — السبب: " + reason));
        } catch (Exception e) { log.debug("onStudentReject log err: {}", e.getMessage()); }
    }

    // ─── حظر طالب ────────────────────────────────────────────────
    @AfterReturning("execution(* com.educore.teacher.TeacherStudentController.blockStudent(..))")
    public void onStudentBlock(JoinPoint jp) {
        try {
            Long studentId = (Long) jp.getArgs()[0];
            String code = studentCode(studentId);
            record("حظر طالب", "STUDENT", code, "تم حظر الطالب كود: " + code);
        } catch (Exception e) { log.debug("onStudentBlock log err: {}", e.getMessage()); }
    }

    // ─── إضافة كورس ──────────────────────────────────────────────
    @AfterReturning(pointcut = "execution(* com.educore.course.CourseController.createCourseWithImage(..))", returning = "result")
    public void onCourseCreate(JoinPoint jp, Object result) {
        try {
            String title = (String) result.getClass().getMethod("getTitle").invoke(
                result.getClass().getMethod("getData").invoke(result));
            record("إضافة كورس", "COURSE", null, "تم إنشاء كورس: " + title);
        } catch (Exception e) {
            record("إضافة كورس", "COURSE", null, "تم إنشاء كورس جديد");
        }
    }

    // ─── اشتراك يدوي ─────────────────────────────────────────────
    @AfterReturning("execution(* com.educore.enrollment.EnrollmentController.adminGrantEnrollment(..))")
    public void onEnroll(JoinPoint jp) {
        try {
            Object[] args = jp.getArgs();
            Long studentId = (Long) args[0];
            Long courseId  = (Long) args[1];
            String code    = studentCode(studentId);
            String title   = courseTitle(courseId);
            record("اشتراك في كورس", "ENROLLMENT", code,
                "تم إضافة الطالب كود: " + code + " إلى كورس: " + title);
        } catch (Exception e) { log.debug("onEnroll log err: {}", e.getMessage()); }
    }

    // ─── حذف اشتراك ──────────────────────────────────────────────
    @AfterReturning("execution(* com.educore.enrollment.EnrollmentController.cancelEnrollment(..))")
    public void onEnrollCancel(JoinPoint jp) {
        try {
            Long enrollmentId = (Long) jp.getArgs()[0];
            record("حذف اشتراك", "ENROLLMENT", enrollmentId.toString(), "تم حذف اشتراك رقم: " + enrollmentId);
        } catch (Exception e) { log.debug("onEnrollCancel log err: {}", e.getMessage()); }
    }

    // ─── إنشاء أكواد وصول ────────────────────────────────────────
    @AfterReturning("execution(* com.educore.copon.AccessCodeController.generate(..))")
    public void onAccessCodeGenerate(JoinPoint jp) {
        try {
            Object req   = jp.getArgs()[0];
            String count = req.getClass().getMethod("getCount").invoke(req).toString();
            record("إنشاء أكواد وصول", "ACCESS_CODE", null, "تم إنشاء " + count + " كود وصول");
        } catch (Exception e) { log.debug("onAccessCodeGenerate log err: {}", e.getMessage()); }
    }

    // ─── helper ──────────────────────────────────────────────────
    private void record(String action, String entityType, String entityId, String details) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String actorName     = "غير معروف";
            String actorUsername = "غير معروف";
            Long   actorId       = null;
            String actorRole     = null;

            if (auth != null && auth.getPrincipal() instanceof JwtUserPrincipal p) {
                actorUsername = p.getUsername() != null ? p.getUsername() : "غير معروف";
                actorName     = (p.getName() != null && !p.getName().isBlank()) ? p.getName() : actorUsername;
                actorId       = p.getUserId();
                actorRole     = p.getRole();
            }

            logService.log(actorName, actorUsername, actorId, actorRole,
                           action, entityType, entityId, details, null);
        } catch (Exception e) {
            log.warn("ActivityLogAspect error: {}", e.getMessage());
        }
    }
}
