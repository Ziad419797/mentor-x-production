package com.educore.lessongate;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StudentMaterialViewRepository extends JpaRepository<StudentMaterialView, Long> {

    boolean existsByStudentIdAndMaterialId(Long studentId, Long materialId);

    java.util.Optional<StudentMaterialView> findByStudentIdAndMaterialId(Long studentId, Long materialId);

    /** كل سجلات المشاهدة لطالب معين ضمن مجموعة من المواد (تستخدم لحساب التقدم بناءً على مدة المشاهدة) */
    @org.springframework.data.jpa.repository.Query(
        "SELECT v FROM StudentMaterialView v WHERE v.studentId = :studentId AND v.materialId IN :materialIds")
    java.util.List<StudentMaterialView> findByStudentIdAndMaterialIdIn(
        @org.springframework.data.repository.query.Param("studentId") Long studentId,
        @org.springframework.data.repository.query.Param("materialIds") java.util.List<Long> materialIds);

    /** Count how many of the given materialIds this student has viewed */
    @org.springframework.data.jpa.repository.Query(
        "SELECT COUNT(v) FROM StudentMaterialView v WHERE v.studentId = :studentId AND v.materialId IN :materialIds")
    long countViewedByStudentIdAndMaterialIds(
        @org.springframework.data.repository.query.Param("studentId") Long studentId,
        @org.springframework.data.repository.query.Param("materialIds") java.util.List<Long> materialIds);
}
