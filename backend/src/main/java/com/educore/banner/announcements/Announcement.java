package com.educore.banner.announcements;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "announcements", indexes = {
        @Index(name = "idx_announcement_active", columnList = "active"),
        @Index(name = "idx_announcement_date", columnList = "announcementDate"),
        @Index(name = "idx_announcement_created", columnList = "createdAt")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Announcement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Builder.Default
    @Column(nullable = false)
    private Boolean active = true;

    @Column(name = "announcement_date")
    private LocalDateTime announcementDate;  // تاريخ الإعلان (متى يظهر)

    @Column(name = "expiry_date")
    private LocalDateTime expiryDate;  // تاريخ انتهاء الإعلان

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

        if (announcementDate != null && now.isBefore(announcementDate)) return false;
        if (expiryDate != null && now.isAfter(expiryDate)) return false;

        return true;
    }
}