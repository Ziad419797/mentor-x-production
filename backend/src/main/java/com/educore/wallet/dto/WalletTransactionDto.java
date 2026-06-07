package com.educore.wallet.dto;

import com.educore.wallet.WalletTransaction;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class WalletTransactionDto {
    private Long id;
    private Long studentId;
    private String studentName;
    private BigDecimal amount;
    private String type;
    private String status;
    private LocalDateTime expiresAt;
    private LocalDateTime createdAt;

    public static WalletTransactionDto from(WalletTransaction tx) {
        WalletTransactionDto d = new WalletTransactionDto();
        d.setId(tx.getId());
        d.setAmount(tx.getAmount());
        d.setType(tx.getType() != null ? tx.getType().name() : null);
        d.setStatus(tx.getStatus() != null ? tx.getStatus().name() : null);
        d.setExpiresAt(tx.getExpiresAt());
        d.setCreatedAt(tx.getCreatedAt());
        if (tx.getWallet() != null && tx.getWallet().getStudent() != null) {
            d.setStudentId(tx.getWallet().getStudent().getId());
            d.setStudentName(tx.getWallet().getStudent().getFullName());
        }
        return d;
    }
}
