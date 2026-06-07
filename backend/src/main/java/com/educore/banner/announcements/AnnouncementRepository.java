package com.educore.banner.announcements;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AnnouncementRepository extends JpaRepository<Announcement, Long> {

    // ✅ للطالب - مع Pagination
    @Query("SELECT a FROM Announcement a WHERE a.active = true " +
            "AND (a.announcementDate IS NULL OR a.announcementDate <= :now) " +
            "AND (a.expiryDate IS NULL OR a.expiryDate >= :now) " +
            "ORDER BY a.announcementDate DESC, a.createdAt DESC")
    Page<Announcement> findActiveAnnouncements(@Param("now") LocalDateTime now, Pageable pageable);

    // ✅ للمدرس - مع Pagination
    Page<Announcement> findAllByOrderByCreatedAtDesc(Pageable pageable);

    // ✅ أحدث إعلان (اختياري)
    Optional<Announcement> findTopByOrderByCreatedAtDesc();

    // ✅ حذف الإعلانات المنتهية
    @Modifying
    @Query("DELETE FROM Announcement a WHERE a.expiryDate IS NOT NULL AND a.expiryDate < :now")
    void deleteExpiredAnnouncements(@Param("now") LocalDateTime now);
}