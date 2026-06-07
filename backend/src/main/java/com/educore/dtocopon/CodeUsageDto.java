package com.educore.dtocopon;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CodeUsageDto {

    private Long studentId;
    private String studentName;
    private String studentCode;
    private Integer enrollmentsCreated;
    private boolean walletCharged;
    private LocalDateTime usedAt;
}
