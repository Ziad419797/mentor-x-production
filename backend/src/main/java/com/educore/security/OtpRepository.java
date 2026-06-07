package com.educore.security;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface OtpRepository extends JpaRepository<OtpRecord, Long> {

    /**
     * آخر OTP غير مستخدم لهذا الرقم
     * (نرتّبه بـ createdAt تنازلياً عشان ناخد الأحدث)
     */
    Optional<OtpRecord> findTopByPhoneAndUsedFalseOrderByCreatedAtDesc(String phone);

    /**
     * عدد المحاولات المرسلة لنفس الرقم في آخر N دقيقة
     * (للحد من الـ spam)
     */
    @Query("""
        SELECT COUNT(r) FROM OtpRecord r
        WHERE r.phone = :phone
          AND r.createdAt >= :since
    """)
    long countRecentByPhone(@Param("phone") String phone,
                            @Param("since") LocalDateTime since);

    /**
     * حذف كل الـ OTPs المنتهية الصلاحية — بيتشغل كـ scheduled cleanup
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM OtpRecord r WHERE r.expiresAt < :now")
    void deleteExpired(@Param("now") LocalDateTime now);

    /**
     * حذف كل الـ OTPs الخاصة برقم معين (بعد الاستخدام الناجح)
     */
    @Modifying
    @Transactional
    void deleteByPhone(String phone);
}
