package com.educore.centerschedule.dto;

import lombok.Data;

@Data
public class CenterScheduleRequest {
    private String centerName;
    private String groupName;
    private String gradeLevel;
    private String dayOfWeek;
    private String startTime;
    private String endTime;
    private String notes;
}
