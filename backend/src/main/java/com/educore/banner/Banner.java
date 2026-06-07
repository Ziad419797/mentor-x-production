package com.educore.banner;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;

@Entity
@Table(name = "banners", indexes = {
        @Index(name = "idx_banner_active", columnList = "active"),
        @Index(name = "idx_banner_order", columnList = "displayOrder"),
        @Index(name = "idx_banner_start_end", columnList = "startDate, endDate")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Banner {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(length = 500)
    private String description;

    @Column(nullable = false)
    private String imageUrl;  // رابط الصورة من Cloudinary

    @Column(length = 500)
    private String linkUrl;  // رابط عند الضغط على البانر (اختياري)

    @Builder.Default
    @Column(nullable = false)
    private Boolean active = true;

    @Builder.Default
    @Column(name = "display_order", nullable = false)
    private Integer displayOrder = 0;  // ترتيب العرض

    @Column(name = "start_date")
    private LocalDateTime startDate;  // تاريخ بدء الظهور

    @Column(name = "end_date")
    private LocalDateTime endDate;    // تاريخ انتهاء الظهور

    @Column(name = "created_by")
    private Long createdBy;  // ID المدرس الذي أنشأه

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Helper methods
    public boolean isCurrentlyActive() {
        if (!active) return false;

        LocalDateTime now = LocalDateTime.now();

        if (startDate != null && now.isBefore(startDate)) return false;
        if (endDate != null && now.isAfter(endDate)) return false;

        return true;
    }
}