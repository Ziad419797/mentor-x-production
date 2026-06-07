package com.educore.category;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    @Query("SELECT c FROM Category c WHERE c.level.id = :levelId AND c.active = true")
    Page<Category> findByLevelId(@Param("levelId") Long levelId, Pageable pageable);

    boolean existsByNameAndLevelId(String name, Long levelId);

    Optional<Category> findByNameAndLevelId(String name, Long levelId);
    @Query("""
       SELECT c FROM Category c
       JOIN FETCH c.level
       WHERE c.id = :id
       """)
    Optional<Category> findByIdWithLevel(Long id);
    @Query(value = "SELECT * FROM categories WHERE id = :id", nativeQuery = true)
    Optional<Category> findByIdIncludingInactive(@Param("id") Long id);

    long countByLevelId(Long levelId);

    @Query(value = "SELECT * FROM categories ORDER BY id DESC", nativeQuery = true)
    Page<Category> findAllIncludingInactive(Pageable pageable);
  /** جلب كل كاتيجوريز المستوى (نشط + غير نشط) للاستخدام في لوحة التحكم */
    @Query(value = "SELECT * FROM categories WHERE level_id = :levelId ORDER BY id DESC", nativeQuery = true)
    Page<Category> findAllByLevelIdIncludingInactive(@Param("levelId") Long levelId, Pageable pageable);

}
