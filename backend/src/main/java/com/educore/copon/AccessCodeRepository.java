package com.educore.copon;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AccessCodeRepository extends JpaRepository<AccessCode, Long> {

    Optional<AccessCode> findByCode(String code);

    boolean existsByCode(String code);

    /** أكواد مدرس معين */
    Page<AccessCode> findByCreatedByIdOrderByCreatedAtDesc(Long teacherId, Pageable pageable);

    /** أكواد دفعة بعينها */
    List<AccessCode> findByCreatedByIdAndBatchLabelOrderByCreatedAtAsc(
            Long teacherId, String batchLabel);

    /** أكواد باقة معينة */
    List<AccessCode> findByCategoryIdAndActiveTrue(Long categoryId);

    /** تعطيل الأكواد المنتهية */
    @Modifying
    @Query("UPDATE AccessCode a SET a.active = false WHERE a.expiresAt < :now AND a.active = true")
    int deactivateExpired(@Param("now") LocalDateTime now);

    /** إحصائيات المدرس */
    @Query("SELECT COUNT(a) FROM AccessCode a WHERE a.createdById = :teacherId AND a.active = true")
    long countActiveByTeacher(@Param("teacherId") Long teacherId);

    @Query("SELECT SUM(a.usedCount) FROM AccessCode a WHERE a.createdById = :teacherId")
    Long sumUsageByTeacher(@Param("teacherId") Long teacherId);
    // ✅ حذف أكواد الوصول المرتبطة بفئة
    @Modifying
    @Query("DELETE FROM AccessCode ac WHERE ac.category.id = :categoryId")
    void deleteByCategoryId(@Param("categoryId") Long categoryId);
    // ✅ هذه الميثودات موجودة أو أضفها
    List<AccessCode> findByCourseId(Long courseId);
    List<AccessCode> findByCategoryId(Long categoryId);

    // ✅ حذف أكواد الوصول المرتبطة بكورس
    @Modifying
    @Query("DELETE FROM AccessCode ac WHERE ac.course.id = :courseId")
    void deleteByCourseId(@Param("courseId") Long courseId);
}


