package com.educore.payment.order;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    Optional<Order> findByOrderNumber(String orderNumber);

    Page<Order> findByStudentIdOrderByCreatedAtDesc(Long studentId, Pageable pageable);

    Page<Order> findByStudentIdAndStatusOrderByCreatedAtDesc(
            Long studentId,OrderStatus status, Pageable pageable);

    /** طلبات PENDING أقدم من X دقائق (للـ cleanup job) */
    List<Order> findByStatusAndCreatedAtBefore(OrderStatus status, LocalDateTime before);

    @Query("""
        SELECT COALESCE(SUM(o.total), 0)
        FROM Order o
        WHERE o.student.id = :studentId
          AND o.status = 'PAID'
    """)
    BigDecimal getTotalSpentByStudent(@Param("studentId") Long studentId);

    boolean existsByStudentIdAndStatusIn(Long studentId, List<OrderStatus> statuses);
    // ✅ حذف عناصر الطلب المرتبطة بفئة
    @Modifying
    @Query("DELETE FROM OrderItem oi WHERE oi.category.id = :categoryId")
    void deleteByCategoryId(@Param("categoryId") Long categoryId);

    // ✅ حذف عناصر الطلب المرتبطة بكورس
    @Modifying
    @Query("DELETE FROM OrderItem oi WHERE oi.course.id = :courseId")
    void deleteByCourseId(@Param("courseId") Long courseId);

    // ✅ حذف عناصر الطلب المرتبطة بقائمة فئات
    @Modifying
    @Query("DELETE FROM OrderItem oi WHERE oi.category.id IN :categoryIds")
    void deleteByCategoryIds(@Param("categoryIds") java.util.List<Long> categoryIds);
}