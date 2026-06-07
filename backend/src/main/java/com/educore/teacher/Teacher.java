package com.educore.teacher;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "teachers", indexes = {
        @Index(name = "idx_teacher_phone", columnList = "phone"),
        @Index(name = "idx_teacher_email", columnList = "email")
})
@Getter
@Setter
public class Teacher {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String phone;

    @Column(nullable = false)
    private String password;

    private String name;

    /** Subject the teacher specialises in, e.g. "Mathematics", "Physics" */
    private String subject;

    /** Short bio shown on the teacher's public profile */
    @Column(columnDefinition = "TEXT")
    private String bio;

    /** Motivational quote shown on the student home page */
    @Column(columnDefinition = "TEXT")
    private String quote;

    /** Cloudinary URL for the teacher's profile photo */
    private String profileImageUrl;

    /** Full-design card image shown on the student home page */
    private String homeCardImageUrl;

    /** Teacher's logo image URL */
    private String logoUrl;

    /** Dark-mode logo URL */
    private String darkLogoUrl;

    private String teacherCardUrl;
    private String teacherCardDarkUrl;

    /** Social media links */
    private String facebookUrl;
    private String youtubeUrl;
    private String instagramUrl;
    private String tiktokUrl;
    private String whatsappNumber;
    private String telegramUrl;

    /** JSON config for home page layout widgets */
    @Column(columnDefinition = "TEXT")
    private String homeLayoutConfig;

    /** Optional email — unique when provided */
    @Column(unique = true)
    private String email;

    private boolean enabled;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
}
