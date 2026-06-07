package com.educore.payment.order;

import com.educore.category.Category;
import com.educore.course.Course;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/* ══════════════════════════════════════════════════════════════
   OrderItem — سطر داخل الطلب (كورس أو باقة)
══════════════════════════════════════════════════════════════ */
@Entity
@Table(
        name = "order_items",
        indexes = {
                @Index(name = "idx_oi_order",  columnList = "order_id"),
                @Index(name = "idx_oi_course", columnList = "course_id"),
                @Index(name = "idx_oi_cat",    columnList = "category_id")
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_oi_order"))
    private Order order;

    /* المنتج المشترى — يمكن أن يكون كورس أو باقة (ليس الاثنين معاً) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id",
            foreignKey = @ForeignKey(name = "fk_oi_category"))
    private Category category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id",
            foreignKey = @ForeignKey(name = "fk_oi_course"))
    private Course course;

    /** CATEGORY | COURSE */
    @Column(nullable = false, length = 20)
    private String productType;

    /** اسم المنتج وقت الشراء — يُحفظ لأن المنتج ممكن يتغير اسمه */
    @Column(nullable = false, length = 300)
    private String productName;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal unitPrice;

    @Column(nullable = false)
    @Builder.Default
    private Integer quantity = 1;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal subtotal;

    @PrePersist
    @PreUpdate
    public void calculateSubtotal() {
        this.subtotal = unitPrice.multiply(BigDecimal.valueOf(quantity));
    }

    public BigDecimal getSubtotal() {
        if (subtotal == null) calculateSubtotal();
        return subtotal;
    }
}