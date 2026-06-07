package com.educore.payment.payment;

import com.educore.category.Category;
import com.educore.category.CategoryRepository;
import com.educore.coupon.CouponService;
import com.educore.course.Course;
import com.educore.course.CourseRepository;
import com.educore.dtopayment.*;
import com.educore.dtopayment.mapper.PaymentMapper;
import com.educore.enrollment.*;
import com.educore.exception.DuplicatePaymentException;
import com.educore.exception.InsufficientBalanceException;
import com.educore.exception.ResourceNotFoundException;

import com.educore.payment.order.Order;
import com.educore.payment.order.OrderItem;
import com.educore.payment.order.OrderRepository;
import com.educore.student.Student;
import com.educore.student.StudentRepository;
import com.educore.wallet.Wallet;
import com.educore.wallet.WalletRepository;
import com.educore.wallet.WalletTransaction;
import com.educore.wallet.WalletTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final OrderRepository             orderRepo;
    private final PaymentRepository           paymentRepo;
    private final WalletRepository            walletRepo;
    private final WalletTransactionRepository walletTxRepo;
    private final StudentRepository           studentRepo;
    private final CategoryRepository          categoryRepo;
    private final CourseRepository            courseRepo;
    private final EnrollmentService           enrollmentService;
    private final PaymentMapper               mapper;
    private final PaymentGatewayService       gatewayService;
    private final CacheManager                cacheManager;
    private final CouponService couponService;  // ← أضف

    /* ════════════════════════════════════════════════════
       1. CREATE ORDER
    ════════════════════════════════════════════════════ */

    @Transactional
    public OrderDto createOrder(Long studentId, CreateOrderRequest request) {
        log.info("Creating order: student={}, items={}", studentId, request.getItems().size());
        Student student = findStudent(studentId);
        Order   order   = buildOrder(student, request.getItems());
        Order   saved   = orderRepo.save(order);
        log.info("Order created: {}", saved.getOrderNumber());
        return mapper.toOrderDto(saved);
    }

    /* ════════════════════════════════════════════════════
       2. PROCESS PAYMENT — بطاقة / فوري / فودافون / محفظة فقط
       ❌ كاش و تحويل بنكي محذوفان — استخدم أكواد الوصول بدلاً منهما
    ════════════════════════════════════════════════════ */

    @Transactional
    public PaymentResultDto processPayment(Long studentId, PaymentRequestDto request) {
        log.info("Processing payment: order={}, method={}, student={}, coupon={}",
                request.getOrderId(), request.getPaymentMethod(), studentId, request.getCouponCode());

        // ── رفض الطرق المحذوفة ──
        if (request.getPaymentMethod() == PaymentMethod.CASH ||
                request.getPaymentMethod() == PaymentMethod.BANK_TRANSFER) {
            throw new IllegalArgumentException(
                    "طريقة الدفع غير مدعومة. استخدم: محفظة، بطاقة، فوري، أو فودافون كاش.");
        }

        Order order = orderRepo.findById(request.getOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("الطلب غير موجود"));

        if (!order.getStudent().getId().equals(studentId))
            throw new SecurityException("ليس لديك صلاحية الدفع لهذا الطلب");

        if (!order.isPending())
            return PaymentResultDto.failed("الطلب ليس في حالة انتظار الدفع", order.getOrderNumber());

        // ── Idempotency: هل في دفع ناجح لهذا الطلب مسبقاً؟ ──
        Optional<Payment> existingPayment = paymentRepo.findByOrderId(order.getId());
        if (existingPayment.isPresent() && existingPayment.get().isCompleted()) {
            log.warn("Duplicate payment attempt for order={}", order.getOrderNumber());
            throw new DuplicatePaymentException(
                    "تم الدفع مسبقاً لهذا الطلب. رقم المعاملة: " +
                            existingPayment.get().getTransactionId());
        }

        // ── تطبيق الكوبون (الخصم الفعلي والتسجيل) ──
        BigDecimal discountAmount = BigDecimal.ZERO;
        BigDecimal finalAmount = order.getTotal();
        String appliedCouponCode = null;

        if (request.getCouponCode() != null && !request.getCouponCode().trim().isEmpty()) {
            try {
                // 🔥 هنا يتم الخصم الفعلي والتسجيل في coupon_redemption
                discountAmount = couponService.redeemCoupon(
                        request.getCouponCode().trim().toUpperCase(),
                        studentId,
                        order.getId(),
                        order.getTotal()
                );
                finalAmount = order.getTotal().subtract(discountAmount);
                appliedCouponCode = request.getCouponCode().trim().toUpperCase();

                log.info("Coupon applied: code={}, original={}, discount={}, final={}",
                        appliedCouponCode, order.getTotal(), discountAmount, finalAmount);

            } catch (IllegalArgumentException e) {
                log.warn("Invalid coupon: {}", e.getMessage());
                return PaymentResultDto.failed("كود الخصم غير صالح: " + e.getMessage(),
                        order.getOrderNumber());
            }
        }

        // ── معالجة الدفع بالمبلغ النهائي بعد الخصم ──
        if (request.getPaymentMethod() == PaymentMethod.WALLET)
            return processWalletPayment(studentId, order, finalAmount, discountAmount, appliedCouponCode);

        return processExternalPayment(request, order, finalAmount, discountAmount, appliedCouponCode);
    }

    /* ── دفع من المحفظة مع الخصم ── */
    private PaymentResultDto processWalletPayment(Long studentId, Order order,
                                                  BigDecimal finalAmount,
                                                  BigDecimal discountAmount,
                                                  String couponCode) {
        Wallet wallet = walletRepo.findByStudentIdWithLock(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("المحفظة غير موجودة. يرجى الشحن أولاً."));

        // التحقق من الرصيد بالمبلغ بعد الخصم
        if (!wallet.hasSufficientBalance(finalAmount))
            return PaymentResultDto.failed(
                    String.format("الرصيد غير كافٍ. المطلوب بعد الخصم: %.2f، المتاح: %.2f",
                            finalAmount, wallet.getBalance()),
                    order.getOrderNumber());

        try {
            // خصم المبلغ النهائي فقط
            wallet.debit(finalAmount);
            walletRepo.save(wallet);

            // بناء وصف المعاملة مع تفاصيل الخصم
            String transactionDesc = buildTransactionDescription(order, discountAmount, couponCode);

            WalletTransaction wtx = buildWalletTx(wallet, TransactionType.PURCHASE,
                    finalAmount.negate(), wallet.getBalance(), PaymentMethod.WALLET,
                    order.getOrderNumber(), transactionDesc);
            wtx.complete();
            walletTxRepo.save(wtx);

            return finalizeAndEnroll(order, PaymentMethod.WALLET,
                    "WALLET-" + System.currentTimeMillis(),
                    "Wallet payment" + (couponCode != null ? " with coupon " + couponCode : ""),
                    finalAmount, discountAmount, couponCode);

        } catch (Wallet.InsufficientBalanceException ex) {
            throw new InsufficientBalanceException(ex.getMessage());
        }
    }
    /* ── دفع من المحفظة ── */
//    private PaymentResultDto processWalletPayment(Long studentId, Order order) {
//        Wallet wallet = walletRepo.findByStudentIdWithLock(studentId)
//                .orElseThrow(() -> new ResourceNotFoundException("المحفظة غير موجودة. يرجى الشحن أولاً."));
//
//        if (!wallet.hasSufficientBalance(order.getTotal()))
//            return PaymentResultDto.failed(
//                    String.format("الرصيد غير كافٍ. المطلوب: %.2f، المتاح: %.2f",
//                            order.getTotal(), wallet.getBalance()),
//                    order.getOrderNumber());
//
//        try {
//            wallet.debit(order.getTotal());
//            walletRepo.save(wallet);
//
//            WalletTransaction wtx = buildWalletTx(wallet, TransactionType.PURCHASE,
//                    order.getTotal(), wallet.getBalance(), PaymentMethod.WALLET,
//                    order.getOrderNumber(), "شراء: " + buildItemsSummary(order));
//            wtx.complete();
//            walletTxRepo.save(wtx);
//
//            return finalizeAndEnroll(order, PaymentMethod.WALLET,
//                    "WALLET-" + System.currentTimeMillis(), "Wallet payment");
//
//        } catch (Wallet.InsufficientBalanceException ex) {
//            throw new InsufficientBalanceException(ex.getMessage());
//        }
//    }

    /**
     * بوابات خارجية — الآن عبر Fawaterek (بطاقة / فوري / فودافون كاش).
     *
     * الـ Flow:
     *   1. نُنشئ Payment بحالة PENDING
     *   2. نُرسل الطلب لـ Fawaterek → يرجع invoiceId + redirectUrl
     *   3. نُحدّث Payment لـ PROCESSING مع الـ invoiceId
     *   4. نُرسل الـ redirectUrl للفرونتند — الطالب يدفع على صفحة Fawaterek
     *   5. Fawaterek بتبعت callback → FawaterekCallbackController يكمّل العملية
     */
    private PaymentResultDto processExternalPayment(PaymentRequestDto req, Order order,
                                                    BigDecimal finalAmount,
                                                    BigDecimal discountAmount,
                                                    String couponCode) {
        // ننشئ Payment بالسعر بعد الخصم (ملاحظة: السعر الأصلي محفوظ في order)
        Payment payment = Payment.builder()
                .order(order)
                .paymentMethod(req.getPaymentMethod())
                .amount(finalAmount)  // ← المبلغ بعد الخصم
                .currency("EGP")
                .discountAmount(discountAmount)  // ← لو عندك حقل discountAmount في Payment
                .couponCode(couponCode)          // ← لو عندك حقل couponCode في Payment
                .build();
        paymentRepo.save(payment);

        try {
            // تعديل الـ request عشان يبعت المبلغ بعد الخصم للبوابة
            PaymentRequestDto modifiedRequest = PaymentRequestDto.builder()
                    .orderId(req.getOrderId())
                    .paymentMethod(req.getPaymentMethod())
                    .amount(finalAmount)  // ← المبلغ بعد الخصم
                    .couponCode(couponCode)
                    .build();

            PaymentGatewayService.GatewayResult result = gatewayService.processPayment(modifiedRequest, order);

            // ── Fawaterek Redirect ──
            if (result.requiresRedirect()) {
                payment.markAsProcessing(result.transactionId());
                paymentRepo.save(payment);

                log.info("Fawaterek redirect: order={}, invoiceId={}, finalAmount={}, discount={}, url={}",
                        order.getOrderNumber(), result.transactionId(), finalAmount, discountAmount, result.redirectUrl());

                return PaymentResultDto.builder()
                        .success(false)
                        .pending(true)
                        .message(result.message())
                        .orderNumber(order.getOrderNumber())
                        .redirectUrl(result.redirectUrl())
                        .discountAmount(discountAmount)  // ← أضف للـ response
                        .originalAmount(order.getTotal())
                        .finalAmount(finalAmount)
                        .build();
            }

            // ── نجاح فوري ──
            if (result.success()) {
                payment.complete(result.transactionId(), result.message());
                order.markAsPaid(req.getPaymentMethod());
                paymentRepo.save(payment);
                orderRepo.save(order);

                int enrollments = createEnrollmentsFromOrder(order);
                evictStudentCaches(order.getStudent().getId());

                return PaymentResultDto.successWithDiscount(
                        order.getOrderNumber(), result.transactionId(),
                        order.getTotal(), finalAmount, discountAmount, enrollments);
            }

            // ── انتظار موافقة يدوية ──
            if (result.pendingApproval()) {
                payment.markAsProcessing(result.transactionId());
                order.markAsAwaitingApproval(req.getPaymentMethod());
                paymentRepo.save(payment);
                orderRepo.save(order);
                return PaymentResultDto.pending(order.getOrderNumber(), result.message());
            }

            // ── فشل ──
            payment.fail(result.message());
            order.markAsFailed(result.message());
            paymentRepo.save(payment);
            orderRepo.save(order);
            return PaymentResultDto.failed(result.message(), order.getOrderNumber());

        } catch (Exception ex) {
            log.error("Payment exception order={}: {}", order.getOrderNumber(), ex.getMessage(), ex);
            payment.fail(ex.getMessage());
            order.markAsFailed(ex.getMessage());
            paymentRepo.save(payment);
            orderRepo.save(order);
            throw new RuntimeException("حدث خطأ أثناء الدفع. يرجى المحاولة مرة أخرى.");
        }
    }

    private PaymentResultDto finalizeAndEnroll(
            Order order, PaymentMethod method, String txnId, String resp,
            BigDecimal finalAmount, BigDecimal discountAmount, String couponCode) {

        Payment payment = Payment.builder()
                .order(order)
                .paymentMethod(method)
                .amount(finalAmount)  // ← المبلغ بعد الخصم
                .currency("EGP")
                .discountAmount(discountAmount)
                .couponCode(couponCode)
                .build();
        payment.complete(txnId, resp);
        order.markAsPaid(method);

        // اختياري: حفظ مبلغ الخصم في الـ order
        order.setDiscountAmount(discountAmount);
        order.setFinalAmount(finalAmount);
        order.setAppliedCouponCode(couponCode);

        paymentRepo.save(payment);
        orderRepo.save(order);

        int enrollments = createEnrollmentsFromOrder(order);
        evictStudentCaches(order.getStudent().getId());

        return PaymentResultDto.successWithDiscount(
                order.getOrderNumber(), txnId,
                order.getTotal(), finalAmount, discountAmount, enrollments);
    }
    // Helper لبناء وصف المعاملة
    private String buildTransactionDescription(Order order, BigDecimal discountAmount, String couponCode) {
        String items = buildItemsSummary(order);
        if (discountAmount != null && discountAmount.compareTo(BigDecimal.ZERO) > 0) {
            return String.format("شراء: %s (خصم: %.2f جنيه - كود: %s)",
                    items, discountAmount, couponCode);
        }
        return "شراء: " + items;
    }
//    private PaymentResultDto finalizeAndEnroll(
//            Order order, PaymentMethod method, String txnId, String resp) {
//
//        Payment payment = Payment.builder()
//                .order(order).paymentMethod(method)
//                .amount(order.getTotal()).currency("EGP")
//                .build();
//        payment.complete(txnId, resp);
//        order.markAsPaid(method);
//        paymentRepo.save(payment);
//        orderRepo.save(order);
//
//        int enrollments = createEnrollmentsFromOrder(order);
//        evictStudentCaches(order.getStudent().getId());
//
//        return PaymentResultDto.success(order.getOrderNumber(), txnId, order.getTotal(), enrollments);
//    }

    /* ════════════════════════════════════════════════════
       3. WALLET
    ════════════════════════════════════════════════════ */

    @Transactional
    public WalletTransactionDto depositToWallet(Long studentId, DepositRequestDto req) {
        log.info("Deposit: {} EGP via {} for student={}", req.getAmount(), req.getPaymentMethod(), studentId);

        // ── Idempotency للإيداع ──
        String idempotencyKey = "DEP-" + studentId + "-" + req.getAmount() + "-" +
                (req.getIdempotencyKey() != null ? req.getIdempotencyKey() : "");

        Wallet wallet = walletRepo.findByStudentIdWithLock(studentId)
                .orElseGet(() -> createWallet(studentId));

        PaymentGatewayService.GatewayResult result = gatewayService.processDeposit(req, studentId);

        if (!result.success())
            throw new RuntimeException("فشل الإيداع: " + result.message());

        wallet.credit(req.getAmount());
        walletRepo.save(wallet);

        WalletTransaction wtx = buildWalletTx(wallet, TransactionType.DEPOSIT,
                req.getAmount(), wallet.getBalance(), req.getPaymentMethod(),
                result.transactionId(), "إيداع عبر " + req.getPaymentMethod().name());
        wtx.complete();

        // ── Cache evict targeted ──
        evictWalletCache(studentId);

        return mapper.toWalletTransactionDto(walletTxRepo.save(wtx));
    }

    @Cacheable(value = "walletCache", key = "#studentId")
    @Transactional   // ليس readOnly — قد ينشئ محفظة جديدة عند أول استدعاء
    public WalletDto getWallet(Long studentId) {
        Wallet wallet = walletRepo.findByStudentId(studentId)
                .orElseGet(() -> createWallet(studentId));
        return mapper.toWalletDto(wallet);
    }

    @Transactional(readOnly = true)
    public BigDecimal getWalletBalance(Long studentId) {
        return walletRepo.findByStudentId(studentId)
                .map(Wallet::getBalance).orElse(BigDecimal.ZERO);
    }

    @Transactional(readOnly = true)
    public Page<WalletTransactionDto> getWalletTransactions(Long studentId, Pageable pageable) {
        return walletTxRepo
                .findByWalletStudentIdOrderByCreatedAtDesc(studentId, pageable)
                .map(mapper::toWalletTransactionDto);
    }

    /* ════════════════════════════════════════════════════
       4. ORDER QUERIES
    ════════════════════════════════════════════════════ */

    @Transactional(readOnly = true)
    public OrderDto getOrderByNumber(String orderNumber) {
        return orderRepo.findByOrderNumber(orderNumber)
                .map(mapper::toOrderDto)
                .orElseThrow(() -> new ResourceNotFoundException("الطلب غير موجود: " + orderNumber));
    }

    @Transactional(readOnly = true)
    public Page<OrderDto> getStudentOrders(Long studentId, Pageable pageable) {
        return orderRepo.findByStudentIdOrderByCreatedAtDesc(studentId, pageable)
                .map(mapper::toOrderDto);
    }

    /* ════════════════════════════════════════════════════
       ENROLLMENT CREATION
    ════════════════════════════════════════════════════ */

    private int createEnrollmentsFromOrder(Order order) {
        int count = 0;
        Long studentId = order.getStudent().getId();

        for (OrderItem item : order.getItems()) {
            if ("CATEGORY".equals(item.getProductType())) {
                for (Course course : item.getCategory().getCourses()) {
                    boolean created = enrollmentService.enrollAfterPayment(
                            studentId, course.getId(), item.getCategory().getId(),
                            EnrollmentType.CATEGORY_PURCHASE, "PAYMENT_SYSTEM"
                    ).isPresent();
                    if (created) count++;
                }
            } else {
                boolean created = enrollmentService.enrollAfterPayment(
                        studentId, item.getCourse().getId(), null,
                        EnrollmentType.COURSE_PURCHASE, "PAYMENT_SYSTEM"
                ).isPresent();
                if (created) count++;
            }
        }

        log.info("Created {} enrollments for order={}", count, order.getOrderNumber());
        return count;
    }

    /* ════════════════════════════════════════════════════
       TARGETED CACHE EVICTION
    ════════════════════════════════════════════════════ */

    private void evictStudentCaches(Long studentId) {
        evictKey("studentEnrollments", studentId);
        evictKey("accessibleCourses",  studentId);
        evictKey("walletCache",        studentId);
    }

    private void evictWalletCache(Long studentId) {
        evictKey("walletCache", studentId);
    }

    private void evictKey(String cacheName, Object key) {
        var cache = cacheManager.getCache(cacheName);
        if (cache != null) cache.evict(key);
    }

    /* ════════════════════════════════════════════════════
       PRIVATE HELPERS
    ════════════════════════════════════════════════════ */

    private Order buildOrder(Student student, List<PurchaseItemDto> items) {
        Order order = Order.builder()
                .orderNumber(generateOrderNumber())
                .student(student)
                .build();
        for (PurchaseItemDto item : items)
            order.addItem(buildOrderItem(item));
        return order;
    }

    private OrderItem buildOrderItem(PurchaseItemDto item) {
        if ("CATEGORY".equalsIgnoreCase(item.getProductType())) {
            Category cat = categoryRepo.findById(item.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException("الباقة غير موجودة: " + item.getProductId()));
            if (cat.getPrice() == null)
                throw new IllegalArgumentException("الباقة ليس لها سعر: " + cat.getName());
            return OrderItem.builder()
                    .productType("CATEGORY").category(cat)
                    .productName(cat.getName()).unitPrice(cat.getPrice()).build();
        } else {
            Course course = courseRepo.findById(item.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException("الكورس غير موجود: " + item.getProductId()));
            if (course.getPrice() == null)
                throw new IllegalArgumentException("الكورس ليس له سعر: " + course.getTitle());
            return OrderItem.builder()
                    .productType("COURSE").course(course)
                    .productName(course.getTitle()).unitPrice(course.getPrice()).build();
        }
    }

    private Wallet createWallet(Long studentId) {
        Wallet wallet = Wallet.builder().student(findStudent(studentId)).build();
        return walletRepo.save(wallet);
    }

    private Student findStudent(Long id) {
        return studentRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("الطالب غير موجود: " + id));
    }

    private WalletTransaction buildWalletTx(Wallet wallet, TransactionType type,
                                            BigDecimal amount, BigDecimal after, PaymentMethod method, String ref, String desc) {
        return WalletTransaction.builder()
                .wallet(wallet).transactionNumber(generateTxnNumber())
                .type(type).amount(amount).balanceAfter(after)
                .paymentMethod(method).referenceId(ref).description(desc).build();
    }

    private String buildItemsSummary(Order order) {
        return order.getItems().stream()
                .map(OrderItem::getProductName)
                .reduce((a, b) -> a + "، " + b).orElse("منتجات");
    }

    private String generateOrderNumber() {
        return "ORD-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
                + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private String generateTxnNumber() {
        return "TXN-" + System.currentTimeMillis()
                + "-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }
}