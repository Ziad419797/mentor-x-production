package com.educore.lessongate;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StudentMaterialViewRepository extends JpaRepository<StudentMaterialView, Long> {

    boolean existsByStudentIdAndMaterialId(Long studentId, Long materialId);

    /** Count how many of the given materialIds this student has viewed */
    @org.springframework.data.jpa.repository.Query(
        "SELECT COUNT(v) FROM StudentMaterialView v WHERE v.studentId = :studentId AND v.materialId IN :materialIds")
    long countViewedByStudentIdAndMaterialIds(
        @org.springframework.data.repository.query.Param("studentId") Long studentId,
        @org.springframework.data.repository.query.Param("materialIds") java.util.List<Long> materialIds);
}
