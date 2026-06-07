package com.educore.copon;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AccessCodeUsageRepository extends JpaRepository<AccessCodeUsage, Long> {
    // ✅ حذف استخدامات أكواد مرتبطة بكورس
    @Modifying
    @Query("DELETE FROM AccessCodeUsage acu WHERE acu.accessCode.course.id = :courseId")
    void deleteByCourseId(@Param("courseId") Long courseId);
    boolean existsByAccessCodeIdAndStudentId(Long codeId, Long studentId);

    List<AccessCodeUsage> findByAccessCodeIdOrderByUsedAtDesc(Long codeId);

    List<AccessCodeUsage> findByStudentIdOrderByUsedAtDesc(Long studentId);
    @Modifying
    @Query("DELETE FROM AccessCodeUsage acu WHERE acu.accessCode.id = :accessCodeId")
    void deleteByAccessCodeId(@Param("accessCodeId") Long accessCodeId);

    // ✅ حذف استخدامات الكود المرتبطة بقائمة أكواد
    @Modifying
    @Query("DELETE FROM AccessCodeUsage acu WHERE acu.accessCode.id IN :accessCodeIds")
    void deleteByAccessCodeIds(@Param("accessCodeIds") List<Long> accessCodeIds);


    // ✅ حذف استخدامات أكواد مرتبطة بفئة
    @Modifying
    @Query("DELETE FROM AccessCodeUsage acu WHERE acu.accessCode.category.id = :categoryId")
    void deleteByCategoryId(@Param("categoryId") Long categoryId);


}
