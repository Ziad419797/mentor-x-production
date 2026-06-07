package com.educore.center.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CenterRequest {

    @NotBlank(message = "اسم السنتر مطلوب")
    @Size(max = 150)
    private String name;

    @NotBlank(message = "المحافظة مطلوبة")
    @Size(max = 100)
    private String governorate;

    @Size(max = 100)
    private String area;

    @Size(max = 300)
    private String address;

    @Size(max = 20)
    private String phone;

    private boolean active = true;

    // Services
    private boolean sellsBooks = false;
    private boolean sellsCodes = false;

    // Location
    @Size(max = 500)
    private String mapsLink;

    // Social links — all optional
    @Size(max = 500)
    private String whatsappGroupLink;

    @Size(max = 500)
    private String instagramLink;

    @Size(max = 500)
    private String facebookLink;

    @Size(max = 500)
    private String telegramLink;

    @Size(max = 500)
    private String youtubeLink;

    @Size(max = 500)
    private String tiktokLink;
}
