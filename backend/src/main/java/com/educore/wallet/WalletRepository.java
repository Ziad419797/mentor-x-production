package com.educore.wallet;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.math.BigDecimal;
import java.util.Optional;


@Repository
public interface WalletRepository extends JpaRepository<Wallet, Long> {

    Optional<Wallet> findByStudentId(Long studentId);

    boolean existsByStudentId(Long studentId);

    /** Lock للتحكم في الـ concurrent deposits/withdrawals */
    @Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT w FROM Wallet w WHERE w.student.id = :studentId")
    Optional<Wallet> findByStudentIdWithLock(@Param("studentId") Long studentId);

    long countByBalanceGreaterThan(BigDecimal balance);
}

