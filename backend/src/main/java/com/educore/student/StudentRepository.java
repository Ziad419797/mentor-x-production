package com.educore.student;

import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

public interface StudentRepository extends JpaRepository<Student, Long> {
    Optional<Student> findByParentPhone(String parentPhone);

    Optional<Student> findByPhone(String phone);
//    Optional<Student> findByUserId(Long userId);
// ✅ ده الصح
Optional<Student> findById(Long id);
    Optional<Student> findByStudentCode(String studentCode);
    boolean existsByStudentCode(String studentCode);
    // جلب الطلاب حسب الحالة مع دعم الصفحة
    Page<Student> findByStatus(StudentStatus status, Pageable pageable);

    // للبحث عن طالب معين برقم تليفونه أو كوده في قائمة الانتظار
    Optional<Student> findByPhoneOrStudentCode(String phone, String studentCode);

    // ── Analytics / Map ───────────────────────────────────────

    /** كل الطلاب النشطين ببياناتهم الجغرافية — للخريطة */
    @Query("""
        SELECT s FROM Student s
        WHERE s.status = com.educore.student.StudentStatus.ACTIVE
        ORDER BY s.governorate ASC, s.area ASC
    """)
    List<Student> findAllActiveForMap();

    /** توزيع الطلاب بالمحافظات — للإحصائيات */
    @Query("""
        SELECT s.governorate, COUNT(s)
        FROM Student s
        WHERE s.status = com.educore.student.StudentStatus.ACTIVE
        GROUP BY s.governorate
        ORDER BY COUNT(s) DESC
    """)
    List<Object[]> countActiveByGovernorate();

    /** عدد الطلاب الجدد في فترة زمنية */
    @Query("""
        SELECT COUNT(s) FROM Student s
        WHERE s.createdAt >= :from AND s.createdAt <= :to
    """)
    long countNewStudents(
        @Param("from") java.time.LocalDateTime from,
        @Param("to")   java.time.LocalDateTime to);

    /** إجمالي عدد الطلاب النشطين */
    long countByStatus(StudentStatus status);

    /**
     * تحديث جلسات الطلاب المنتهية بـ bulk query واحدة — بديل عن findAll().forEach
     * يُستخدم في SessionCleanupConfig لتجنب OOM على آلاف الطلاب
     */
    @Modifying
    @Query("""
        UPDATE Student s
        SET s.activeDeviceId  = null,
            s.activeSessionId = null,
            s.lastActivityAt  = null
        WHERE s.activeDeviceId IS NOT NULL
          AND s.lastActivityAt < :cutoff
    """)
    int clearExpiredSessions(@Param("cutoff") LocalDateTime cutoff);

    // جلب الطلاب حسب الحالة والصف الدراسي
    Page<Student> findByStatusAndGrade(StudentStatus status, String grade, Pageable pageable);

    /** طلاب أونلاين نشطين اختاروا سنتر مستقبلي */
    @Query("SELECT s FROM Student s WHERE s.online = true AND s.status = com.educore.student.StudentStatus.ACTIVE AND s.centerName IS NOT NULL AND s.centerName <> ''")
    Page<Student> findFutureCenterStudents(Pageable pageable);

    /** الطلاب النشطين اللي عيد ميلادهم النهارده (نفس اليوم والشهر) */
    @Query("""
        SELECT s FROM Student s
        WHERE s.status = com.educore.student.StudentStatus.ACTIVE
          AND s.dateOfBirth IS NOT NULL
          AND FUNCTION('MONTH', s.dateOfBirth) = FUNCTION('MONTH', :today)
          AND FUNCTION('DAY',   s.dateOfBirth) = FUNCTION('DAY',   :today)
    """)
    List<Student> findTodayBirthdays(@Param("today") LocalDate today);

}