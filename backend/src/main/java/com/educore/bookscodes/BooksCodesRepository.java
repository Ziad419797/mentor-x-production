package com.educore.bookscodes;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface BooksCodesRepository extends JpaRepository<BooksCodesLocation, Long> {
    List<BooksCodesLocation> findByActiveTrueOrderByNameAsc();
}
