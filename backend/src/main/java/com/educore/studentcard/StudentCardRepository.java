package com.educore.studentcard;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StudentCardRepository extends JpaRepository<StudentCard, Long> {

    Optional<StudentCard> findByStudentId(Long studentId);

    Optional<StudentCard> findByQrToken(String qrToken);

    Optional<StudentCard> findByCardCode(String cardCode);

    boolean existsByStudentId(Long studentId);

    boolean existsByQrToken(String qrToken);

    boolean existsByCardCode(String cardCode);
}
