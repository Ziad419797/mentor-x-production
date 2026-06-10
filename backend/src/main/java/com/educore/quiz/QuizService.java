package com.educore.quiz;

import com.educore.common.CacheNames;
import com.educore.common.SortValidator;
import com.educore.dtocourse.mapper.*;
import com.educore.dtocourse.request.*;
import com.educore.dtocourse.response.*;
import com.educore.exception.ResourceNotFoundException;
import com.educore.exception.ResourceAlreadyExistsException;
import com.educore.exception.UnauthorizedException;
import com.educore.lesson.LessonRepository;
import com.educore.lesson.Week;
import com.educore.lessongate.LessonGateService;
import com.educore.notification.NotificationService;
import com.educore.parent.Parent;
import com.educore.question.Question;
import com.educore.question.AnswerOption;
import com.educore.security.JwtUserPrincipal;
import com.educore.student.Student;
import com.educore.student.StudentAnswer;
import com.educore.student.StudentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class QuizService {

    private final QuizRepository quizRepository;
    private final StudentQuizAttemptRepository attemptRepository;
    private final QuizCreateMapper createMapper;
    private final QuizMapper quizMapper;
    private final QuizSubmitMapper submitMapper;
    private final QuizResultMapper resultMapper;
    private final SortValidator sortValidator;
    private final LessonRepository weekRepository;
    private final StudentAnswerRepository studentAnswerRepository;
    private final QuizValidator quizValidator;
    private final QuizScoreCalculator scoreCalculator;
    private final QuizAnswerMapper answerMapper;
    private final StudentRepository studentRepository;
    private final LessonGateService lessonGateService;
    private final NotificationService notificationService;
    private final com.educore.studentactivity.StudentActivityLogService studentActivityLogService;
    private static final List<String> ALLOWED_SORT_FIELDS =
            List.of("id", "title", "createdAt", "orderNumber");

    // ================= HELPER METHOD =================

    private Student getCurrentStudent() {
        try {
            JwtUserPrincipal principal = (JwtUserPrincipal) SecurityContextHolder
                    .getContext()
                    .getAuthentication()
                    .getPrincipal();

            Long userId = principal.getUserId();

            // جلب الطالب من قاعدة البيانات
            return studentRepository.findById(userId)
                    .orElseThrow(() -> new UnauthorizedException("الطالب غير موجود"));
        } catch (Exception e) {
            log.error("Failed to get current student: {}", e.getMessage());
            throw new UnauthorizedException("غير مصرح لك بالوصول");
        }
    }

    private Long getCurrentStudentId() {
        return getCurrentStudent().getId();
    }

    // ================= CREATE =================

    @CacheEvict(value = {CacheNames.QUIZZES_BY_WEEK}, allEntries = true)
    @Transactional
    public QuizResponse createQuiz(CreateQuizRequest request) {

        log.info("Creating quiz with title: {}", request.getTitle());
// 1. هاتي الأسبوع من الداتابيز باستخدام الـ ID اللي جاي في الـ Request
        Week week = weekRepository.findById(request.getWeekId())
                .orElseThrow(() -> new ResourceNotFoundException("Week not found"));
        Quiz quiz = createMapper.toEntity(request);
        // 3. الخطوة اللي ناقصة: اربطي الأسبوع بالـ Quiz يدوياً
        quiz.setWeek(week);
        Quiz saved = quizRepository.save(quiz);

        log.info("Quiz created successfully with id: {}", saved.getId());

        return quizMapper.toResponse(saved);
    }

    // ================= GET BY ID =================

    @Cacheable(value = CacheNames.QUIZZES, key = "#quizId")
    @Transactional(readOnly = true)
    public QuizResponse getQuiz(Long quizId) {

        log.info("Fetching quiz with id: {}", quizId);

        Quiz quiz = quizRepository.findWithQuestionsById(quizId)
                .orElseThrow(() -> new ResourceNotFoundException("Quiz not found with id: " + quizId));

        return quizMapper.toResponse(quiz);
    }

    // ================= LIST BY WEEK =================

    @Transactional(readOnly = true)
    @Cacheable(value = CacheNames.QUIZZES_BY_WEEK,
            key = "#weekId + '-' + #pageable.pageNumber")
    public Page<QuizResponse> getQuizzesByWeek(
            Long weekId,
            Pageable pageable
    ) {

        sortValidator.validate(pageable, ALLOWED_SORT_FIELDS);

        log.info("Fetching quizzes for week: {} page: {}", weekId, pageable.getPageNumber());

        Page<Quiz> page = quizRepository.findByWeekId(weekId, pageable);

        return page.map(quizMapper::toResponse);
    }

    // ================= START QUIZ =================

    @Transactional
    public QuizResultResponse startQuiz(Long quizId) {

        Student student = getCurrentStudent();  // 👈 جلب الطالب من SecurityContext
        Long studentId = student.getId();

        log.info("Student {} started quiz {}", studentId, quizId);
        Quiz quiz = quizRepository
                .findWithQuestionsByIdAndDeletedFalse(quizId)
                .orElseThrow(() -> new ResourceNotFoundException("الاختبار غير موجود بالرقم: " + quizId));

        if (!quiz.getActive()) {
            throw new IllegalStateException("الاختبار غير نشط بالرقم: " + quizId);
        }

        if (attemptRepository.existsByQuizAndStudent(quiz, student)) {
            throw new ResourceAlreadyExistsException(
                    String.format("الطالب %d قام بالفعل بمحاولة الاختبار %d", studentId, quizId)
            );
        }

        LocalDateTime now = LocalDateTime.now();

        StudentQuizAttempt attempt = StudentQuizAttempt.builder()
                .quiz(quiz)
                .student(student)
                .startedAt(now)
                .expiresAt(
                        quiz.getTimeRestricted()
                                ? now.plusMinutes(quiz.getDurationMinutes())
                                : null
                )
                .submitted(false)
                .score(0)
                .build();

        attemptRepository.save(attempt);

        int totalMarks = scoreCalculator.calculateTotalMarks(quiz);

        log.info("Quiz started successfully - attempt id: {}", attempt.getId());

        return QuizResultResponse.builder()
                .score(0)
                .totalMarks(totalMarks)
                .build();    }
    // ================= DELETE QUIZ (SOFT DELETE) =================

    @Transactional
    @CacheEvict(value = {
            CacheNames.QUIZZES,
            CacheNames.QUIZZES_BY_WEEK
    }, allEntries = true)
    public void deleteQuiz(Long quizId) {

        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new ResourceNotFoundException("الاختبار غير موجود بالرقم: " + quizId));

        // التحقق من عدم وجود محاولات نشطة
        if (attemptRepository.existsByQuizIdAndSubmittedFalse(quizId)) {
            throw new IllegalStateException("لا يمكن حذف الاختبار لوجود محاولات نشطة");
        }

        quiz.setDeleted(true);
        quiz.setActive(false); // إلغاء تنشيط الاختبار أيضاً

        log.info("Quiz soft-deleted: {}", quizId);
    }
    // ================= SUBMIT QUIZ =================

    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = {CacheNames.LEADERBOARD_QUIZ, CacheNames.LEADERBOARD_COURSE, CacheNames.LEADERBOARD_GLOBAL}, allEntries = true)
    public QuizResultResponse submitQuiz(
            Long quizId,
            SubmitQuizRequest request
    ) {

        Student student = getCurrentStudent();  // 👈 جلب الطالب من SecurityContext
        Long studentId = student.getId();

        // التحقق من صحة الطلب
        quizValidator.validateSubmitRequest(request, quizId);
        Quiz quiz = quizRepository
                .findWithQuestionsByIdAndDeletedFalse(quizId)
                .orElseThrow(() -> new ResourceNotFoundException("الاختبار غير موجود بالرقم: " + quizId));

        // جلب المحاولة مع قفل Pessimistic لمنع التحديث المتزامن
        StudentQuizAttempt attempt = attemptRepository
                .findByQuizAndStudentWithLock(quiz.getId(), student)  // 👈 تعديل الاستعلام
                .orElseThrow(() -> new IllegalStateException(
                        String.format("الطالب %d لم يبدأ الاختبار بعد", studentId)
                ));

        // التحقق من أن المحاولة لم تُسلم بعد
        if (attempt.getSubmitted()) {
            throw new IllegalStateException("تم تسليم هذا الاختبار بالفعل");
        }

        // التحقق من انتهاء الوقت
        if (quizValidator.isQuizExpired(attempt)) {
            attempt.setSubmitted(true);
            attempt.setSubmittedAt(LocalDateTime.now());
            attemptRepository.save(attempt);
            throw new IllegalStateException("انتهى وقت الاختبار: " + quizId);
        }



        Map<Long, String> answers = submitMapper.toAnswerMap(request);
        // التحقق من أن جميع الأسئلة تمت الإجابة عليها
        quizValidator.validateAllQuestionsAnswered(quiz, answers);

        // حساب الدرجة
        QuizScoreCalculator.ScoreCalculationResult result =   scoreCalculator.calculateScore(quiz, answers);


        // تحديث المحاولة
        boolean passed = result.getPercentage() >= 50.0;

        attempt.setScore(result.getScore());
        attempt.setSubmitted(true);
        attempt.setSubmittedAt(LocalDateTime.now());

        attemptRepository.save(attempt);

        // حفظ إجابات الطالب للرجوع إليها لاحقاً
        Map<Long, Question> questionMap = quiz.getQuestions().stream()
                .collect(Collectors.toMap(Question::getId, q -> q));
        List<StudentAnswer> studentAnswers = answerMapper.toStudentAnswersWithQuestions(attempt, answers, questionMap);
        studentAnswerRepository.saveAll(studentAnswers);

        // ─── تحديد المسار: إكمال الحصة وفتح التالية إن نجح ───────────
        Long weekId = quiz.getWeek().getId();
        try {
            lessonGateService.completeLesson(studentId, weekId, result.getPercentage(), passed);
        } catch (Exception e) {
            // الـ gate لا يوقف عملية التسليم — يُسجَّل فقط
            log.warn("LessonGate update skipped for student={}, week={}: {}", studentId, weekId, e.getMessage());
        }

        // ─── إشعارات: الطالب + ولي الأمر (async) ────────────────────
        // notifyQuizResult يرسل إشعار للـ parent ولإشعار للطالب نفسه معاً
        Parent parent = student.getParent();
        if (parent != null) {
            notificationService.notifyQuizResult(
                    parent.getId(),
                    student.getFullName(),
                    quiz.getTitle(),
                    result.getScore(),
                    passed,
                    attempt.getId(),
                    studentId);
        } else {
            // لو ما فيش ولي أمر — بلّغ الطالب مباشرة بدون إشعار PARENT
            notificationService.send(
                    studentId, "STUDENT",
                    passed ? "🎉 نجحت في الكويز!" : "📚 راجع الحصة",
                    (passed ? "أحسنت! حصلت على " : "حصلت على ") + result.getScore()
                        + " — " + (passed ? "الحصة التالية اتفتحت!" : "راجع الحصة وحاول تاني"),
                    passed ? com.educore.notification.NotificationType.QUIZ_PASSED
                           : com.educore.notification.NotificationType.QUIZ_FAILED,
                    attempt.getId(), "QUIZ_ATTEMPT", studentId);
        }

        log.info("Quiz submitted - student: {}, quiz: {}, score: {}/{}, passed: {}",
                studentId, quizId, result.getScore(), result.getTotalMarks(), passed);

        studentActivityLogService.log(
                studentId, student.getFullName(),
                com.educore.studentactivity.StudentEventType.QUIZ_SUBMITTED,
                "حل امتحان: " + quiz.getTitle(),
                "الدرجة: " + result.getScore() + "/" + result.getTotalMarks() + " — " + (passed ? "ناجح" : "راسب")
        );

        return QuizResultResponse.builder()
                .attemptId(attempt.getId())
                .quizId(quizId)
                .quizTitle(quiz.getTitle())
                .studentId(studentId)
                .score(result.getScore())
                .totalMarks(result.getTotalMarks())
                .percentage(result.getPercentage())
                .passed(passed)
                .submitted(true)
                .startedAt(resultMapper.formatDate(attempt.getStartedAt()))
                .submittedAt(resultMapper.formatDate(attempt.getSubmittedAt()))
                .expiresAt(resultMapper.formatDate(attempt.getExpiresAt()))
                .build();
    }
// ================= GET ALL QUIZZES (PAGINATED) =================

    @Transactional(readOnly = true)
    @Cacheable(value = CacheNames.QUIZZES, key = "'all-' + #pageable.pageNumber + '-' + #pageable.pageSize")
    public Page<QuizResponse> getAllQuizzes(Pageable pageable) {

        sortValidator.validate(pageable, ALLOWED_SORT_FIELDS);

        log.info("Fetching all quizzes - page: {}, size: {}",
                pageable.getPageNumber(), pageable.getPageSize());

        Page<Quiz> page = quizRepository.findAllByDeletedFalseWithQuestions(pageable);

        return page.map(quizMapper::toResponse);
    }


}