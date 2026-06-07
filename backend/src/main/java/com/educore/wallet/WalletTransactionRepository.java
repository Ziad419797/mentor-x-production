package com.educore.wallet;

import com.educore.payment.payment.TransactionType;
import com.educore.payment.payment.TransactionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface WalletTransactionRepository extends JpaRepository<WalletTransaction, Long> {

    Page<WalletTransaction> findByWalletStudentIdOrderByCreatedAtDesc(
            Long studentId, Pageable pageable);

    Page<WalletTransaction> findByWalletStudentIdAndStatusNotOrderByCreatedAtDesc(
            Long studentId, TransactionStatus status, Pageable pageable);

    List<WalletTransaction> findByWalletStudentIdAndTypeOrderByCreatedAtDesc(
            Long studentId, TransactionType type);

    Optional<WalletTransaction> findByTransactionNumber(String transactionNumber);

    List<WalletTransaction> findByWalletStudentIdAndStatusAndType(
            Long studentId, TransactionStatus status, TransactionType type);

    Page<WalletTransaction> findByTypeOrderByCreatedAtDesc(TransactionType type, Pageable pageable);

    @Query("SELECT COUNT(t) FROM WalletTransaction t WHERE t.type = 'DEPOSIT' AND t.status = 'COMPLETED' AND t.createdAt >= :from AND t.createdAt <= :to")
    long countDepositsInPeriod(@Param("from") java.time.LocalDateTime from, @Param("to") java.time.LocalDateTime to);

    @Query("""
        SELECT SUM(t.amount) FROM WalletTransaction t
        WHERE t.wallet.student.id = :studentId
          AND t.type = :type
          AND t.status = 'COMPLETED'
    """)
    Optional<BigDecimal> sumByStudentAndType(
            @Param("studentId") Long studentId,
            @Param("type") TransactionType type);

    @Query("""
        SELECT COALESCE(SUM(
            CASE
              WHEN t.type = 'DEPOSIT'
                AND t.status = 'COMPLETED'
                AND (t.expiresAt IS NULL OR t.expiresAt > CURRENT_TIMESTAMP)
              THEN t.amount
              WHEN t.type IN ('PURCHASE','WITHDRAWAL')
                AND t.status = 'COMPLETED'
              THEN -t.amount
              WHEN t.type = 'REFUND'
                AND t.status = 'COMPLETED'
              THEN t.amount
              ELSE 0
            END
        ), 0)
        FROM WalletTransaction t
        WHERE t.wallet.student.id = :studentId
    """)
    BigDecimal computeEffectiveBalance(@Param("studentId") Long studentId);

    @Query("""
        SELECT t FROM WalletTransaction t
        WHERE t.type = 'DEPOSIT'
          AND t.status = 'COMPLETED'
          AND t.expiresAt IS NOT NULL
          AND t.expiresAt <= CURRENT_TIMESTAMP
        ORDER BY t.expiresAt ASC
    """)
    List<WalletTransaction> findExpiredDeposits();

    @Query("""
        SELECT COALESCE(SUM(t.amount), 0) FROM WalletTransaction t
        WHERE t.type = 'DEPOSIT'
          AND t.status = 'COMPLETED'
          AND t.createdAt >= :from AND t.createdAt <= :to
    """)
    BigDecimal sumDepositsInPeriod(
        @Param("from") java.time.LocalDateTime from,
        @Param("to")   java.time.LocalDateTime to);

    @Query(value = """
        SELECT t FROM WalletTransaction t
        JOIN FETCH t.wallet w
        JOIN FETCH w.student s
        WHERE t.type = :type
        ORDER BY t.createdAt DESC
    """, countQuery = "SELECT COUNT(t) FROM WalletTransaction t WHERE t.type = :type")
    Page<WalletTransaction> findByTypeWithStudent(
            @Param("type") TransactionType type, Pageable pageable);
}
