package com.educore.dto.response;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentResponse {
    private Long id;
    private String phone;
    private String parentPhone;
    private String firstName;
    private String nationalId;
    private LocalDate dateOfBirth;
    private String secondName;
    private String thirdName;
    private String fourthName;
    private String fullName;
    private String shortName;
    private String grade;
    private String governorate;
    private String area;
    private String schoolName;
    private String schoolType;
    private String educationDepartment;
    private Boolean online;
    private String studyType;
    private String studyLocation;
    private String centerName;
    private String profileImageUrl;
    private String identityDocumentUrl;
    private String status;
    private String studentCode;
    private boolean enabled;
    private boolean hasActiveSession;
    private boolean isPending;
    private boolean isActive;
    private boolean isRejected;
    private LocalDateTime createdAt;
    private LocalDateTime lastLoginAt;
    private Integer devicesCount;
    private Integer loginCount;
    private String rejectionReason;
    private String rejectedBy;
    private Long groupId;        // الجروب اللي الطالب مسجل فيه
    private String groupName;

    // ── ID Verification ──────────────────────────────────────────────────
    /** NOT_CHECKED / VERIFIED / REJECTED */
    private String idVerificationStatus;
    /** النتيجة الكاملة من موديل التحقق (parsed JSON) */
    private Object idVerificationResult;

    // ── Enriched fields (added by service, not entity) ──────────────────
    private java.math.BigDecimal walletBalance;
    private Long attendanceCount;
    private Double attendanceRate;
}
