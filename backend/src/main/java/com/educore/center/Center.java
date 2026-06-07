package com.educore.center;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * يمثل سنتر (فرع) للمنصة.
 * كل سنتر ممكن يبقي ليه:
 *   - بيانات الموقع (محافظة + عنوان)
 *   - روابط سوشيال ميديا خاصة بيه (واتساب، إنستاجرام، إلخ)
 *
 * الطالب بيختار السنتر عند التسجيل (centerName).
 * الـ Frontend يعرض روابط السنتر بتاع الطالب بعد الـ login.
 */
@Entity
@Table(name = "centers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Center {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ─── Identity ────────────────────────────────────────────────

    /** اسم السنتر — مثل "القاهرة - المقطم" أو "الجيزة - الدقي" */
    @Column(nullable = false, length = 150)
    private String name;

    /** المحافظة التي يقع فيها السنتر */
    @Column(nullable = false, length = 100)
    private String governorate;

    /** المنطقة / الحي */
    @Column(length = 150)
    private String area;

    /** العنوان التفصيلي (نص) */
    @Column(length = 300)
    private String address;

    /** رقم التليفون الخاص بالسنتر */
    @Column(length = 20)
    private String phone;

    /** هل السنتر نشط (يظهر للطلاب في قائمة التسجيل)؟ */
    @Builder.Default
    @Column(nullable = false)
    private boolean active = true;

    // ─── Social Links ─────────────────────────────────────────────

    /** رابط جروب واتساب الخاص بهذا السنتر */
    @Column(name = "whatsapp_group_link", length = 500)
    private String whatsappGroupLink;

    /** رابط صفحة إنستاجرام الخاصة بهذا السنتر (أو الرئيسية لو مشتركين) */
    @Column(name = "instagram_link", length = 500)
    private String instagramLink;

    /** رابط صفحة فيسبوك الخاصة بهذا السنتر */
    @Column(name = "facebook_link", length = 500)
    private String facebookLink;

    /** رابط قناة أو جروب تيليجرام الخاص بهذا السنتر */
    @Column(name = "telegram_link", length = 500)
    private String telegramLink;

    /** رابط يوتيوب (لو في محتوى مسجل) */
    @Column(name = "youtube_link", length = 500)
    private String youtubeLink;

    /** رابط تيك توك */
    @Column(name = "tiktok_link", length = 500)
    private String tiktokLink;

    // ─── Location ────────────────────────────────────────────────

    /** رابط خرائط جوجل للسنتر */
    @Column(name = "maps_link", length = 500)
    private String mapsLink;

    // ─── Services ─────────────────────────────────────────────────

    /** هل السنتر بيبيع كتب؟ */
    @Builder.Default
    @Column(name = "sells_books", nullable = false)
    private boolean sellsBooks = false;

    /** هل السنتر بيبيع أكواد دخول؟ */
    @Builder.Default
    @Column(name = "sells_codes", nullable = false)
    private boolean sellsCodes = false;

    // ─── Audit ────────────────────────────────────────────────────

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "created_by", length = 50)
    private String createdBy;
}
