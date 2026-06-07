package com.educore.banner;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface BannerRepository extends JpaRepository<Banner, Long> {

    // للطالب: البانرات النشطة حالياً
    // أضف هذه الميثود
    @Query("SELECT b FROM Banner b WHERE b.active = true " +
            "AND (b.startDate IS NULL OR b.startDate <= :now) " +
            "AND (b.endDate IS NULL OR b.endDate >= :now) " +
            "ORDER BY b.displayOrder ASC, b.createdAt DESC")
    Page<Banner> findActiveBanners(@Param("now") LocalDateTime now, Pageable pageable);
    // للمدرس: كل البانرات مع الترتيب
    Page<Banner> findAllByOrderByDisplayOrderAscCreatedAtDesc(Pageable pageable);

    // حذف بانرات منتهية (للمهمة المجدولة)
    @Modifying
    @Query("DELETE FROM Banner b WHERE b.endDate IS NOT NULL AND b.endDate < :now")
    void deleteExpiredBanners(@Param("now") LocalDateTime now);
}