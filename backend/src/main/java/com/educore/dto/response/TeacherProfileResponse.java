package com.educore.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TeacherProfileResponse {

    private Long id;
    private String name;
    private String phone;
    private String email;
    private String subject;
    private String bio;
    private String quote;
    private String profileImageUrl;
    private String homeCardImageUrl;
    private String logoUrl;
    private String darkLogoUrl;
    private String teacherCardUrl;
    private String teacherCardDarkUrl;
    private String facebookUrl;
    private String youtubeUrl;
    private String instagramUrl;
    private String tiktokUrl;
    private String whatsappNumber;
    private String telegramUrl;
    private String homeLayoutConfig;
    private boolean enabled;
    private LocalDateTime createdAt;
    private Long studentCount;
    private Long courseCount;
}
