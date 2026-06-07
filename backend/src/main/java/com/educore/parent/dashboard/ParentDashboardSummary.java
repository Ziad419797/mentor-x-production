package com.educore.parent.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParentDashboardSummary {

    private Long               parentId;
    private String             parentName;
    private int                childrenCount;
    private long               totalUnreadNotifications;
    private List<ChildSummaryCard> children;
}
