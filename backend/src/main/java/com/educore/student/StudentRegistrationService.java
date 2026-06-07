package com.educore.student;

import com.educore.common.FileUploadService;
import com.educore.dto.mapper.RegistrationMapper;
import com.educore.dto.mapper.StudentMapper;
import com.educore.dto.request.CompleteRegisterRequest;
import com.educore.dto.request.StartRegisterRequest;
import com.educore.dto.response.CompleteRegisterResponse;
import com.educore.dto.response.PhoneCheckResponse;
import com.educore.dto.response.ResendOtpResponse;
import com.educore.dto.response.StartRegisterResponse;
import com.educore.exception.FileUploadException;
import com.educore.exception.ResourceAlreadyExistsException;
import com.educore.parent.Parent;
import com.educore.parent.ParentRepository;
import com.educore.security.JwtService;
import com.educore.security.OtpService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class StudentRegistrationService {

    private final ParentRepository parentRepository;
    private final StudentRepository studentRepository;
    private final OtpService otpService;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final StudentMapper studentMapper;
    private final RegistrationMapper registrationMapper;
    private final FileUploadService fileUploadService;

    /**
     * Step 1: Start registration and send OTP
     */
    public StartRegisterResponse startRegistration(StartRegisterRequest request) {
        // Check phone is not already registered
        if (studentRepository.findByPhone(request.getPhone()).isPresent()) {
            throw new ResourceAlreadyExistsException("Phone number is already registered");
        }

        // Generate and send OTP
        String otp = otpService.generateAndSendOtp(request.getPhone());
        log.info("Registration started for phone: {}", request.getPhone());

        return registrationMapper.toStartRegisterResponse(
                "OTP sent successfully",
                request.getPhone(),
                otp
        );
    }

    /**
     * Step 2: Complete registration after OTP verification
     */
    @Transactional
    public CompleteRegisterResponse completeRegistration(
            CompleteRegisterRequest request,
            HttpServletRequest httpRequest
    ) {
        // 1. Verify OTP
        otpService.verifyOtp(request.getPhone(), String.valueOf(request.getOtp()));

        // 2. Check phone is not already registered
        if (studentRepository.findByPhone(request.getPhone()).isPresent()) {
            throw new ResourceAlreadyExistsException("Phone number is already registered");
        }

        // 2b. Check parent phone is not registered as a student
        if (studentRepository.findByPhone(request.getParentPhone()).isPresent()) {
            throw new IllegalArgumentException("رقم ولي الأمر مسجل بالفعل كطالب في المنصة. يرجى استخدام رقم مختلف لولي الأمر.");
        }

        // 3. Validate center name if study mode is center
        if (request.getOnline() != null && !request.getOnline() &&
                (request.getCenterName() == null || request.getCenterName().isBlank())) {
            throw new IllegalArgumentException("Center name is required when study mode is center");
        }

        // لو أونلاين: امسح attendanceGroupId تماماً — الطالب لا يُضاف لأي جروب وقت التسجيل
        // centerName تُحفظ كـ "السنتر المستقبلي" في بيانات الطالب
        if (Boolean.TRUE.equals(request.getOnline())) {
            request.setAttendanceGroupId(null);
        }

        Map<String, String> uploadedFiles = uploadStudentFiles(request);

        // 4. Create student entity via mapper
        Student student = studentMapper.toEntity(request);
        student.setPassword(passwordEncoder.encode(request.getPassword()));

        // Attach uploaded file URLs
        if (uploadedFiles.containsKey("profilePicture")) {
            student.setProfileImageUrl(uploadedFiles.get("profilePicture"));
        }
        if (uploadedFiles.containsKey("IdentityDocument")) {
            student.setIdentityDocumentUrl(uploadedFiles.get("IdentityDocument"));
        }

        // 5. Link or create parent account
        Parent parent = parentRepository.findByPhone(request.getParentPhone())
                .orElseGet(() -> {
                    log.info("Auto-creating parent account for phone: {}", request.getParentPhone());
                    return parentRepository.save(Parent.builder()
                            .phone(request.getParentPhone())
                            .build());
                });

        student.setParent(parent);

        // 6. Save student
        Student savedStudent = studentRepository.save(student);
        log.info("Student registered successfully: code={}, phone={}", savedStudent.getStudentCode(), savedStudent.getPhone());

        // 7. Generate JWT token
        String sessionId = UUID.randomUUID().toString();
        String token = jwtService.generateToken(
                savedStudent.getPhone(),
                "STUDENT",
                savedStudent.getId(),
                null,
                sessionId,
                savedStudent.getStudentCode(),
                savedStudent.getShortName(),
                savedStudent.getStatus().name()
        );

        return registrationMapper.toCompleteRegisterResponse(
                "Registration successful! Your account is pending admin approval.",
                savedStudent,
                token
        );
    }

    /**
     * Resend OTP
     */
    public ResendOtpResponse resendOtp(String phone) {
        // Check phone is not already registered
        if (studentRepository.findByPhone(phone).isPresent()) {
            throw new ResourceAlreadyExistsException("Phone number is already registered");
        }

        // Regenerate and send OTP
        otpService.generateAndSendOtp(phone);
        log.info("OTP resent for phone: {}", phone);

        return new ResendOtpResponse("OTP resent successfully");
    }

    /**
     * Check phone registration status
     */
    public PhoneCheckResponse checkPhoneStatus(String phone) {
        return studentRepository.findByPhone(phone)
                .map(student -> new PhoneCheckResponse(
                        true,
                        "Phone number is already registered",
                        student.getStatus() != null ? student.getStatus().name() : null,
                        student.getStudentCode()
                ))
                .orElse(new PhoneCheckResponse(
                        false,
                        "Phone number is available for registration",
                        null,
                        null
                ));
    }

    /**
     * Get student repository (used externally)
     */
    public StudentRepository getStudentRepository() {
        return studentRepository;
    }

    /**
     * Upload student files (profile image + identity document)
     */
    private Map<String, String> uploadStudentFiles(CompleteRegisterRequest request) {
        Map<String, String> uploadedFiles = new HashMap<>();
        List<String> uploadedUrls = new ArrayList<>();

        try {
            if (request.getProfileImageUrl() != null && !request.getProfileImageUrl().isEmpty()) {
                String profileUrl = fileUploadService.uploadProfilePicture(request.getProfileImageUrl());
                uploadedFiles.put("profilePicture", profileUrl);
                uploadedUrls.add(profileUrl);
                log.info("Profile picture uploaded for phone: {}", request.getPhone());
            }

            if (request.getIdentityDocumentUrl() != null && !request.getIdentityDocumentUrl().isEmpty()) {
                String documentUrl = fileUploadService.uploadBirthCertificate(request.getIdentityDocumentUrl());
                uploadedFiles.put("IdentityDocument", documentUrl);
                uploadedUrls.add(documentUrl);
                log.info("Identity document uploaded for phone: {}", request.getPhone());
            }

        } catch (Exception e) {
            if (!uploadedUrls.isEmpty()) {
                fileUploadService.deleteFiles(uploadedUrls);
            }
            log.error("File upload failed for phone {}: {}", request.getPhone(), e.getMessage());
            throw new FileUploadException("File upload failed", "student", request.getPhone(), e);
        }

        return uploadedFiles;
    }
}
