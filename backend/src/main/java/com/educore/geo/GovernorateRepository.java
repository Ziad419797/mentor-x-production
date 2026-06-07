package com.educore.geo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GovernorateRepository extends JpaRepository<Governorate, Long> {

    List<Governorate> findAllByOrderByDisplayOrderAscNameArAsc();

    Optional<Governorate> findByNameArIgnoreCase(String nameAr);

    Optional<Governorate> findByNameEnIgnoreCase(String nameEn);
}
