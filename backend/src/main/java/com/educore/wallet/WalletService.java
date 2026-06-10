package com.educore.wallet;

import com.educore.exception.ResourceNotFoundException;
import com.educore.payment.payment.TransactionStatus;
import com.educore.payment.payment.TransactionType;
import com.educore.student.Student;
import com.educore.student.StudentRepository;
import com.educore.wallet.dto.WalletResponse;
import com.educore.wallet.dto.WalletTopUpRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class WalletService {

    private final WalletRepository            walletRepository;
    private final WalletTransactionRepository txRepository;
    private final StudentRepository           studentRepository;
    private final com.educore.studentactivity.StudentActivityLogService studentActivityLogService;

    // ─── جلب أو إنشاء محفظة ────────────────────────────────────

    @Transactional
    public Wallet getOrCreateWallet(Long studentId) {
        return walletRepository.findByStudentId(studentId).orElseGet(() -> {
            Student student = studentRepository.findById(studentId)
                    .orElseThrow(() -> new ResourceNotFoundException("الطالب غير موجود: " + studentId));
            Wallet wallet = Wallet.builder()
                    .student(student)
                    .balance(BigDecimal.ZERO)
                    .totalDeposited(BigDecimal.ZERO)
                    .totalSpent(BigDecimal.ZERO)
                    .totalRefunded(BigDecimal.ZERO)
                    .isActive(true)
                    .isVerified(true)
                    .build();
            log.info("Created wallet for student={}", studentId);
            return walletRepository.save(wallet);
        });
    }

    // ─── عرض المحفظة ────────────────────────────────────────────

    @Transactional(readOnly = true)
    public WalletResponse getWallet(Long studentId) {
        Wallet wallet = walletRepository.findByStudentId(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("المحفظة غير موجودة"));

        BigDecimal effective = txRepository.computeEffectiveBalance(studentId);

        return toResponse(wallet, effective,
                txRepository.findByWalletStudentIdOrderByCreatedAtDesc(studentId, Pageable.ofSize(20))
                        .getContent());
    }

    // ─── شحن المحفظة (أدمن / مدرس) ─────────────────────────────

    @Transactional
    public WalletResponse topUp(WalletTopUpRequest request, String topUpBy) {
        Wallet wallet = walletRepository.findByStudentIdWithLock(request.getStudentId())
                .orElseGet(() -> getOrCreateWallet(request.getStudentId()));

        LocalDateTime expiresAt = resolveExpiry(request);

        wallet.credit(request.getAmount());
        walletRepository.save(wallet);

        WalletTransaction tx = WalletTransaction.builder()
                .wallet(wallet)
                .transactionNumber(generateTxNumber())
                .type(TransactionType.DEPOSIT)
                .status(TransactionStatus.COMPLETED)
                .amount(request.getAmount())
                .balanceAfter(wallet.getBalance())
                .description(request.getDescription() != null
                        ? request.getDescription()
                        : "شحن رصيد بواسطة " + topUpBy)
                .expiresAt(expiresAt)
                .completedAt(LocalDateTime.now())
                .build();
        txRepository.save(tx);

        log.info("Wallet topped-up: student={}, amount={}, expiresAt={}, by={}",
                request.getStudentId(), request.getAmount(), expiresAt, topUpBy);

        studentRepository.findById(request.getStudentId()).ifPresent(s ->
            studentActivityLogService.log(
                    s.getId(), s.getFullName(),
                    com.educore.studentactivity.StudentEventType.WALLET_TOPPED_UP,
                    "شحن المحفظة",
                    "المبلغ: " + request.getAmount() + " ج.م — بواسطة: " + topUpBy
            )
        );

        return getWallet(request.getStudentId());
    }

    // ─── شحن اونلاين (الطالب عبر فواتيرك) ─────────────────────

    /**
     * ينشئ معاملة إيداع PENDING ويرجع رقمها ليُخزَّن كـ reference في الـ invoice.
     * الـ FawaterekCallbackController بيكمّل المعاملة لما يستلم الـ callback.
     */
    @Transactional
    public Map<String, String> initiateOnlineDeposit(Long studentId, BigDecimal amount) {
        Wallet wallet = walletRepository.findByStudentId(studentId)
                .orElseGet(() -> getOrCreateWallet(studentId));

        // إلغاء أي معاملات PENDING سابقة لنفس الطالب لتجنب التراكم
        List<WalletTransaction> pendingOld = txRepository.findByWalletStudentIdAndStatusAndType(
                studentId, TransactionStatus.PENDING, TransactionType.DEPOSIT);
        for (WalletTransaction old : pendingOld) {
            old.fail("تم الإلغاء — بدأ الطالب عملية دفع جديدة");
            txRepository.save(old);
            log.info("Cancelled stale PENDING deposit: txNumber={}", old.getTransactionNumber());
        }

        String txNumber = generateTxNumber();

        WalletTransaction tx = WalletTransaction.builder()
                .wallet(wallet)
                .transactionNumber(txNumber)
                .type(TransactionType.DEPOSIT)
                .status(TransactionStatus.PENDING)
                .amount(amount)
                .balanceAfter(wallet.getBalance()) // سيُحدَّث عند الإتمام
                .description("شحن اونلاين عبر فواتيرك")
                .build();
        txRepository.save(tx);

        log.info("Online deposit initiated: student={}, amount={}, txNumber={}", studentId, amount, txNumber);
        return Map.of("transactionNumber", txNumber);
    }

    /**
     * يُكمّل معاملة إيداع اونلاين بعد تأكيد فواتيرك.
     * يُستدعى من FawaterekCallbackController.
     */
    @Transactional
    public void completeOnlineDeposit(String txNumber, String invoiceId) {
        WalletTransaction tx = txRepository.findByTransactionNumber(txNumber)
                .orElseThrow(() -> new ResourceNotFoundException("معاملة غير موجودة: " + txNumber));

        if (tx.getStatus() == TransactionStatus.COMPLETED) {
            log.warn("Deposit already completed: txNumber={}", txNumber);
            return;
        }

        Wallet wallet = walletRepository.findByStudentIdWithLock(tx.getWallet().getStudent().getId())
                .orElseThrow(() -> new ResourceNotFoundException("المحفظة غير موجودة"));

        wallet.credit(tx.getAmount());
        walletRepository.save(wallet);

        tx.setBalanceAfter(wallet.getBalance());
        tx.setReferenceId(invoiceId);
        tx.complete();
        txRepository.save(tx);

        log.info("Online deposit completed: txNumber={}, invoiceId={}, amount={}", txNumber, invoiceId, tx.getAmount());

        studentActivityLogService.log(
                wallet.getStudent().getId(), wallet.getStudent().getFullName(),
                com.educore.studentactivity.StudentEventType.WALLET_TOPPED_UP,
                "شحن المحفظة أونلاين",
                "المبلغ: " + tx.getAmount() + " ج.م — فاتورة: " + invoiceId
        );
    }

    /**
     * يُفشّل معاملة إيداع اونلاين لو الدفع فشل.
     */
    @Transactional
    public void failOnlineDeposit(String txNumber, String reason) {
        txRepository.findByTransactionNumber(txNumber).ifPresent(tx -> {
            if (tx.getStatus() == TransactionStatus.PENDING) {
                tx.fail(reason);
                txRepository.save(tx);
                log.warn("Online deposit failed: txNumber={}, reason={}", txNumber, reason);
            }
        });
    }

    /** يرجع حالة معاملة إيداع اونلاين — للـ polling من الـ frontend */
    @Transactional(readOnly = true)
    public String getDepositStatus(String txNumber, Long studentId) {
        return txRepository.findByTransactionNumber(txNumber)
                .filter(tx -> tx.getWallet().getStudent().getId().equals(studentId))
                .map(tx -> tx.getStatus().name())
                .orElse("NOT_FOUND");
    }

    // ─── خصم من المحفظة (الدفع) ─────────────────────────────────

    @Transactional
    public String debitForPurchase(Long studentId, BigDecimal amount,
                                   String description, String referenceId) {
        Wallet wallet = walletRepository.findByStudentIdWithLock(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("المحفظة غير موجودة"));

        wallet.debit(amount);
        walletRepository.save(wallet);

        String txNumber = generateTxNumber();
        WalletTransaction tx = WalletTransaction.builder()
                .wallet(wallet)
                .transactionNumber(txNumber)
                .type(TransactionType.PURCHASE)
                .status(TransactionStatus.COMPLETED)
                .amount(amount)
                .balanceAfter(wallet.getBalance())
                .description(description)
                .referenceId(referenceId)
                .completedAt(LocalDateTime.now())
                .build();
        txRepository.save(tx);

        log.info("Wallet debit: student={}, amount={}, ref={}", studentId, amount, referenceId);
        return txNumber;
    }

    // ─── استرداد (Refund) ────────────────────────────────────────

    @Transactional
    public void refund(Long studentId, BigDecimal amount, String referenceId, String reason) {
        Wallet wallet = walletRepository.findByStudentIdWithLock(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("المحفظة غير موجودة"));

        wallet.refund(amount);
        walletRepository.save(wallet);

        WalletTransaction tx = WalletTransaction.builder()
                .wallet(wallet)
                .transactionNumber(generateTxNumber())
                .type(TransactionType.REFUND)
                .status(TransactionStatus.COMPLETED)
                .amount(amount)
                .balanceAfter(wallet.getBalance())
                .description(reason)
                .referenceId(referenceId)
                .completedAt(LocalDateTime.now())
                .build();
        txRepository.save(tx);
        log.info("Wallet refund: student={}, amount={}", studentId, amount);
    }

    // ─── فحص الرصيد ─────────────────────────────────────────────

    @Transactional(readOnly = true)
    public boolean hasSufficientBalance(Long studentId, BigDecimal required) {
        BigDecimal effective = txRepository.computeEffectiveBalance(studentId);
        return effective.compareTo(required) >= 0;
    }

    @Transactional(readOnly = true)
    public BigDecimal getEffectiveBalance(Long studentId) {
        return txRepository.computeEffectiveBalance(studentId);
    }

    // ─── سجل المعاملات ───────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<WalletTransaction> getTransactions(Long studentId, Pageable pageable) {
        return txRepository.findByWalletStudentIdAndStatusNotOrderByCreatedAtDesc(
                studentId, TransactionStatus.FAILED, pageable);
    }

    // ─── كل عمليات الشحن للمدرس/الأدمن ──────────────────────────

    @Transactional(readOnly = true)
    public Page<WalletTransaction> getAllDepositTransactions(Pageable pageable) {
        return txRepository.findByTypeOrderByCreatedAtDesc(
                TransactionType.DEPOSIT, pageable);
    }

    // ─── إحصائيات المحفظة ────────────────────────────────────────

    @Transactional(readOnly = true)
    public com.educore.wallet.dto.WalletStatsResponse getStats() {
        LocalDateTime startOfMonth = LocalDateTime.now()
                .withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime now = LocalDateTime.now();

        BigDecimal totalCharged = txRepository.sumDepositsInPeriod(startOfMonth, now);
        long count = txRepository.countDepositsInPeriod(startOfMonth, now);
        long studentsWithBalance = walletRepository.countByBalanceGreaterThan(BigDecimal.ZERO);

        return com.educore.wallet.dto.WalletStatsResponse.builder()
                .totalChargedThisMonth(totalCharged)
                .topUpCountThisMonth(count)
                .studentsWithBalance(studentsWithBalance)
                .build();
    }

    // ─── Helpers ─────────────────────────────────────────────────

    private LocalDateTime resolveExpiry(WalletTopUpRequest req) {
        if (req.getExpiresAt() != null) return req.getExpiresAt();
        if (req.getValidDays() != null && req.getValidDays() > 0)
            return LocalDateTime.now().plusDays(req.getValidDays());
        return null;
    }

    public String generateTxNumber() {
        return "WTX-" + System.currentTimeMillis() + "-"
                + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }

    private WalletResponse toResponse(Wallet wallet, BigDecimal effective,
                                       List<WalletTransaction> recent) {
        List<WalletResponse.WalletTransactionResponse> txList = recent.stream()
                .map(t -> WalletResponse.WalletTransactionResponse.builder()
                        .id(t.getId())
                        .transactionNumber(t.getTransactionNumber())
                        .type(t.getType() != null ? t.getType().name() : null)
                        .status(t.getStatus() != null ? t.getStatus().name() : null)
                        .amount(t.getAmount())
                        .balanceAfter(t.getBalanceAfter())
                        .description(t.getDescription())
                        .createdAt(t.getCreatedAt())
                        .expiresAt(t.getExpiresAt())
                        .expired(t.isExpired())
                        .build())
                .toList();

        return WalletResponse.builder()
                .walletId(wallet.getId())
                .studentId(wallet.getStudent().getId())
                .studentName(wallet.getStudent().getFullName())
                .balance(wallet.getBalance())
                .effectiveBalance(effective)
                .totalDeposited(wallet.getTotalDeposited())
                .totalSpent(wallet.getTotalSpent())
                .recentTransactions(txList)
                .build();
    }
}