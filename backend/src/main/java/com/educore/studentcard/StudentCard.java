package com.educore.studentcard;

import com.educore.student.Student;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * البطاقة الفيزيائية للطالب — تحتوي على QR Code.
 *
 * cardCode  → الرقم المطبوع على الكارنيه (يظهر للطالب)
 * qrToken   → التوكن المشفّر داخل الـ QR (لا يظهر بشكل مباشر — أكثر أماناً)
 *
 * لما موظف السنتر يعمل Scan للـ QR، يبعت qrToken للـ API
 * اللي بيرجع بيانات الطالب ويسجّل الحضور.
 */
@Entity
@Table(
    name = "student_cards",
    indexes = {
        @Index(name = "idx_card_student",   columnList = "student_id"),
        @Index(name = "idx_card_code",      columnList = "card_code"),
        @Index(name = "idx_card_qr_token",  columnList = "qr_token")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentCard {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ─── ربط بالطالب (كارنيه واحد فعال لكل طالب) ─────────
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false, unique = true)
    private Student student;

    // ─── الكود المطبوع على الكارنيه ───────────────────────
    @Column(name = "card_code", nullable = false, unique = true, length = 20)
    private String cardCode;

    // ─── التوكن المشفّر في الـ QR ─────────────────────────
    @Column(name = "qr_token", nullable = false, unique = true, length = 100)
    private String qrToken;

    // ─── حالة الكارنيه ─────────────────────────────��──────
    @Builder.Default
    @Column(nullable = false)
    private boolean active = true;

    // ─── التواريخ ───────────────────────────��─────────────
    @CreationTimestamp
    @Column(name = "issued_at", updatable = false)
    private LocalDateTime issuedAt;

    @Column(name = "deactivated_at")
    private LocalDateTime deactivatedAt;

    @Column(name = "issued_by", length = 100)
    private String issuedBy;
}
