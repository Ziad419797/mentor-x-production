package com.educore.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateTeacherProfileRequest {

    @Size(min = 2, max = 100, message = "الاسم يجب أن يكون بين 2 و 100 حرف")
    private String name;

    @Email(message = "صيغة البريد الإلكتروني غير صحيحة")
    private String email;

    /** Subject the teacher teaches */
    private String subject;

    @Size(max = 500, message = "النبذة التعريفية لا تتجاوز 500 حرف")
    private String bio;

    @Size(max = 300, message = "الاقتباس لا يتجاوز 300 حرف")
    private String quote;

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
}
