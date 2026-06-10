package com.educore.parent.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class ChildAssignmentResultDto {
    private Long   attemptId;
    private Long   assignmentId;
    private String assignmentTitle;
    private Integer score;
    private Boolean submitted;
    private LocalDateTime submittedAt;
}
