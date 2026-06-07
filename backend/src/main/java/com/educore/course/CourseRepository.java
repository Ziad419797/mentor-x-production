package com.educore.course;

    import org.springframework.data.domain.Page;
    import org.springframework.data.domain.Pageable;
    import org.springframework.data.jpa.repository.JpaRepository;
    import org.springframework.data.jpa.repository.Modifying;
    import org.springframework.data.jpa.repository.Query;
    import org.springframework.data.repository.query.Param;

    import java.util.List;
    import java.util.Optional;

    public interface CourseRepository extends JpaRepository<Course, Long> {

        Page<Course> findByCategoriesId(Long categoryId, Pageable pageable);

        /** إزالة كل الكورسات من التصنيف في جدول الربط — بدون حذف الكورسات نفسها */
        @Modifying
        @Query(value = "DELETE FROM course_category WHERE category_id = :categoryId", nativeQuery = true)
        void unlinkAllCoursesFromCategory(@Param("categoryId") Long categoryId);

        boolean existsByTitle(String title);

        @Query("SELECT COUNT(DISTINCT c) FROM Course c JOIN c.categories cat WHERE cat.level.id = :levelId")
        long countByLevelId(@Param("levelId") Long levelId);
        // CourseRepository.java
        @Query(value = "SELECT * FROM courses WHERE id = :id", nativeQuery = true)
        Optional<Course> findByIdIncludingInactive(@Param("id") Long id);
        // ✅ أحدث الكورسات (للكل - للمعلمين)
        @Query("SELECT c FROM Course c WHERE c.active = true ORDER BY c.createdAt DESC")
        List<Course> findLatestCourses(Pageable pageable);
        // ✅ أحدث الكورسات التي يمكن للطالب الوصول إليها (حسب اشتراكه)
        @Query("SELECT DISTINCT c FROM Course c " +
                "JOIN c.sessions s " +
                "JOIN Enrollment e ON e.course.id = c.id " +
                "WHERE e.student.id = :studentId " +
                "AND e.status = 'ACTIVE' " +
                "AND c.active = true " +
                "ORDER BY c.createdAt DESC")
        List<Course> findLatestCoursesForStudent(@Param("studentId") Long studentId, Pageable pageable);
        // ✅ أضف هذه الدالة لجلب عدد الطلاب المشتركين في كل كورس
        // ✅ تعديل: يجيب ACTIVE, COMPLETED, EXPIRED (يستثني CANCELLED و SUSPENDED)
        // ✅ الكورسات المميزة للطلاب
        @Query("SELECT c FROM Course c WHERE c.featured = true AND c.active = true ORDER BY c.createdAt DESC")
        List<Course> findFeaturedCourses(Pageable pageable);

        @Query("SELECT c.id, COUNT(DISTINCT e.student.id) FROM Course c " +
                "LEFT JOIN Enrollment e ON e.course.id = c.id AND e.active = true " +
                "AND e.status IN ('ACTIVE', 'COMPLETED', 'EXPIRED') " +
                "WHERE c.id IN :courseIds " +
                "GROUP BY c.id")
        List<Object[]> countEnrolledStudentsByCourseIds(@Param("courseIds") List<Long> courseIds);
    }
