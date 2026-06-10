package com.educore.enrollment;

import com.educore.student.Student;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {
    // أضف هذه الطريقة في EnrollmentRepository.java

    /**
     * جلب IDs الكورسات التي الطالب مشترك فيها
     */
    @Query("SELECT DISTINCT e.course.id FROM Enrollment e " +
            "WHERE e.student.id = :studentId " +
            "AND e.active = true " +
            "AND e.status = 'ACTIVE'")
    List<Long> findCourseIdsByStudentId(@Param("studentId") Long studentId);
    @Query("SELECT COUNT(e) > 0 FROM Enrollment e " +
            "WHERE e.student.id = :studentId " +
            "AND e.course.id = :courseId " +
            "AND e.status = 'ACTIVE' " +
            "AND e.active = true")
    boolean hasActiveEnrollment(@Param("studentId") Long studentId,
                                @Param("courseId") Long courseId);

    /**
     * هل الطالب مشترك في أي كورس يحتوي على هذه الحصة (عبر Session)؟
     * يُستخدم في LessonGate للتحقق من الشراء قبل السماح بالمشاهدة.
     */
    @Query("""
        SELECT CASE WHEN COUNT(e) > 0 THEN true ELSE false END
        FROM Enrollment e
        JOIN e.course c
        JOIN c.sessions s
        JOIN s.weeks w
        WHERE e.student.id = :studentId
          AND w.id = :weekId
          AND e.status = 'ACTIVE'
          AND e.active = true
    """)
    boolean isEnrolledInWeek(@Param("studentId") Long studentId,
                             @Param("weekId") Long weekId);

    // ==================== Basic Queries ====================
    @EntityGraph(attributePaths = {"course", "student"})
    Optional<Enrollment> findByStudentIdAndCourseIdAndActiveTrue(Long studentId, Long courseId);

    boolean existsByStudentIdAndCourseIdAndActiveTrue(Long studentId, Long courseId);

    @org.springframework.data.jpa.repository.Query(value = "SELECT * FROM enrollments WHERE student_id = :studentId AND course_id = :courseId AND active = false LIMIT 1", nativeQuery = true)
    Optional<Enrollment> findInactiveByStudentIdAndCourseId(@org.springframework.data.repository.query.Param("studentId") Long studentId, @org.springframework.data.repository.query.Param("courseId") Long courseId);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query(value = "UPDATE enrollments SET active = true, status = 'ACTIVE', enrollment_type = 'ADMIN_GRANT', enrolled_at = NOW(), expires_at = NOW() + INTERVAL '1 year', deleted_at = NULL, deleted_by = NULL, created_by = :adminUsername WHERE student_id = :studentId AND course_id = :courseId AND active = false", nativeQuery = true)
    int reactivateEnrollment(@org.springframework.data.repository.query.Param("studentId") Long studentId, @org.springframework.data.repository.query.Param("courseId") Long courseId, @org.springframework.data.repository.query.Param("adminUsername") String adminUsername);
    @EntityGraph(attributePaths = {"course", "student"})
    List<Enrollment> findByStudentIdAndActiveTrue(Long studentId);
    @EntityGraph(attributePaths = {"course", "student"})
    Page<Enrollment> findByStudentIdAndActiveTrue(Long studentId, Pageable pageable);
    @EntityGraph(attributePaths = {"course", "student"})
    List<Enrollment> findByCourseIdAndActiveTrue(Long courseId);
    @EntityGraph(attributePaths = {"course", "student"})
    Page<Enrollment> findByCourseIdAndActiveTrue(Long courseId, Pageable pageable);

    // ==================== Status Based Queries ====================
    @EntityGraph(attributePaths = {"course", "student"})
    List<Enrollment> findByStudentIdAndStatusAndActiveTrue(Long studentId, EnrollmentStatus status);
    @EntityGraph(attributePaths = {"course", "student"})
    Page<Enrollment> findByStudentIdAndStatusAndActiveTrue(Long studentId, EnrollmentStatus status, Pageable pageable);
    @EntityGraph(attributePaths = {"course", "student"})
    @Query("SELECT e FROM Enrollment e WHERE e.student.id = :studentId AND e.status = :status AND e.active = true")
    List<Enrollment> findActiveByStudentAndStatus(@Param("studentId") Long studentId, @Param("status") EnrollmentStatus status);

    // ==================== Progress Based Queries ====================
    @EntityGraph(attributePaths = {"course", "student"})
    @Query("SELECT e FROM Enrollment e WHERE e.student.id = :studentId AND e.progress < 100 AND e.active = true")
    List<Enrollment> findInProgressByStudent(@Param("studentId") Long studentId);
    @EntityGraph(attributePaths = {"course", "student"})
    @Query("SELECT e FROM Enrollment e WHERE e.student.id = :studentId AND e.progress >= 100 AND e.active = true")
    List<Enrollment> findCompletedByStudent(@Param("studentId") Long studentId);

    // ==================== Expiry Based Queries ====================
    @EntityGraph(attributePaths = {"course", "student"})
    @Query("SELECT e FROM Enrollment e WHERE e.expiresAt < :now AND e.status = 'ACTIVE' AND e.active = true")
    List<Enrollment> findExpiredEnrollments(@Param("now") LocalDateTime now);
    @EntityGraph(attributePaths = {"course", "student"})
    @Query("SELECT e FROM Enrollment e WHERE e.expiresAt > :now AND e.status = 'ACTIVE' AND e.active = true")
    List<Enrollment> findValidEnrollments(@Param("now") LocalDateTime now);

    // ==================== Statistics ====================
    @Query("SELECT COUNT(e) FROM Enrollment e WHERE e.student.id = :studentId AND e.active = true")
    long countActiveEnrollmentsByStudent(@Param("studentId") Long studentId);

    @Query("SELECT COUNT(e) FROM Enrollment e WHERE e.course.id = :courseId AND e.active = true")
    long countActiveEnrollmentsByCourse(@Param("courseId") Long courseId);

    @Query("SELECT AVG(e.progress) FROM Enrollment e WHERE e.course.id = :courseId AND e.active = true")
    Double averageProgressByCourse(@Param("courseId") Long courseId);

    @Query("SELECT e.status, COUNT(e) FROM Enrollment e WHERE e.course.id = :courseId AND e.active = true GROUP BY e.status")
    List<Object[]> getEnrollmentStatsByCourse(@Param("courseId") Long courseId);

    // ==================== Batch Operations ====================

    @Modifying
    @Transactional
    @Query("UPDATE Enrollment e SET e.status = 'EXPIRED', e.updatedAt = CURRENT_TIMESTAMP WHERE e.expiresAt < :now AND e.status = 'ACTIVE'")
    int expireEnrollments(@Param("now") LocalDateTime now);

    // ✅ اصلاح: استخدم CURRENT_TIMESTAMP بدلاً من NOW()
    @Modifying
    @Transactional
    @Query("UPDATE Enrollment e SET e.lastAccessedAt = CURRENT_TIMESTAMP, e.accessCount = e.accessCount + 1 WHERE e.id = :enrollmentId")
    int recordAccess(@Param("enrollmentId") Long enrollmentId);


    // ==================== Native Queries for Complex Reports ====================

    @Query(value = """
        SELECT
            c.id as courseId,
            c.title as courseTitle,
            COUNT(e.id) as totalEnrollments,
            AVG(e.progress) as averageProgress,
            SUM(CASE WHEN e.status = 'COMPLETED' THEN 1 ELSE 0 END) as completedCount
        FROM enrollments e
        JOIN courses c ON e.course_id = c.id
        WHERE e.active = true
        GROUP BY c.id, c.title
        ORDER BY totalEnrollments DESC
        """, nativeQuery = true)
    List<Object[]> getCourseEnrollmentStats();

    // ==================== Delete Operations ====================

    @Modifying
    @Transactional
    // ✅ اصلاح: استخدم CURRENT_TIMESTAMP بدلاً من NOW()
    @Query("UPDATE Enrollment e SET e.active = false, e.deletedAt = CURRENT_TIMESTAMP, e.deletedBy = :deletedBy WHERE e.student.id = :studentId")
    int cancelAllStudentEnrollments(@Param("studentId") Long studentId, @Param("deletedBy") String deletedBy);

    // أضف هذه الـ Queries في EnrollmentRepository.java

// ==================== New Queries for EnrollmentType ====================

    @Query("SELECT CASE WHEN COUNT(e) > 0 THEN true ELSE false END FROM Enrollment e " +
            "WHERE e.student.id = :studentId AND e.course.id = :courseId " +
            "AND e.enrollmentType = :enrollmentType AND e.active = true")
    boolean existsByStudentIdAndCourseIdAndEnrollmentType(
            @Param("studentId") Long studentId,
            @Param("courseId") Long courseId,
            @Param("enrollmentType") EnrollmentType enrollmentType
    );

    // جيب كل الـ Enrollments لطالب حسب النوع
    @Query("SELECT e FROM Enrollment e WHERE e.student.id = :studentId " +
            "AND e.enrollmentType = :enrollmentType AND e.active = true")
    List<Enrollment> findByStudentIdAndEnrollmentType(
            @Param("studentId") Long studentId,
            @Param("enrollmentType") EnrollmentType enrollmentType
    );

    // جيب كل الـ Enrollments لطالب في Category معين
    @Query("SELECT e FROM Enrollment e WHERE e.student.id = :studentId " +
            "AND e.category.id = :categoryId AND e.active = true")
    List<Enrollment> findByStudentIdAndCategoryId(
            @Param("studentId") Long studentId,
            @Param("categoryId") Long categoryId
    );

    // جيب كل الطلاب المشتركين في Category
    @Query("SELECT DISTINCT e.student FROM Enrollment e WHERE e.category.id = :categoryId AND e.active = true")
    List<Student> findStudentsByCategoryId(@Param("categoryId") Long categoryId);

    // إحصائيات: عدد الـ Enrollments لكل Category
    @Query("SELECT e.category.id, COUNT(e) FROM Enrollment e WHERE e.category IS NOT NULL AND e.active = true GROUP BY e.category.id")
    List<Object[]> countEnrollmentsByCategory();

    @Modifying
    @Query("DELETE FROM Enrollment e WHERE e.course.id = :courseId")
    void deleteByCourseId(@Param("courseId") Long courseId);

    @Modifying
    @Query("DELETE FROM Enrollment e WHERE e.category.id = :categoryId")
    void deleteByCategoryId(@Param("categoryId") Long categoryId);

    boolean existsByCourseId(Long courseId);
    boolean existsByCategoryId(Long categoryId);

    // ── Analytics ────────────────────────────────────────────────

    /** إجمالي الاشتراكات النشطة */
    long countByActiveTrue();

    /** أكثر الكورسات اشتراكاً — يرجع [courseId, courseTitle, count, avgProgress] */
    @Query("""
        SELECT e.course.id, e.course.title, COUNT(e), AVG(e.progress)
        FROM Enrollment e
        WHERE e.active = true AND e.course IS NOT NULL
        GROUP BY e.course.id, e.course.title
        ORDER BY COUNT(e) DESC
    """)
    List<Object[]> findTopCoursesByEnrollment(Pageable pageable);

    /** عدد الاشتراكات الجديدة في فترة */
    @Query("""
        SELECT COUNT(e) FROM Enrollment e
        WHERE e.enrolledAt >= :from AND e.enrolledAt <= :to AND e.active = true
    """)
    long countNewEnrollmentsInPeriod(
        @Param("from") LocalDateTime from,
        @Param("to")   LocalDateTime to);

    /** Sales by enrollment type: [enrollmentType, count] */
    @Query("SELECT e.enrollmentType, COUNT(e) FROM Enrollment e WHERE e.active = true GROUP BY e.enrollmentType")
    List<Object[]> countByEnrollmentType();

    /** Avg quiz score per course: [courseId, courseTitle, avgScore, enrollmentCount] */
    @Query("""
        SELECT e.course.id, e.course.title,
               AVG(e.averageQuizScore), COUNT(e)
        FROM Enrollment e
        WHERE e.active = true AND e.averageQuizScore IS NOT NULL AND e.course IS NOT NULL
        GROUP BY e.course.id, e.course.title
        ORDER BY e.course.title
    """)
    List<Object[]> getAvgQuizScoreByCourse();

    /** Avg quiz score per center: [centerName, avgScore, studentCount] */
    @Query("""
        SELECT e.student.centerName, AVG(e.averageQuizScore), COUNT(DISTINCT e.student.id)
        FROM Enrollment e
        WHERE e.active = true
          AND e.averageQuizScore IS NOT NULL
          AND e.student.centerName IS NOT NULL
        GROUP BY e.student.centerName
        ORDER BY AVG(e.averageQuizScore) DESC
    """)
    List<Object[]> getAvgQuizScoreByCenter();

    /** Per-student progress list: used for student dashboard */
    @Query("""
        SELECT e FROM Enrollment e
        WHERE e.student.id = :studentId AND e.active = true
        ORDER BY e.enrolledAt ASC
    """)
    List<Enrollment> findByStudentIdOrderByEnrolledAt(@Param("studentId") Long studentId);

    /**
     * اشتراكات جديدة كل يوم في الفترة المحددة — [date, count]
     * للـ Analytics Chart (trend line)
     */
    @Query(value = """
        SELECT CAST(enrolled_at AS DATE) AS day, COUNT(*) AS cnt
        FROM enrollments
        WHERE enrolled_at >= :from AND enrolled_at <= :to AND active = true
        GROUP BY CAST(enrolled_at AS DATE)
        ORDER BY day
    """, nativeQuery = true)
    List<Object[]> getDailyEnrollmentTrend(
        @Param("from") LocalDateTime from,
        @Param("to")   LocalDateTime to);

    /**
     * إجمالي عدد الطلاب المكتملين في كل كورس: [courseId, courseTitle, completedCount]
     */
    @Query("""
        SELECT e.course.id, e.course.title, COUNT(e)
        FROM Enrollment e
        WHERE e.active = true AND e.status = 'COMPLETED' AND e.course IS NOT NULL
        GROUP BY e.course.id, e.course.title
        ORDER BY COUNT(e) DESC
    """)
    List<Object[]> getCompletedCountByCourse();

    /**
     * متوسط التقدم لكل كورس: [courseId, courseTitle, avgProgress, enrollmentCount]
     */
    @Query("""
        SELECT e.course.id, e.course.title, AVG(e.progress), COUNT(e)
        FROM Enrollment e
        WHERE e.active = true AND e.course IS NOT NULL
        GROUP BY e.course.id, e.course.title
        ORDER BY AVG(e.progress) DESC
    """)
    List<Object[]> getAvgProgressByCourse();
}