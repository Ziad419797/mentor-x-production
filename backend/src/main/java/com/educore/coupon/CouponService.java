package com.educore.coupon;

import com.educore.coupon.dto.*;
import com.educore.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CouponService {

    private final CouponRepository            couponRepository;
    private final CouponRedemptionRepository  redemptionRepository;

    // ─── إنشاء كوبون ─────────────────────────────────────────────

    @Transactional
    public CouponResponse create(CouponRequest request, String createdBy) {
        if (couponRepository.existsByCodeIgnoreCase(request.getCode())) {
            throw new IllegalArgumentException("كود الكوبون موجود بالفعل: " + request.getCode());
        }

        // التحقق من صحة النسبة المئوية
        if (request.getType() == CouponType.PERCENTAGE) {
            if (request.getValue().compareTo(BigDecimal.valueOf(100)) > 0) {
                throw new IllegalArgumentException("نسبة الخصم لا تتجاوز 100%");
            }
        }

        Coupon coupon = Coupon.builder()
                .code(request.getCode().toUpperCase())
                .description(request.getDescription())
                .type(request.getType())
                .value(request.getValue())
                .maxDiscount(request.getMaxDiscount())
                .minAmount(request.getMinAmount())
                .maxUses(request.getMaxUses())
                .expiresAt(request.getExpiresAt())
                .createdBy(createdBy)
                .active(true)
                .build();

        couponRepository.save(coupon);
        log.info("Coupon created: code={}, type={}, value={}, by={}",
                coupon.getCode(), coupon.getType(), coupon.getValue(), createdBy);
        return toResponse(coupon);
    }

    // ─── تعديل كوبون ─────────────────────────────────────────────

    @Transactional
    public CouponResponse update(Long id, CouponRequest request) {
        Coupon coupon = findOrThrow(id);
        coupon.setDescription(request.getDescription());
        coupon.setType(request.getType());
        coupon.setValue(request.getValue());
        coupon.setMaxDiscount(request.getMaxDiscount());
        coupon.setMinAmount(request.getMinAmount());
        coupon.setMaxUses(request.getMaxUses());
        coupon.setExpiresAt(request.getExpiresAt());
        couponRepository.save(coupon);
        return toResponse(coupon);
    }

    // ─── تفعيل / تعطيل ───────────────────────────────────────────

    @Transactional
    public CouponResponse toggleActive(Long id) {
        Coupon coupon = findOrThrow(id);
        coupon.setActive(!coupon.isActive());
        couponRepository.save(coupon);
        log.info("Coupon {} toggled active={}", coupon.getCode(), coupon.isActive());
        return toResponse(coupon);
    }

    // ─── جلب الكوبونات ───────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<CouponResponse> getAll(Pageable pageable) {
        return couponRepository.findAllByOrderByCreatedAtDesc(pageable)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public CouponResponse getById(Long id) {
        return toResponse(findOrThrow(id));
    }

    @Transactional(readOnly = true)
    public List<CouponResponse> getValidCoupons() {
        return couponRepository.findValidCoupons().stream()
                .map(this::toResponse)
                .toList();
    }

    // ─── تطبيق كوبون (Preview — قبل تأكيد الدفع) ───────────────

    /**
     * الطالب يكتب الكود فالسيستم يحسبله الخصم ويرجعله المبلغ النهائي.
     * لا يُسجل استخداماً هنا — الاستخدام بيتسجل فقط عند تأكيد الدفع.
     */
    @Transactional(readOnly = true)
    public ApplyCouponResponse preview(ApplyCouponRequest request) {
        Coupon coupon = couponRepository.findByCodeIgnoreCase(request.getCode())
                .orElseThrow(() -> new IllegalArgumentException("كود الكوبون غير صحيح"));

        validateCoupon(coupon, request.getOriginalPrice());

        BigDecimal discount = coupon.calculateDiscount(request.getOriginalPrice())
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal finalPrice = request.getOriginalPrice().subtract(discount)
                .max(BigDecimal.ZERO);

        String summary = buildDiscountSummary(coupon, discount);

        return ApplyCouponResponse.builder()
                .couponCode(coupon.getCode())
                .couponDescription(coupon.getDescription())
                .originalPrice(request.getOriginalPrice())
                .discountAmount(discount)
                .finalPrice(finalPrice)
                .discountSummary(summary)
                .build();
    }

    // ─── تسجيل استخدام الكوبون (عند تأكيد الدفع) ───────────────

    /**
     * يُستدعى من PaymentService بعد تأكيد الطلب.
     * يسجل الاستخدام ويزيد العداد.
     *
     * @return مبلغ الخصم الفعلي
     */
    @Transactional
    public BigDecimal redeemCoupon(String code, Long studentId,
                                   Long orderId, BigDecimal originalPrice) {
        Coupon coupon = couponRepository.findByCodeIgnoreCaseWithLock(code)
                .orElseThrow(() -> new IllegalArgumentException("كوبون غير صحيح"));

        validateCoupon(coupon, originalPrice);

        BigDecimal discount = coupon.calculateDiscount(originalPrice)
                .setScale(2, RoundingMode.HALF_UP);

        // تسجيل الاستخدام
        CouponRedemption redemption = CouponRedemption.builder()
                .coupon(coupon)
                .studentId(studentId)
                .orderId(orderId)
                .discountAmount(discount)
                .originalAmount(originalPrice)
                .build();
        redemptionRepository.save(redemption);

        // زيادة العداد
        coupon.incrementUsage();
        couponRepository.save(coupon);

        log.info("Coupon redeemed: code={}, student={}, discount={}, order={}",
                code, studentId, discount, orderId);
        return discount;
    }

    // ─── Helpers ─────────────────────────────────────────────────

    private void validateCoupon(Coupon coupon, BigDecimal price) {
        if (!coupon.isValid()) {
            throw new IllegalArgumentException("الكوبون غير صالح أو منتهي الصلاحية");
        }
        if (coupon.getMinAmount() != null && price.compareTo(coupon.getMinAmount()) < 0) {
            throw new IllegalArgumentException(
                    "الكوبون يتطلب حد أدنى للسعر: " + coupon.getMinAmount() + " جنيه");
        }
    }

    private String buildDiscountSummary(Coupon coupon, BigDecimal discount) {
        if (coupon.getType() == CouponType.PERCENTAGE) {
            return "خصم " + coupon.getValue().stripTrailingZeros().toPlainString()
                    + "% = " + discount + " جنيه";
        }
        return "خصم " + discount + " جنيه";
    }

    private Coupon findOrThrow(Long id) {
        return couponRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("الكوبون غير موجود: " + id));
    }

    CouponResponse toResponse(Coupon c) {
        return CouponResponse.builder()
                .id(c.getId())
                .code(c.getCode())
                .description(c.getDescription())
                .type(c.getType())
                .value(c.getValue())
                .maxDiscount(c.getMaxDiscount())
                .minAmount(c.getMinAmount())
                .maxUses(c.getMaxUses())
                .usedCount(c.getUsedCount())
                .active(c.isActive())
                .valid(c.isValid())
                .expiresAt(c.getExpiresAt())
                .createdBy(c.getCreatedBy())
                .createdAt(c.getCreatedAt())
                .build();
    }
}
