package com.educore.dtocourse.response;


import com.educore.lessonmaterial.MaterialType;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LessonMaterialResponse {

    private Long id;
    private MaterialType materialType;

    /** الـ URL الأصلي اللي أدخله المدرس */
    private String fileUrl;

    /**
     * الـ URL الجاهز للـ iframe — مختلف عن fileUrl بس في حالة YouTube.
     *
     * يوتيوب:  https://www.youtube.com/embed/{videoId}?rel=0&modestbranding=1
     * غيره:    نفس fileUrl
     *
     * الفرونتند يستخدم دايماً embedUrl للعرض.
     */
    private String embedUrl;

    /** الـ Video ID لو النوع YOUTUBE — للفرونتند لو محتاج يعمل حاجة إضافية */
    private String youtubeVideoId;

    private String fileName;
    private Long fileSize;
    private Set<Long> weekIds;
    private Integer downloadCount;
    private Boolean preview;
    private Long durationSeconds;
    private LocalDateTime createdAt;
    private Integer orderNumber;
}
