package com.educore.lessonmaterial;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LessonMaterialRepository extends JpaRepository<LessonMaterial, Long> {
    // ✅ استخدم @Query زي ما عملنا في LessonRepository
    @Query(
        value = "SELECT DISTINCT m FROM LessonMaterial m JOIN m.weeks w WHERE w.id = :weekId AND m.active = true",
        countQuery = "SELECT COUNT(DISTINCT m) FROM LessonMaterial m JOIN m.weeks w WHERE w.id = :weekId AND m.active = true"
    )
    Page<LessonMaterial> findByWeeksId(@Param("weekId") Long weekId, Pageable pageable);
    @Query(value = "SELECT * FROM lesson_materials WHERE id = :id", nativeQuery = true)
    Optional<LessonMaterial> findByIdIncludingInactive(@Param("id") Long id);

    /** Used by AccessService — resolves which course a material belongs to via its weeks → sessions → courses */
    @Query("SELECT DISTINCT c.id FROM LessonMaterial m JOIN m.weeks w JOIN w.sessions s JOIN s.courses c WHERE m.id = :materialId")
    Long findCourseIdById(@Param("materialId") Long materialId);

    /** Used by LessonMaterialController — يحدد أسبوع المادة لتسجيل الحضور أونلاين تلقائياً عند فتح الفيديو */
    @Query("SELECT MIN(w.id) FROM LessonMaterial m JOIN m.weeks w WHERE m.id = :materialId")
    Long findWeekIdByMaterialId(@Param("materialId") Long materialId);

    /** Used by LessonGateService — get contentOrder of the course this material belongs to */
    @Query("SELECT DISTINCT c.contentOrder FROM LessonMaterial m JOIN m.weeks w JOIN w.sessions s JOIN s.courses c WHERE m.id = :materialId")
    String findContentOrderByMaterialId(@Param("materialId") Long materialId);

    /** Used by LessonGateService (LOCK_BY_ELEMENT) — get the previous material in same week by orderNumber */
    @Query("""
        SELECT m FROM LessonMaterial m
        JOIN m.weeks w
        WHERE w IN (SELECT w2 FROM LessonMaterial m2 JOIN m2.weeks w2 WHERE m2.id = :materialId)
          AND m.orderNumber < :currentOrder
          AND m.active = true
        ORDER BY m.orderNumber DESC
    """)
    List<LessonMaterial> findPreviousMaterialsInSameWeek(
            @Param("materialId") Long materialId,
            @Param("currentOrder") Integer currentOrder);

    /** Get orderNumber for a specific material */
    @Query("SELECT m.orderNumber FROM LessonMaterial m WHERE m.id = :materialId")
    Integer findOrderNumberById(@Param("materialId") Long materialId);

    /** Count active materials in a week — used by LOCK_BY_SESSION gate */
    @Query("SELECT COUNT(DISTINCT m) FROM LessonMaterial m JOIN m.weeks w WHERE w.id = :weekId AND m.active = true")
    long countActiveByWeekId(@Param("weekId") Long weekId);

    /** Get all active material IDs in a week — used to check if all are viewed */
    @Query("SELECT m.id FROM LessonMaterial m JOIN m.weeks w WHERE w.id = :weekId AND m.active = true")
    List<Long> findActiveIdsByWeekId(@Param("weekId") Long weekId);
//    Page<LessonMaterial> findByWeeksId(Long lessonId, Pageable pageable);

    @Query("""
        SELECT COUNT(DISTINCT m) FROM LessonMaterial m
        JOIN m.weeks w JOIN w.sessions s JOIN s.courses c JOIN c.categories cat
        WHERE cat.level.id = :levelId AND m.materialType IN ('VIDEO','YOUTUBE')
    """)
    long countVideosByLevelId(@Param("levelId") Long levelId);

    @Query("""
        SELECT COUNT(DISTINCT m) FROM LessonMaterial m
        JOIN m.weeks w JOIN w.sessions s JOIN s.courses c JOIN c.categories cat
        WHERE cat.level.id = :levelId
    """)
    long countByLevelId(@Param("levelId") Long levelId);

    /**
     * كل مواد الفيديو الفعّالة في كورس معين (عبر week → session → course) — تُستخدم في حساب التقدم
     * بناءً على طول الفيديو ومدة المشاهدة. الملفات (PDF/DOC/...) مُستثناة عمداً من حساب التقدم.
     */
    @Query("""
        SELECT DISTINCT m FROM LessonMaterial m
        JOIN m.weeks w JOIN w.sessions s JOIN s.courses c
        WHERE c.id = :courseId AND m.active = true AND m.materialType IN ('VIDEO','YOUTUBE')
    """)
    List<LessonMaterial> findVideoMaterialsByCourseId(@Param("courseId") Long courseId);

}