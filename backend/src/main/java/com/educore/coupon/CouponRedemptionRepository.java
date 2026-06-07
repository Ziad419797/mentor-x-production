package com.educore.coupon;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CouponRedemptionRepository extends JpaRepository<CouponRedemption, Long> {

    List<CouponRedemption> findByStudentIdOrderByRedeemedAtDesc(Long studentId);

    Optional<CouponRedemption> findByCouponIdAndStudentId(Long couponId, Long studentId);

    boolean existsByCouponIdAndStudentId(Long couponId, Long studentId);

    long countByCouponId(Long couponId);
}
