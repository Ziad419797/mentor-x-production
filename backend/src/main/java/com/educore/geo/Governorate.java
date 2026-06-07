package com.educore.geo;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

/**
 * محافظة مصرية.
 * الطالب يختار المحافظة أولاً، ثم تتفلتر المناطق (Area) حسبها.
 */
@Entity
@Table(name = "governorates")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Governorate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** الاسم بالعربي — مثل "القاهرة" */
    @Column(nullable = false, unique = true, length = 100)
    private String nameAr;

    /** الاسم بالإنجليزي — مثل "Cairo" (optional, for API consistency) */
    @Column(length = 100)
    private String nameEn;

    /** رمز ترتيبي لعرض المحافظات بترتيب منطقي (مش أبجدي) */
    @Column
    private Integer displayOrder;

    @Builder.Default
    @OneToMany(mappedBy = "governorate", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Area> areas = new ArrayList<>();
}
