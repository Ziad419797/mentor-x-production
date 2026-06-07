package com.educore.coupon;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CouponRepository extends JpaRepository<Coupon, Long> {

    Optional<Coupon> findByCodeIgnoreCase(String code);

    /** للاستخدام عند الـ redemption — Pessimistic Lock لمنع Race Condition */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Coupon c WHERE UPPER(c.code) = UPPER(:code)")
    Optional<Coupon> findByCodeIgnoreCaseWithLock(@Param("code") String code);

    boolean existsByCodeIgnoreCase(String code);

    Page<Coupon> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Page<Coupon> findByActiveOrderByCreatedAtDesc(boolean active, Pageable pageable);

    /** الكوبونات النشطة وغير المنتهية */
    @Query("""
        SELECT c FROM Coupon c
        WHERE c.active = true
          AND (c.expiresAt IS NULL OR c.expiresAt > CURRENT_TIMESTAMP)
          AND (c.maxUses IS NULL OR c.usedCount < c.maxUses)
        ORDER BY c.createdAt DESC
    """)
    List<Coupon> findValidCoupons();

    /** عدد الكوبونات المستخدمة في فترة معينة — للـ analytics */
    @Query("""
        SELECT COUNT(r) FROM CouponRedemption r
        WHERE r.redeemedAt >= :from AND r.redeemedAt <= :to
    """)
    long countRedemptionsInPeriod(
        @Param("from") java.time.LocalDateTime from,
        @Param("to")   java.time.LocalDateTime to);
}
