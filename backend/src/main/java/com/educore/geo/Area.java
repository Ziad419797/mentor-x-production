package com.educore.geo;

import jakarta.persistence.*;
import lombok.*;

/**
 * منطقة (حي/قرية) تابعة لمحافظة معينة.
 * الطالب يختار المحافظة أولاً، ثم يختار المنطقة من القائمة المفلترة.
 */
@Entity
@Table(name = "areas", indexes = {
        @Index(name = "idx_area_governorate", columnList = "governorate_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Area {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** اسم المنطقة — مثل "المقطم" أو "حلوان" */
    @Column(nullable = false, length = 150)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "governorate_id", nullable = false)
    private Governorate governorate;

    /** ترتيب العرض داخل المحافظة */
    @Column
    private Integer displayOrder;
}
