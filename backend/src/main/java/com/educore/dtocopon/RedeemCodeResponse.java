package com.educore.dtocopon;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RedeemCodeResponse {

    private boolean success;
    private String message;
    private String targetType;
    private String targetName;
    private int enrollmentsCreated;
    private List<String> unlockedCourses;
}