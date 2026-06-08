package com.educore.lessongate;

import com.educore.lesson.LessonRepository;
import com.educore.lesson.StudentWeekAccess;
import com.educore.lesson.StudentWeekAccessRepository;
import com.educore.lesson.Week;
import com.educore.lesson.WeekLockType;
import com.educore.assignment.Assignment;
import com.educore.assignment.AssignmentRepository;
import com.educore.enrollment.Enrollment;
import com.educore.enrollment.EnrollmentRepository;
import com.educore.lessonmaterial.LessonMaterial;
import com.educore.lessonmaterial.LessonMaterialRepository;
import com.educore.quiz.Quiz;
import com.educore.quiz.QuizRepository;
import com.educore.student.Student;
import com.educore.student.StudentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * يتحكم في إمكانية مشاهدة فيديوهات الحصص.
 *
 * القاعدة:
 *   - الطالب يشتري أي حصة بحرية كاملة (Enrollment = مش شغل الـ Gate).
 *   - لما يحاول يفتح فيديو الحصة N، السيستم يتحقق:
 *       ✔ الحصة الأولى في الـ session → متاحة دايماً
 *       ✔ أي حصة تانية → الحصة N-1 لازم تكون COMPLETED (عدّى كويزها)
 *       ✘ لو N-1 مش COMPLETED → "لم تجتز اختبار الحصة السابقة"
 *
 * لا يوجد "UNLOCK chain" — الـ access يتحدد dynamically من canAccessLesson().
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LessonGateService {

    private final StudentLessonProgressRepository progressRepository;
    private final LessonRepository                lessonRepository;
    private final StudentRepository               studentRepository;
    private final StudentWeekAccessRepository     weekAccessRepository;
    private final LessonMaterialRepository        materialRepository;
    private final StudentMaterialViewRepository   materialViewRepository;
    private final EnrollmentRepository            enrollmentRepository;
    private final QuizRepository                  quizRepository;
    private final AssignmentRepository            assignmentRepository;

    // ─────────────────────────────────────────────────────────────
    // هل الطالب يقدر يشوف فيديو الحصة دي؟
    // ─────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public boolean canAccessLesson(Long studentId, Long weekId) {
        return checkAccess(studentId, weekId).isAllowed();
    }

    /**
     * بيرجع نتيجة مفصّلة بسبب الرفض — يُستدعى من الـ controllers.
     * الفحص بالترتيب:
     *   1. هل الحصة موجودة؟
     *   2. هل الحصة مقفلة بسبب teacher lock (globally أو duration)؟
     *   3. هل الطالب أكمل الحصة السابقة (sequential gate)؟
     */
    @Transactional(readOnly = true)
    public AccessCheckResult checkAccess(Long studentId, Long weekId) {
        Week week = lessonRepository.findById(weekId).orElse(null);
        if (week == null) {
            return AccessCheckResult.denied("الحصة غير موجودة");
        }

        // ── Teacher Lock check ──────────────────────────────────────
        AccessCheckResult lockCheck = checkTeacherLock(week, studentId);
        if (!lockCheck.isAllowed()) {
            return lockCheck;
        }

        // ── ContentOrder Gate check ─────────────────────────────────
        String contentOrder = lessonRepository.findContentOrderByWeekId(weekId);

        // NONE → وصول حر — تخطى كل الـ sequential checks
        if ("NONE".equals(contentOrder)) {
            return AccessCheckResult.allowed();
        }

        // LOCK_BY_SESSION أو LOCK_BY_ELEMENT → sequential week check
        // يجب إكمال كل محتويات الحصة السابقة قبل الانتقال للحصة الحالية
        int order = week.getOrderNumber() != null ? week.getOrderNumber() : 1;

        // الحصة الأولى في الـ session → دايماً متاحة
        if (order <= 1 || lessonRepository.isFirstInSessions(weekId, order)) {
            return AccessCheckResult.allowed();
        }

        // جيب الحصة السابقة مباشرة في نفس الـ sessions
        List<Week> prevWeeks = lessonRepository.findPreviousWeeksInSameSessions(weekId, order);
        if (prevWeeks.isEmpty()) {
            return AccessCheckResult.allowed(); // ما فيش حصة سابقة → متاحة
        }

        Week prevWeek = prevWeeks.get(0);

        // تحقق: هل الطالب شاف كل مواد الحصة السابقة؟
        List<Long> prevMaterialIds = materialRepository.findActiveIdsByWeekId(prevWeek.getId());
        if (prevMaterialIds.isEmpty()) {
            // الحصة السابقة بدون محتوى → اعتبرها مكتملة
            return AccessCheckResult.allowed();
        }

        long viewedCount = materialViewRepository
                .countViewedByStudentIdAndMaterialIds(studentId, prevMaterialIds);

        if (viewedCount >= prevMaterialIds.size()) {
            return AccessCheckResult.allowed();
        }

        long remaining = prevMaterialIds.size() - viewedCount;
        return AccessCheckResult.denied(
                "أكمل \"" + prevWeek.getTitle() + "\" أولاً — تبقى " + remaining + " عنصر لم تُشاهده بعد"
        );
    }

    /**
     * يتحقق من قفل الحصة بناءً على إعداد المدرس.
     */
    private AccessCheckResult checkTeacherLock(Week week, Long studentId) {
        WeekLockType lockType = week.getLockType();
        if (lockType == null || lockType == WeekLockType.NEVER) {
            return AccessCheckResult.allowed();
        }

        // ON_DATE: تقفل في تاريخ محدد لكل الطلاب
        if (lockType == WeekLockType.ON_DATE) {
            if (week.isGloballyLocked()) {
                return AccessCheckResult.denied("انتهت مدة الوصول لهذه الحصة");
            }
            if (week.getLockDate() != null && !LocalDate.now().isBefore(week.getLockDate())) {
                return AccessCheckResult.denied("انتهت مدة الوصول لهذه الحصة في " + week.getLockDate());
            }
            return AccessCheckResult.allowed();
        }

        // AFTER_DURATION: تقفل بعد X أيام من أول وصول للطالب
        if (lockType == WeekLockType.AFTER_DURATION && week.getLockAfterDays() != null) {
            return weekAccessRepository.findByStudentIdAndWeekId(studentId, week.getId())
                    .map(access -> {
                        LocalDateTime expiry = access.getFirstAccessAt()
                                .plusDays(week.getLockAfterDays());
                        if (LocalDateTime.now().isAfter(expiry)) {
                            return AccessCheckResult.denied(
                                    "انتهت مدة الوصول لهذه الحصة (كانت متاحة لمدة " +
                                    week.getLockAfterDays() + " أيام من أول فتح)"
                            );
                        }
                        return AccessCheckResult.allowed();
                    })
                    // لو الطالب لم يدخل الحصة قبل كده → متاحة (السجل هيتنشأ عند أول فتح)
                    .orElseGet(AccessCheckResult::allowed);
        }

        return AccessCheckResult.allowed();
    }

    // ─────────────────────────────────────────────────────────────
    // الطالب بيفتح الفيديو → تحقق من الـ gate وسجّل IN_PROGRESS
    // ─────────────────────────────────────────────────────────────

    @Transactional
    public LessonProgressStatus markLessonStarted(Long studentId, Long weekId) {
        AccessCheckResult access = checkAccess(studentId, weekId);
        if (!access.isAllowed()) {
            throw new IllegalStateException(access.getDenialReason());
        }

        // أنشئ record لو مش موجود (الطالب اشترى الحصة مباشرة)
        StudentLessonProgress progress = progressRepository
                .findByStudentIdAndWeekId(studentId, weekId)
                .orElseGet(() -> {
                    Student student = studentRepository.getReferenceById(studentId);
                    Week week = lessonRepository.getReferenceById(weekId);
                    return StudentLessonProgress.builder()
                            .student(student)
                            .week(week)
                            .build();
                });

        progress.markInProgress();
        progressRepository.save(progress);

        // سجّل أول وصول للطالب لهذه الحصة (لو مسجّلش قبل كده)
        if (!weekAccessRepository.existsByStudentIdAndWeekId(studentId, weekId)) {
            weekAccessRepository.save(
                    StudentWeekAccess.builder()
                            .studentId(studentId)
                            .weekId(weekId)
                            .build()
            );
            log.debug("First access recorded: student={}, week={}", studentId, weekId);
        }

        log.info("Lesson IN_PROGRESS: student={}, week={}", studentId, weekId);
        return progress.getStatus();
    }

    // ─────────────────────────────────────────────────────────────
    // الطالب عدّى الكويز → سجّل COMPLETED
    // الحصة التالية تبقى متاحة تلقائياً بناءً على canAccessLesson
    // ─────────────────────────────────────────────────────────────

    @Transactional
    public void completeLesson(Long studentId, Long weekId, double quizScore, boolean passed) {
        StudentLessonProgress progress = progressRepository
                .findByStudentIdAndWeekId(studentId, weekId)
                .orElseGet(() -> {
                    Student student = studentRepository.getReferenceById(studentId);
                    Week week = lessonRepository.getReferenceById(weekId);
                    return StudentLessonProgress.builder()
                            .student(student)
                            .week(week)
                            .build();
                });

        progress.complete(quizScore, passed);
        progressRepository.save(progress);

        log.info("Lesson COMPLETED: student={}, week={}, score={}, passed={}",
                studentId, weekId, quizScore, passed);

        // ✔ لا unlockNextLesson() — الـ access للحصة الجاية يتحدد تلقائياً
        //   بناءً على canAccessLesson() اللي بتشوف هل الحصة دي COMPLETED
    }

    // ─────────────────────────────────────────────────────────────
    // تسجيل تسليم الواجب في Progress Record
    // ─────────────────────────────────────────────────────────────

    @Transactional
    public void recordAssignmentSubmission(Long studentId, Long weekId, double score) {
        progressRepository.findByStudentIdAndWeekId(studentId, weekId)
                .ifPresent(p -> {
                    p.setAssignmentSubmitted(true);
                    p.setAssignmentScore(score);
                    progressRepository.save(p);
                    log.info("Assignment recorded: student={}, week={}, score={}",
                            studentId, weekId, score);
                });
    }

    // ─────────────────────────────────────────────────────────────
    // جلب حالة حصة معينة (للـ UI والـ dashboard)
    // ─────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public LessonProgressStatus getLessonStatus(Long studentId, Long weekId) {
        Optional<StudentLessonProgress> existing =
                progressRepository.findByStudentIdAndWeekId(studentId, weekId);

        if (existing.isPresent()) {
            return existing.get().getStatus();
        }

        // لو ما فيش record → حدد الحالة بناءً على الـ sequential check
        return canAccessLesson(studentId, weekId)
                ? LessonProgressStatus.UNLOCKED   // يقدر يبدأ
                : LessonProgressStatus.LOCKED;    // محتاج يخلص الحصة السابقة
    }

    // ─────────────────────────────────────────────────────────────
    // كل تقدم الطالب في Session معين
    // ─────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<StudentLessonProgress> getStudentProgressInSession(Long studentId, Long sessionId) {
        return progressRepository.findByStudentAndSession(studentId, sessionId);
    }

    // ─────────────────────────────────────────────────────────────
    // unlockFirstLesson — no-op في الـ design الجديد
    // ─────────────────────────────────────────────────────────────

    /**
     * @deprecated الـ design الجديد بيحدد الـ access dynamically.
     * الحصة الأولى دايماً متاحة بناءً على orderNumber check.
     * الميثود دي موجودة للـ backward compatibility فقط.
     */
    @Transactional
    public void unlockFirstLesson(Long studentId, Long sessionId) {
        log.debug("unlockFirstLesson called (no-op in new gate design): student={}, session={}",
                studentId, sessionId);
    }

    // ─────────────────────────────────────────────────────────────
    // LOCK_BY_ELEMENT: فحص وصول الطالب لمادة تعليمية بعينها
    // ─────────────────────────────────────────────────────────────

    /**
     * هل الطالب يقدر يشوف المادة دي؟
     * - NONE: حرية تامة
     * - LOCK_BY_SESSION: حرية داخل الحصة (بدون ترتيب للعناصر)
     * - LOCK_BY_ELEMENT: العنصر السابق بالترتيب لازم يكون اتشيّف أولاً
     */
    @Transactional(readOnly = true)
    public AccessCheckResult checkMaterialAccess(Long studentId, Long materialId) {
        String contentOrder = materialRepository.findContentOrderByMaterialId(materialId);

        // NONE أو LOCK_BY_SESSION → حرية كاملة في ترتيب العناصر داخل الحصة
        if (contentOrder == null || !("LOCK_BY_ELEMENT".equals(contentOrder))) {
            return AccessCheckResult.allowed();
        }

        // LOCK_BY_ELEMENT: جيب orderNumber للمادة الحالية
        Integer currentOrder = materialRepository.findOrderNumberById(materialId);
        if (currentOrder == null || currentOrder <= 0) {
            return AccessCheckResult.allowed(); // أول عنصر أو ما عندوش ترتيب
        }

        // جيب المادة السابقة في نفس الحصة
        List<LessonMaterial> prev = materialRepository
                .findPreviousMaterialsInSameWeek(materialId, currentOrder);

        if (prev.isEmpty()) {
            return AccessCheckResult.allowed(); // أول مادة في الحصة
        }

        LessonMaterial prevMaterial = prev.get(0);
        boolean prevViewed = materialViewRepository
                .existsByStudentIdAndMaterialId(studentId, prevMaterial.getId());

        if (prevViewed) {
            return AccessCheckResult.allowed();
        }

        String prevName = prevMaterial.getFileName() != null ? prevMaterial.getFileName() : "العنصر السابق";
        return AccessCheckResult.denied(
                "أكمل \"" + prevName + "\" أولاً قبل الوصول لهذا العنصر"
        );
    }

    /**
     * سجّل أن الطالب شاف المادة دي (يُستدعى عند فتح المادة بنجاح).
     * يُستخدم في LOCK_BY_ELEMENT لفتح العنصر التالي.
     */
    @Transactional
    public void markMaterialViewed(Long studentId, Long materialId) {
        markMaterialViewed(studentId, materialId, null);
    }

    /**
     * يسجّل مشاهدة الطالب لمادة (مع مدة المشاهدة لو كانت فيديو)، ثم يعيد احتساب
     * تقدّم الطالب في الكورس بالكامل بناءً على: طول الفيديوهات/مدة مشاهدتها + عدد أسئلة
     * الكويزات والواجبات — مع استبعاد الملفات (PDF/DOC/...) تماماً من الحساب.
     *
     * watchedSeconds: أقصى مدة وصل لها الطالب في الفيديو (null لو المادة مش فيديو أو غير معروفة)
     */
    @Transactional
    public void markMaterialViewed(Long studentId, Long materialId, Long watchedSeconds) {
        StudentMaterialView view = materialViewRepository
                .findByStudentIdAndMaterialId(studentId, materialId)
                .orElse(null);

        if (view == null) {
            view = StudentMaterialView.builder()
                    .studentId(studentId)
                    .materialId(materialId)
                    .watchedSeconds(watchedSeconds != null ? watchedSeconds : 0L)
                    .build();
            materialViewRepository.save(view);
            log.debug("Material viewed: student={}, material={}", studentId, materialId);
        } else if (watchedSeconds != null) {
            long current = view.getWatchedSeconds() == null ? 0L : view.getWatchedSeconds();
            if (watchedSeconds > current) {
                view.setWatchedSeconds(watchedSeconds);
                materialViewRepository.save(view);
            }
        }

        refreshEnrollmentProgress(studentId, materialId);
    }

    /**
     * يعيد احتساب نسبة تقدّم الطالب في الكورس المرتبط بالمادة دي:
     *   - الفيديوهات: توزن بنسبة (مدة المشاهدة ÷ طول الفيديو) — مفتوح بدون مدة معروفة = إنجاز كامل لمجرد الفتح
     *   - الكويزات: توزن بعدد أسئلتها، وتُحتسب "مكتملة" لو الطالب اجتازها (quizPassed)
     *   - الواجبات: توزن بعدد أسئلتها، وتُحتسب "مكتملة" لو الطالب سلّمها (assignmentSubmitted)
     *   - الملفات (PDF/DOC/IMAGE/...) مُستبعدة تماماً من الحساب
     */
    @Transactional
    public void refreshEnrollmentProgress(Long studentId, Long materialId) {
        Long courseId = materialRepository.findCourseIdById(materialId);
        if (courseId == null) return;

        Enrollment enrollment = enrollmentRepository
                .findByStudentIdAndCourseIdAndActiveTrue(studentId, courseId)
                .orElse(null);
        if (enrollment == null) return;

        // 1) الفيديوهات — موزونة بطول الفيديو ومدة المشاهدة
        List<LessonMaterial> videos = materialRepository.findVideoMaterialsByCourseId(courseId);
        double videoWeightDone = 0;
        if (!videos.isEmpty()) {
            List<Long> videoIds = videos.stream().map(LessonMaterial::getId).toList();
            Map<Long, StudentMaterialView> viewsByMaterial = materialViewRepository
                    .findByStudentIdAndMaterialIdIn(studentId, videoIds).stream()
                    .collect(Collectors.toMap(StudentMaterialView::getMaterialId, v -> v, (a, b) -> a));

            for (LessonMaterial video : videos) {
                StudentMaterialView v = viewsByMaterial.get(video.getId());
                if (v == null) continue;
                Long duration = video.getDurationSeconds();
                Long watched  = v.getWatchedSeconds();
                double ratio;
                if (duration != null && duration > 0 && watched != null) {
                    ratio = Math.min(1.0, watched.doubleValue() / duration.doubleValue());
                } else {
                    ratio = 1.0; // فتح الفيديو على الأقل بدون مدة معروفة = اعتبره مشاهَد
                }
                videoWeightDone += ratio;
            }
        }

        // 2) الكويزات — موزونة بعدد الأسئلة، مكتملة = ناجح فيها
        List<Quiz> quizzes = quizRepository.findByCourseId(courseId);
        double quizWeightTotal = 0, quizWeightDone = 0;
        for (Quiz quiz : quizzes) {
            int qCount = (quiz.getQuestions() == null || quiz.getQuestions().isEmpty()) ? 1 : quiz.getQuestions().size();
            quizWeightTotal += qCount;
            boolean passed = quiz.getWeek() != null && progressRepository
                    .findByStudentIdAndWeekId(studentId, quiz.getWeek().getId())
                    .map(p -> Boolean.TRUE.equals(p.getQuizPassed()))
                    .orElse(false);
            if (passed) quizWeightDone += qCount;
        }

        // 3) الواجبات — موزونة بعدد الأسئلة، مكتملة = تم التسليم
        List<Assignment> assignments = assignmentRepository.findByCourseId(courseId);
        double assignmentWeightTotal = 0, assignmentWeightDone = 0;
        for (Assignment assignment : assignments) {
            int qCount = (assignment.getQuestions() == null || assignment.getQuestions().isEmpty()) ? 1 : assignment.getQuestions().size();
            assignmentWeightTotal += qCount;
            boolean submitted = assignment.getWeek() != null && progressRepository
                    .findByStudentIdAndWeekId(studentId, assignment.getWeek().getId())
                    .map(p -> Boolean.TRUE.equals(p.getAssignmentSubmitted()))
                    .orElse(false);
            if (submitted) assignmentWeightDone += qCount;
        }

        double totalWeight = videos.size() + quizWeightTotal + assignmentWeightTotal;
        if (totalWeight <= 0) return;

        double doneWeight = videoWeightDone + quizWeightDone + assignmentWeightDone;
        double progressPercent = Math.min(100.0, (doneWeight / totalWeight) * 100.0);

        int totalCount = videos.size() + quizzes.size() + assignments.size();
        int completedCount = totalCount > 0 ? (int) Math.round(progressPercent / 100.0 * totalCount) : 0;

        enrollment.setTotalLessonsCount(totalCount);
        enrollment.setCompletedLessonsCount(completedCount);
        enrollment.updateProgress(progressPercent);
        enrollmentRepository.save(enrollment);

        log.debug("Progress refreshed: student={}, course={}, progress={}%, completed={}/{}",
                studentId, courseId, progressPercent, completedCount, totalCount);
    }

    // ─────────────────────────────────────────────────────────────
    // AccessCheckResult — نتيجة فحص الـ access مع سبب الرفض
    // ─────────────────────────────────────────────────────────────

    @lombok.Value
    public static class AccessCheckResult {
        boolean allowed;
        String  denialReason;

        public static AccessCheckResult allowed() {
            return new AccessCheckResult(true, null);
        }

        public static AccessCheckResult denied(String reason) {
            return new AccessCheckResult(false, reason);
        }
    }
}
