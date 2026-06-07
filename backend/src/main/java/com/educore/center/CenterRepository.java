package com.educore.center;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CenterRepository extends JpaRepository<Center, Long> {

    /** كل السناتر النشطة — للقوائم العامة والتسجيل */
    List<Center> findByActiveTrueOrderByGovernorateAscNameAsc();

    /** السناتر في محافظة معينة */
    List<Center> findByGovernorateAndActiveTrueOrderByNameAsc(String governorate);

    /** بحث باسم السنتر (بدقة كاملة) */
    Optional<Center> findByNameIgnoreCase(String name);

    /** هل اسم السنتر موجود؟ */
    boolean existsByNameIgnoreCase(String name);
}
