package com.educore.payment.payment;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByOrderId(Long orderId);

    Optional<Payment> findByOrderOrderNumber(String orderNumber);

    Optional<Payment> findByTransactionId(String transactionId);

    Page<Payment> findByPaymentMethodAndStatusOrderByCreatedAtDesc(
            PaymentMethod method,PaymentStatus status, Pageable pageable);

    /** طلبات PENDING بانتظار موافقة الأدمن */
    @Query("""
        SELECT p FROM Payment p
        WHERE p.paymentMethod IN ('CASH', 'BANK_TRANSFER')
          AND p.status = 'PENDING'
        ORDER BY p.createdAt ASC
    """)
    List<Payment> findPendingApprovals();

    /** Admin Dashboard Stats */
    @Query("""
        SELECT p.paymentMethod, COUNT(p), SUM(p.amount)
        FROM Payment p
        WHERE p.status = 'COMPLETED'
          AND p.createdAt BETWEEN :from AND :to
        GROUP BY p.paymentMethod
    """)
    List<Object[]> getRevenueStatsByMethod(
            @Param("from") LocalDateTime from,
            @Param("to")   LocalDateTime to);

    /** إجمالي الإيرادات (كل وقت) */
    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.status = 'COMPLETED'")
    java.math.BigDecimal getTotalRevenue();

    /** إجمالي الإيرادات في فترة */
    @Query("""
        SELECT COALESCE(SUM(p.amount), 0) FROM Payment p
        WHERE p.status = 'COMPLETED'
          AND p.createdAt BETWEEN :from AND :to
    """)
    java.math.BigDecimal getRevenueInPeriod(
            @Param("from") LocalDateTime from,
            @Param("to")   LocalDateTime to);

    /** إجمالي عدد المدفوعات المكتملة */
    long countByStatus(PaymentStatus status);

    /** Purchase heatmap: [dayOfWeek 0-6, hour 0-23, count] */
    @Query(value = """
        SELECT EXTRACT(DOW FROM created_at)::int  AS dow,
               EXTRACT(HOUR FROM created_at)::int AS hr,
               COUNT(*) AS cnt
        FROM payments
        WHERE status = 'COMPLETED'
        GROUP BY dow, hr
        ORDER BY dow, hr
    """, nativeQuery = true)
    List<Object[]> getPurchaseHeatmap();
}