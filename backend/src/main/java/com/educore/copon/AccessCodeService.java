package com.educore.copon;

import com.educore.category.Category;
import com.educore.category.CategoryRepository;
import com.educore.course.Course;
import com.educore.course.CourseRepository;
import com.educore.unit.Session;
import com.educore.unit.SessionRepository;
import com.educore.dtocopon.*;
import com.educore.enrollment.EnrollmentService;
import com.educore.enrollment.EnrollmentType;
import com.educore.exception.*;
import com.educore.wallet.WalletService;
import com.educore.wallet.dto.WalletTopUpRequest;
import com.educore.student.Student;
import com.educore.student.StudentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.Valid;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@Validated
@RequiredArgsConstructor
public class AccessCodeService {

    private final AccessCodeRepository      codeRepo;
    private final AccessCodeUsageRepository usageRepo;
    private final CategoryRepository        categoryRepo;
    private final CourseRepository          courseRepo;
    private final SessionRepository         sessionRepo;
    private final StudentRepository         studentRepo;
    private final EnrollmentService         enrollmentService;
    private final WalletService             walletService;
    private final AccessCodeMapper mapper;

    private static final String ALPHABET     = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int    CODE_LENGTH  = 10;
    private static final SecureRandom RANDOM = new SecureRandom();

    /* ════════════════════════════════════════════════════
       GENERATE — المدرس يولد أكواد
    ════════════════════════════════════════════════════ */

    @Transactional
    public GenerateCodesResponse generateCodes(
            @Valid GenerateCodesRequest req, Long teacherId, String teacherName) {

        log.info("Teacher {} generating {} codes, type={}", teacherName, req.getCount(), req.getTargetType());

        // التحقق من صحة الطلب
        if (!req.isValidTarget()) {
            throw new IllegalArgumentException("نوع المنتج لا يتطابق مع المعرف المحدد");
        }

        String targetName = validateAndGetTargetName(req);
        List<String> generatedCodes = new ArrayList<>(req.getCount());
        List<AccessCode> toSave = new ArrayList<>(req.getCount());

        for (int i = 0; i < req.getCount(); i++) {
            String code = generateUniqueCode();
            generatedCodes.add(code);

            AccessCode ac = AccessCode.builder()
                    .code(code)
                    .targetType(req.getTargetType())
                    .createdById(teacherId)
                    .createdByName(teacherName)
                    .maxUses(req.getMaxUsesPerCode() != null ? req.getMaxUsesPerCode() : 1)
                    .usedCount(0)
                    .expiresAt(req.getExpiresAt())
                    .batchLabel(req.getBatchLabel())
                    .price(req.getPrice())
                    .active(true)
                    .build();

            switch (req.getTargetType()) {
                case CATEGORY -> ac.setCategory(categoryRepo.getReferenceById(req.getCategoryId()));
                case COURSE   -> ac.setCourse(courseRepo.getReferenceById(req.getCourseId()));
                case SESSION  -> ac.setSession(sessionRepo.getReferenceById(req.getSessionId()));
            }

            toSave.add(ac);
        }

        codeRepo.saveAll(toSave);

        log.info("Generated {} codes for teacher={}, target={}",
                generatedCodes.size(), teacherId, targetName);

        return GenerateCodesResponse.builder()
                .totalGenerated(generatedCodes.size())
                .targetType(req.getTargetType().name())
                .targetName(targetName)
                .batchLabel(req.getBatchLabel())
                .codes(generatedCodes)
                .expiresAt(req.getExpiresAt())
                .maxUsesPerCode(req.getMaxUsesPerCode() != null ? req.getMaxUsesPerCode() : 1)
                .price(req.getPrice())
                .sessionTitle(req.getTargetType() == CodeTargetType.SESSION ? targetName : null)
                .build();
    }

    /* ════════════════════════════════════════════════════
       REDEEM — الطالب يُدخل الكود
    ════════════════════════════════════════════════════ */

    @Transactional
    @CacheEvict(value = {"studentEnrollments", "courseEnrollments",
            "courseAccess", "accessibleCourses"}, allEntries = true)
    public RedeemCodeResponse redeemCode(@Valid RedeemCodeRequest request, Long studentId) {
        String rawCode = request.getCode();
        String code = rawCode.trim().toUpperCase();
        log.info("Student {} redeeming code: {}", studentId, code);

        AccessCode ac = codeRepo.findByCode(code)
                .orElseThrow(() -> new InvalidAccessCodeException("الكود غير صحيح"));

        if (!ac.isValid()) {
            if (ac.getExpiresAt() != null && ac.getExpiresAt().isBefore(LocalDateTime.now())) {
                throw new InvalidAccessCodeException("انتهت صلاحية الكود");
            }
            if (ac.getMaxUses() != null && ac.getUsedCount() >= ac.getMaxUses()) {
                throw new InvalidAccessCodeException("تم استخدام هذا الكود بالكامل");
            }
            throw new InvalidAccessCodeException("الكود غير صالح للاستخدام");
        }

        if (usageRepo.existsByAccessCodeIdAndStudentId(ac.getId(), studentId)) {
            throw new DuplicatePaymentException("لقد استخدمت هذا الكود من قبل");
        }

        // ── التحقق من تطابق الكورس والسعر ──────────────────────────────
        if (ac.getTargetType() == CodeTargetType.COURSE) {
            if (request.getCourseId() == null ||
                    !ac.getCourse().getId().equals(request.getCourseId())) {
                throw new InvalidAccessCodeException(
                        "هذا الكود مخصص لكورس آخر وليس للكورس المحدد");
            }

            // سعر الكود لازم يطابق سعر الكورس
            java.math.BigDecimal codePrice   = ac.getPrice() != null ? ac.getPrice() : java.math.BigDecimal.ZERO;
            java.math.BigDecimal coursePrice = ac.getCourse().getPrice() != null ? ac.getCourse().getPrice() : java.math.BigDecimal.ZERO;
            if (codePrice.compareTo(coursePrice) != 0) {
                throw new InvalidAccessCodeException(
                        "سعر الكود (" + codePrice + " ج.م) لا يطابق سعر الكورس (" + coursePrice + " ج.م)");
            }
        }

        // ── خصم سعر الكود من المحفظة (فقط للـ COURSE — WALLET يضيف مش يخصم) ──
        boolean isCourseCode = ac.getTargetType() == CodeTargetType.COURSE
                && ac.getPrice() != null
                && ac.getPrice().compareTo(java.math.BigDecimal.ZERO) > 0;

        if (isCourseCode) {
            if (!walletService.hasSufficientBalance(studentId, ac.getPrice())) {
                throw new InvalidAccessCodeException(
                        "رصيد المحفظة غير كافٍ — يلزم " + ac.getPrice() + " ج.م لتفعيل هذا الكود");
            }
        }

        Student student = studentRepo.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("الطالب غير موجود"));

        if (isCourseCode) {
            walletService.debitForPurchase(studentId, ac.getPrice(),
                    "شراء بكود: " + code,
                    "ACCESS_CODE:" + code);
        }

        List<String> unlockedCourses = new ArrayList<>();
        boolean walletCharged = false;

        switch (ac.getTargetType()) {
            case CATEGORY -> {
                Category category = ac.getCategory();
                for (Course course : category.getCourses()) {
                    enrollmentService.enrollAfterPayment(
                            studentId, course.getId(), category.getId(),
                            EnrollmentType.CATEGORY_PURCHASE, "ACCESS_CODE:" + code
                    ).ifPresent(e -> unlockedCourses.add(course.getTitle()));
                }
            }
            case COURSE -> {
                Course course = ac.getCourse();
                enrollmentService.enrollAfterPayment(
                        studentId, course.getId(), null,
                        EnrollmentType.COURSE_PURCHASE, "ACCESS_CODE:" + code
                ).ifPresent(e -> unlockedCourses.add(course.getTitle()));
            }
            case SESSION -> {
                // حصة معينة → نسجل الطالب في جميع الكورسات المرتبطة بهذه الحصة
                Session session = ac.getSession();
                for (Course course : session.getCourses()) {
                    enrollmentService.enrollAfterPayment(
                            studentId, course.getId(), null,
                            EnrollmentType.COURSE_PURCHASE, "ACCESS_CODE:SESSION:" + code
                    ).ifPresent(e -> unlockedCourses.add(course.getTitle() + " (حصة: " + session.getTitle() + ")"));
                }
            }
            case WALLET -> {
                java.math.BigDecimal codePrice = ac.getPrice() != null
                        ? ac.getPrice() : java.math.BigDecimal.ZERO;

                if (request.getCourseId() != null) {
                    // ── الطالب يستخدم الكود لشراء كورس محدد ──
                    Course target = courseRepo.findById(request.getCourseId())
                            .orElseThrow(() -> new ResourceNotFoundException("الكورس غير موجود"));

                    java.math.BigDecimal coursePrice = target.getPrice() != null
                            ? target.getPrice() : java.math.BigDecimal.ZERO;

                    if (codePrice.compareTo(coursePrice) != 0) {
                        throw new InvalidAccessCodeException(
                                "سعر الكود (" + codePrice + " ج.م) لا يطابق سعر الكورس (" + coursePrice + " ج.م)");
                    }

                    enrollmentService.enrollAfterPayment(
                            studentId, target.getId(), null,
                            EnrollmentType.COURSE_PURCHASE, "ACCESS_CODE:" + code
                    ).ifPresent(e -> unlockedCourses.add(target.getTitle()));
                } else {
                    // ── شحن محفظة فقط ──
                    if (codePrice.compareTo(java.math.BigDecimal.ZERO) > 0) {
                        WalletTopUpRequest topUp = new WalletTopUpRequest();
                        topUp.setStudentId(studentId);
                        topUp.setAmount(codePrice);
                        topUp.setDescription("شحن بكود: " + code);
                        walletService.topUp(topUp, "ACCESS_CODE");
                        walletCharged = true; // مش كورس — لا يُعدّ enrollment
                    }
                }
            }
        }

        ac.incrementUsage();
        codeRepo.save(ac);

        AccessCodeUsage usage = AccessCodeUsage.builder()
                .accessCode(ac)
                .studentId(studentId)
                .studentName(student.getFullName())
                .studentCode(student.getStudentCode())
                .enrollmentsCreated(walletCharged ? 0 : unlockedCourses.size())
                .build();
        usageRepo.save(usage);

        log.info("REDEEM_SUCCESS: code={}, student={}, unlocked={}, remainingUses={}",
                code, studentId, unlockedCourses.size(), ac.getRemainingUses());

        String targetName = switch (ac.getTargetType()) {
            case CATEGORY -> ac.getCategory() != null ? ac.getCategory().getName() : "باقة";
            case COURSE   -> ac.getCourse() != null ? ac.getCourse().getTitle() : "كورس";
            case SESSION  -> ac.getSession() != null ? ac.getSession().getTitle() : "حصة";
            case WALLET   -> "شحن محفظة";
        };

        String message;
        if (walletCharged) {
            message = "تم شحن محفظتك بـ " + ac.getPrice() + " ج.م بنجاح";
        } else if (unlockedCourses.isEmpty()) {
            message = "أنت مسجل في هذه الكورسات بالفعل";
        } else {
            message = "تم تفعيل " + unlockedCourses.size() + " كورس بنجاح";
        }

        return RedeemCodeResponse.builder()
                .success(true)
                .message(message)
                .targetType(ac.getTargetType().name())
                .targetName(targetName)
                .enrollmentsCreated(walletCharged ? 0 : unlockedCourses.size())
                .unlockedCourses(unlockedCourses)
                .build();
    }

    /* ════════════════════════════════════════════════════
       QUERIES — للمدرس
    ════════════════════════════════════════════════════ */

    public Page<AccessCodeDto> getTeacherCodes(Long teacherId, Pageable pageable) {
        return codeRepo.findByCreatedByIdOrderByCreatedAtDesc(teacherId, pageable)
                .map(mapper::toDto);
    }

    public List<AccessCodeDto> getBatch(Long teacherId, String batchLabel) {
        return codeRepo.findByCreatedByIdAndBatchLabelOrderByCreatedAtAsc(teacherId, batchLabel)
                .stream().map(mapper::toDto).collect(Collectors.toList());
    }

    public List<CodeUsageDto> getCodeUsages(Long codeId, Long teacherId) {
        AccessCode ac = codeRepo.findById(codeId)
                .orElseThrow(() -> new ResourceNotFoundException("الكود غير موجود"));

        if (!ac.getCreatedById().equals(teacherId)) {
            throw new SecurityException("ليس لديك صلاحية لرؤية هذا الكود");
        }

        return usageRepo.findByAccessCodeIdOrderByUsedAtDesc(codeId)
                .stream().map(mapper::toUsageDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public void deactivateCode(Long codeId, Long teacherId) {
        AccessCode ac = codeRepo.findById(codeId)
                .orElseThrow(() -> new ResourceNotFoundException("الكود غير موجود"));

        if (!ac.getCreatedById().equals(teacherId)) {
            throw new SecurityException("ليس لديك صلاحية لإلغاء هذا الكود");
        }

        ac.setActive(false);
        codeRepo.save(ac);
        log.info("Code {} deactivated by teacher {}", ac.getCode(), teacherId);
    }

    @Transactional
    public int deactivateBatch(String batchLabel, Long teacherId) {
        List<AccessCode> batch = codeRepo
                .findByCreatedByIdAndBatchLabelOrderByCreatedAtAsc(teacherId, batchLabel);

        batch.forEach(ac -> ac.setActive(false));
        codeRepo.saveAll(batch);

        log.info("Deactivated {} codes in batch '{}' by teacher {}",
                batch.size(), batchLabel, teacherId);
        return batch.size();
    }

    /* ════════════════════════════════════════════════════
       PRIVATE HELPERS
    ════════════════════════════════════════════════════ */

    private String generateUniqueCode() {
        String code;
        int attempts = 0;
        do {
            code = randomCode();
            attempts++;
            if (attempts > 50) {
                throw new RuntimeException("فشل توليد كود فريد بعد 50 محاولة");
            }
        } while (codeRepo.existsByCode(code));
        return code;
    }

    private String randomCode() {
        StringBuilder sb = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            sb.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
        }
        sb.insert(5, '-');
        return sb.toString();
    }

    private String validateAndGetTargetName(GenerateCodesRequest req) {
        return switch (req.getTargetType()) {
            case CATEGORY -> {
                if (req.getCategoryId() == null) throw new IllegalArgumentException("معرف الباقة مطلوب");
                yield categoryRepo.findById(req.getCategoryId())
                        .orElseThrow(() -> new ResourceNotFoundException("الباقة غير موجودة")).getName();
            }
            case COURSE -> {
                if (req.getCourseId() == null) throw new IllegalArgumentException("معرف الكورس مطلوب");
                yield courseRepo.findById(req.getCourseId())
                        .orElseThrow(() -> new ResourceNotFoundException("الكورس غير موجود")).getTitle();
            }
            case SESSION -> {
                if (req.getSessionId() == null) throw new IllegalArgumentException("معرف الحصة مطلوب");
                yield sessionRepo.findById(req.getSessionId())
                        .orElseThrow(() -> new ResourceNotFoundException("الحصة غير موجودة")).getTitle();
            }
            case WALLET -> "شحن محفظة";
        };
    }
}