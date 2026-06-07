package com.educore.center.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class CenterResponse {
    private Long   id;
    private String name;
    private String governorate;
    private String area;
    private String address;
    private String phone;
    private boolean active;

    // Services
    private boolean sellsBooks;
    private boolean sellsCodes;

    // Location
    private String mapsLink;

    // Social links
    private String whatsappGroupLink;
    private String instagramLink;
    private String facebookLink;
    private String telegramLink;
    private String youtubeLink;
    private String tiktokLink;

    private LocalDateTime createdAt;
}
