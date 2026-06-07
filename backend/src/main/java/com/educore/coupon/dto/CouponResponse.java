package com.educore.coupon.dto;

import com.educore.coupon.CouponType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class CouponResponse {

    private Long        id;
    private String      code;
    private String      description;
    private CouponType  type;
    private BigDecimal  value;
    private BigDecimal  maxDiscount;
    private BigDecimal  minAmount;
    private Integer     maxUses;
    private int         usedCount;
    private boolean     active;
    private boolean     valid;           // isValid() من entity
    private LocalDateTime expiresAt;
    private String      createdBy;
    private LocalDateTime createdAt;
}
