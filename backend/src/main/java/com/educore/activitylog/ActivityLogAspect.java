package com.educore.activitylog;

import com.educore.security.JwtUserPrincipal;
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

    private final ActivityLogService logService;

    // ─── شحن محفظة ────────────────────────────────────────────────
    @AfterReturning("execution(* com.educore.wallet.WalletController.topUp(..))")
    public void onWalletTopUp(JoinPoint jp) {
        Object[] args = jp.getArgs();
        try {
            Object req = args[0];
            String studentId = req.getClass().getMethod("getStudentId").invoke(req).toString();
            String amount    = req.getClass().getMethod("getAmount").invoke(req).toString();
            record("شحن محفظة", "WALLET", studentId, "مبلغ: " + amount + " جنيه");
        } catch (Exception e) {
            record("شحن محفظة", "WALLET", null, null);
        }
    }

    // ─── قبول طالب ───────────────────────────────────────────────
    @AfterReturning("execution(* com.educore.teacher.TeacherStudentController.approve(..))")
    public void onStudentApprove(JoinPoint jp) {
        Object[] args = jp.getArgs();
        try {
            String studentId = args[0] != null ? args[0].toString() : "?";
            record("قبول طالب", "STUDENT", studentId, null);
        } catch (Exception e) {
            record("قبول طالب", "STUDENT", null, null);
        }
    }

    // ─── رفض طالب ────────────────────────────────────────────────
    @AfterReturning("execution(* com.educore.teacher.TeacherStudentController.reject(..))")
    public void onStudentReject(JoinPoint jp) {
        Object[] args = jp.getArgs();
        try {
            String studentId = args[0] != null ? args[0].toString() : "?";
            String reason    = args.length > 1 && args[1] != null ? args[1].toString() : "";
            record("رفض طالب", "STUDENT", studentId, reason.isEmpty() ? null : "السبب: " + reason);
        } catch (Exception e) {
            record("رفض طالب", "STUDENT", null, null);
        }
    }

    // ─── حظر طالب ────────────────────────────────────────────────
    @AfterReturning("execution(* com.educore.teacher.TeacherStudentController.blockStudent(..))")
    public void onStudentBlock(JoinPoint jp) {
        Object[] args = jp.getArgs();
        try {
            String studentId = args[0] != null ? args[0].toString() : "?";
            record("حظر طالب", "STUDENT", studentId, null);
        } catch (Exception e) {
            record("حظر طالب", "STUDENT", null, null);
        }
    }

    // ─── إضافة كورس ──────────────────────────────────────────────
    @AfterReturning("execution(* com.educore.course.CourseController.createCourseWithImage(..))")
    public void onCourseCreate(JoinPoint jp) {
        record("إضافة كورس", "COURSE", null, null);
    }

    // ─── اشتراك يدوي ─────────────────────────────────────────────
    @AfterReturning("execution(* com.educore.enrollment.EnrollmentController.adminGrantEnrollment(..))")
    public void onEnroll(JoinPoint jp) {
        Object[] args = jp.getArgs();
        try {
            String studentId = args[0] != null ? args[0].toString() : "?";
            String courseId  = args.length > 1 && args[1] != null ? args[1].toString() : "?";
            record("اشتراك في كورس", "ENROLLMENT", studentId, "كورس: " + courseId);
        } catch (Exception e) {
            record("اشتراك في كورس", "ENROLLMENT", null, null);
        }
    }

    // ─── إنشاء أكواد وصول ────────────────────────────────────────
    @AfterReturning("execution(* com.educore.copon.AccessCodeController.generate(..))")
    public void onAccessCodeGenerate(JoinPoint jp) {
        Object[] args = jp.getArgs();
        try {
            Object req = args[0];
            String count = req.getClass().getMethod("getCount").invoke(req).toString();
            record("إنشاء أكواد وصول", "ACCESS_CODE", null, "عدد: " + count);
        } catch (Exception e) {
            record("إنشاء أكواد وصول", "ACCESS_CODE", null, null);
        }
    }

    // ─── helper ──────────────────────────────────────────────────

    private void record(String action, String entityType, String entityId, String details) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String actorName     = "غير معروف";
            String actorUsername = "غير معروف";

            if (auth != null && auth.getPrincipal() instanceof JwtUserPrincipal p) {
                actorUsername = p.getUsername();
                actorName     = p.getUsername();
            }

            String ip = null;
            try {
                ServletRequestAttributes attrs =
                        (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
                if (attrs != null) ip = attrs.getRequest().getRemoteAddr();
            } catch (Exception ignored) {}

            logService.log(actorName, actorUsername, action, entityType, entityId, details, ip);
        } catch (Exception e) {
            log.warn("ActivityLogAspect error: {}", e.getMessage());
        }
    }
}
