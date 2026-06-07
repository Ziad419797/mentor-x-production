package com.educore.student;

import com.educore.attendance.group.AttendanceGroup;
import com.educore.attendance.group.AttendanceGroupRepository;
import com.educore.dto.mapper.ApplicationInfoMapper;
import com.educore.dto.request.CompleteRegisterRequest;
import com.educore.dto.request.ResendOtpRequest;
import com.educore.dto.request.StartRegisterRequest;
import com.educore.dto.response.CompleteRegisterResponse;
import com.educore.dto.response.PhoneCheckResponse;
import com.educore.dto.response.ResendOtpResponse;
import com.educore.dto.response.StartRegisterResponse;
import com.educore.exception.ResourceAlreadyExistsException;
import com.educore.location.LocationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Valid;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/student/register")
@RequiredArgsConstructor
public class StudentRegistrationController {

    private final LocationService locationService;
    private final StudentRegistrationService registrationService;
    private final ApplicationInfoMapper applicationInfoMapper;
    private final ObjectMapper objectMapper;
    private final AttendanceGroupRepository groupRepo;
    private final Validator validator;


    /* =========================
       Location Endpoints
       ========================= */

    @GetMapping("/governorates")
    public ResponseEntity<?> getGovernorates() {
        try {
            List<String> governorates = locationService.getGovernorates();
            return ResponseEntity.ok(governorates);
        } catch (Exception e) {
            log.error("Error getting governorates: {}", e.getMessage());
            return buildErrorResponse("فشل في جلب المحافظات", e.getMessage());
        }
    }

    @GetMapping("/areas/{governorate}")
    public ResponseEntity<?> getAreas(@PathVariable String governorate) {
        try {
            List<String> areas = locationService.getAreas(governorate);

            if (areas == null || areas.isEmpty()) {
                return buildNotFoundResponse("لا توجد مناطق لهذه المحافظة", governorate);
            }

            return ResponseEntity.ok(areas);
        } catch (Exception e) {
            log.error("Error getting areas for governorate {}: {}", governorate, e.getMessage());
            return buildErrorResponse("فشل في جلب المناطق", e.getMessage());
        }
    }

    /* =========================
       Public Groups (Time Slots) for Registration
       ========================= */

    /**
     * GET /api/student/register/centers-with-groups?levelId=Y
     * يرجع السناتر التي لها جروبات للصف الدراسي المعين — بدون توكن
     */
    @GetMapping("/centers-with-groups")
    public ResponseEntity<?> getCentersWithGroups(
            @RequestParam Long levelId,
            @RequestParam(defaultValue = "false") boolean online) {
        // لو أونلاين: لا يُعرض عليه قائمة السناتر من الجروبات
        if (online) return ResponseEntity.ok(List.of());
        return getCentersWithGroupsInternal(levelId);
    }

    private ResponseEntity<?> getCentersWithGroupsInternal(Long levelId) {
        try {
            List<AttendanceGroup> groups = groupRepo.findPublicByLevel(levelId);
            List<Map<String, Object>> result = groups.stream()
                .filter(g -> g.getCenter() != null)
                .map(g -> g.getCenter())
                .distinct()
                .map(c -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("id", c.getId());
                    m.put("name", c.getName());
                    m.put("governorate", c.getGovernorate());
                    m.put("address", c.getAddress());
                    return m;
                }).toList();
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.ok(List.of());
        }
    }

    /**
     * GET /api/student/register/groups?centerId=X&levelId=Y
     * يرجع مجموعات الحضور (المواعيد) المتاحة لسنتر + صف معين — بدون توكن
     */
    @GetMapping("/groups")
    public ResponseEntity<?> getGroupsForRegistration(
            @RequestParam Long centerId,
            @RequestParam Long levelId,
            @RequestParam(defaultValue = "false") boolean online) {
        // لو الطالب أونلاين: لا يُعرض عليه أي جروب — السنتر المستقبلي بس يتحفظ في بياناته
        if (online) return ResponseEntity.ok(List.of());
        try {
            List<AttendanceGroup> groups = groupRepo.findPublicByCenterAndLevel(centerId, levelId);
            List<Map<String, Object>> result = groups.stream().map(g -> {
                long count = groupRepo.countActiveMembers(g.getId());
                boolean full = g.getMaxCapacity() != null && count >= g.getMaxCapacity();
                Map<String, Object> m = new HashMap<>();
                m.put("id", g.getId());
                m.put("title", g.getTitle());
                m.put("dayOfWeek", g.getDayOfWeek());
                m.put("meetingTime", g.getMeetingTime());
                m.put("description", g.getDescription());
                m.put("maxCapacity", g.getMaxCapacity());
                m.put("currentCount", count);
                m.put("isFull", full);
                return m;
            }).toList();
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.ok(List.of());
        }
    }

    /* =========================
       Registration Endpoints
       ========================= */

    @PostMapping("/start")
    public ResponseEntity<?> startRegistration(@Valid @RequestBody StartRegisterRequest request) {
        try {
            log.info("Starting registration for phone: {}", request.getPhone());
            StartRegisterResponse response = registrationService.startRegistration(request);
            return buildSuccessResponse(response, "VERIFY_OTP");
        } catch (ResourceAlreadyExistsException ex) {
            log.warn("Phone already exists: {}", request.getPhone());
            return buildConflictResponse(ex.getMessage(), request.getPhone());
        } catch (Exception ex) {
            log.error("Error starting registration for {}: {}", request.getPhone(), ex.getMessage());
            return buildErrorResponse("فشل في بدء التسجيل", ex.getMessage(), request.getPhone());
        }
    }

    @PostMapping(value = "/complete", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    public ResponseEntity<?> completeRegistration(
            @RequestPart("data") @Valid String dataJson,  // البيانات كـ JSON string
            @RequestPart(value = "profileImage", required = false) MultipartFile profileImage,
            @RequestPart(value = "identityDocument", required = false) MultipartFile identityDocument,
            HttpServletRequest httpRequest
    ) {
        String phone = "غير معروف";
        try {
            // تحويل JSON إلى كائن CompleteRegisterRequest
            CompleteRegisterRequest request = objectMapper.readValue(dataJson, CompleteRegisterRequest.class);
            phone = request.getPhone();

            // التحقق من صحة البيانات يدوياً (لأن @Valid لا يعمل مع multipart JSON string)
            Set<ConstraintViolation<CompleteRegisterRequest>> violations = validator.validate(request);
            if (!violations.isEmpty()) {
                String messages = violations.stream()
                        .map(ConstraintViolation::getMessage)
                        .collect(Collectors.joining(" | "));
                return buildBadRequestResponse(messages, phone);
            }

            // إضافة الملفات إلى الطلب
            request.setProfileImageUrl(profileImage);
            request.setIdentityDocumentUrl(identityDocument);

            log.info("Completing registration for phone: {}", request.getPhone());
            CompleteRegisterResponse response = registrationService.completeRegistration(request, httpRequest);
            return buildCompleteSuccessResponse(response);

        } catch (IllegalArgumentException ex) {
            log.warn("Validation error: {}", ex.getMessage());
            return buildBadRequestResponse(ex.getMessage(), phone);
        } catch (ResourceAlreadyExistsException ex) {
            log.warn("Phone already exists during completion");
            return buildConflictResponse(ex.getMessage(), phone);
        } catch (Exception ex) {
            log.error("Error completing registration: {}", ex.getMessage());
            return buildErrorResponse("فشل في إتمام التسجيل", ex.getMessage(), phone);
        }
    }
    @PostMapping("/resend-otp")
    public ResponseEntity<?> resendOtp(@Valid @RequestBody ResendOtpRequest request) {
        try {
            log.info("Resending OTP for phone: {}", request.getPhone());
            ResendOtpResponse response = registrationService.resendOtp(request.getPhone());
            return buildSimpleSuccessResponse(response.getMessage(), request.getPhone());
        } catch (ResourceAlreadyExistsException ex) {
            log.warn("Phone already exists: {}", request.getPhone());
            return buildConflictResponse(ex.getMessage(), request.getPhone());
        } catch (Exception ex) {
            log.warn("Error resending OTP for {}: {}", request.getPhone(), ex.getMessage());
            return buildErrorResponse("فشل في إعادة إرسال رمز التحقق", ex.getMessage(), request.getPhone());
        }
    }

    @GetMapping("/check-phone/{phone}")
    public ResponseEntity<?> checkPhone(@PathVariable String phone) {
        try {
            PhoneCheckResponse response = registrationService.checkPhoneStatus(phone);
            log.info("Phone check response for {}: exists={}, message={}",
                    phone, response.isExists(), response.getMessage());
            log.info("Full response object: {}", response);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Phone check error: {}", e.getMessage(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("exists", false);
            errorResponse.put("message", "حدث خطأ في التحقق");
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.ok(errorResponse);
        }
    }

    @GetMapping("/application-info")
    public ResponseEntity<?> getApplicationInfo() {
        try {
            return ResponseEntity.ok(applicationInfoMapper.getApplicationInfo());
        } catch (Exception e) {
            log.error("Error getting application info: {}", e.getMessage());
            return buildErrorResponse("فشل في جلب معلومات التقديم", e.getMessage());
        }
    }

    /* =========================
       Helper Methods for Building Responses
       ========================= */

    // ✅ احتفظ بهذه الدالة فقط (المعدلة)
    private ResponseEntity<?> buildSuccessResponse(StartRegisterResponse response, String action) {
        Map<String, Object> successResponse = new HashMap<>();
        successResponse.put("success", true);
        successResponse.put("action", action);
        successResponse.put("message", response.getMessage());
        successResponse.put("phone", response.getPhone());

        // إضافة الـ otpCode إذا كان موجوداً (للتطوير فقط)
        if (response.getOtpCode() != null && !response.getOtpCode().isEmpty()) {
            successResponse.put("otpCode", response.getOtpCode());
        }

        return ResponseEntity.ok(successResponse);
    }

    private ResponseEntity<?> buildCompleteSuccessResponse(CompleteRegisterResponse response) {
        Map<String, Object> successResponse = new HashMap<>();
        successResponse.put("success", true);
        successResponse.put("message", response.getMessage());
        successResponse.put("phone", response.getPhone());
        successResponse.put("token", response.getToken());
        successResponse.put("studentCode", response.getStudentCode());
        successResponse.put("status", "PENDING");
        successResponse.put("redirectTo", "/pending");
        successResponse.put("supportWhatsApp", "+201234567890");
        return ResponseEntity.status(HttpStatus.CREATED).body(successResponse);
    }

    private ResponseEntity<?> buildSimpleSuccessResponse(String message, String phone) {
        Map<String, Object> successResponse = new HashMap<>();
        successResponse.put("success", true);
        successResponse.put("message", message);
        successResponse.put("phone", phone);
        return ResponseEntity.ok(successResponse);
    }

    private ResponseEntity<?> buildConflictResponse(String error, String phone) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("success", false);
        errorResponse.put("error", error);
        errorResponse.put("phone", phone);
        errorResponse.put("action", "LOGIN");
        errorResponse.put("loginUrl", "/api/auth/login");
        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
    }

    private ResponseEntity<?> buildBadRequestResponse(String message, String phone) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("success", false);
        errorResponse.put("error", "خطأ في البيانات");
        errorResponse.put("message", message);
        errorResponse.put("phone", phone);
        errorResponse.put("action", "FIX_DATA");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    private ResponseEntity<?> buildErrorResponse(String error, String message) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("success", false);
        errorResponse.put("error", error);
        errorResponse.put("message", message);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }

    private ResponseEntity<?> buildErrorResponse(String error, String message, String phone) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("success", false);
        errorResponse.put("error", error);
        errorResponse.put("message", message);
        errorResponse.put("phone", phone);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }

    private ResponseEntity<?> buildNotFoundResponse(String message, String detail) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("success", false);
        errorResponse.put("error", "غير موجود");
        errorResponse.put("message", message);
        errorResponse.put("detail", detail);
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }
}