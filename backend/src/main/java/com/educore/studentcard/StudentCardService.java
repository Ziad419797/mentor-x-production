package com.educore.studentcard;

import com.educore.exception.ResourceNotFoundException;
import com.educore.student.Student;
import com.educore.student.StudentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class StudentCardService {

    private final StudentCardRepository cardRepository;
    private final StudentRepository     studentRepository;

    // ──────────────────────────────────────────────────���──────────
    // إصدار كارنيه جديد للطالب
    // ─────────────────────────────────────────────────────────────

    @Transactional
    public StudentCardResponse issueCard(Long studentId, String issuedBy) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("الطالب غير موجود: " + studentId));

        // لو فيه كارنيه قديم → وقفه وأصدر جديد
        cardRepository.findByStudentId(studentId).ifPresent(old -> {
            old.setActive(false);
            old.setDeactivatedAt(LocalDateTime.now());
            cardRepository.save(old);
            log.info("Deactivated old card for student {}", studentId);
        });

        String cardCode = generateCardCode(studentId);
        String qrToken  = UUID.randomUUID().toString().replace("-", "");

        StudentCard card = StudentCard.builder()
                .student(student)
                .cardCode(cardCode)
                .qrToken(qrToken)
                .issuedBy(issuedBy)
                .active(true)
                .build();

        cardRepository.save(card);
        log.info("Card issued for student {} — code: {}", studentId, cardCode);

        return toResponse(card);
    }

    // ─────────────────────────────────────────────────────────────
    // التحقق من الـ QR Token (يُستدعى من موظف السنتر عند الـ Scan)
    // ─────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public StudentCardResponse validateQrToken(String qrToken) {
        StudentCard card = cardRepository.findByQrToken(qrToken)
                .orElseThrow(() -> new ResourceNotFoundException("QR غير معروف أو منتهي"));

        if (!card.isActive()) {
            throw new IllegalStateException("الكارنيه موقوف — يرجى إصدار كارنيه جديد");
        }

        return toResponse(card);
    }

    // ─────────────────────────────────────────────────────────────
    // بيانات كارنيه الطالب (الطالب يشوف كارنيهه)
    // ─────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public StudentCardResponse getMyCard(Long studentId) {
        StudentCard card = cardRepository.findByStudentId(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("لم يتم إصدار كارنيه بعد"));

        if (!card.isActive()) {
            throw new IllegalStateException("الكارنيه موقوف — تواصل مع الإدارة");
        }

        return toResponse(card);
    }

    // ─────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────

    private String generateCardCode(Long studentId) {
        // Format: EDU-YYYYXXXXX  (e.g. EDU-2024-00042)
        int year = LocalDateTime.now().getYear();
        return String.format("EDU-%d-%05d", year, studentId);
    }

    private StudentCardResponse toResponse(StudentCard card) {
        return StudentCardResponse.builder()
                .id(card.getId())
                .studentId(card.getStudent().getId())
                .studentName(card.getStudent().getFullName())
                .studentCode(card.getStudent().getStudentCode())
                .cardCode(card.getCardCode())
                .qrToken(card.getQrToken())
                .active(card.isActive())
                .issuedAt(card.getIssuedAt())
                .issuedBy(card.getIssuedBy())
                .build();
    }
}
