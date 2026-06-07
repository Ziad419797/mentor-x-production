package com.educore.notification;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResponse {

    private Long             id;
    private String           title;
    private String           body;
    private NotificationType type;
    private boolean          isRead;
    private LocalDateTime    readAt;
    private Long             relatedEntityId;
    private String           relatedEntityType;
    private Long             studentId;
    private LocalDateTime    createdAt;
}
