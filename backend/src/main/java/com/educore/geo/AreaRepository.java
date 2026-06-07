package com.educore.geo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AreaRepository extends JpaRepository<Area, Long> {

    /** كل مناطق محافظة معينة — مرتبة بـ displayOrder ثم الاسم */
    List<Area> findByGovernorateIdOrderByDisplayOrderAscNameAsc(Long governorateId);

    /** نفس السابقة لكن بالاسم العربي للمحافظة */
    List<Area> findByGovernorateNameArIgnoreCaseOrderByDisplayOrderAscNameAsc(String governorateNameAr);
}
