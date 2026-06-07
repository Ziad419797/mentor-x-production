package com.educore.staff;

import com.educore.exception.ResourceAlreadyExistsException;
import com.educore.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * StaffService — منطق إدارة الموظفين.
 *
 * كل العمليات مقيّدة بـ teacherId — المدرس ما يشوفش
 * ولا يعدّلش إلا موظفيه هو.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StaffService {

    private final StaffRepository   staffRepository;
    private final PasswordEncoder   passwordEncoder;
    private final PermissionService permissionService;

    // ─────────────────────────────────────────────────────────────
    // Create
    // ─────────────────────────────────────────────────────────────

    @Transactional
    public StaffResponse createStaff(Long teacherId, StaffCreateRequest req) {
        if (staffRepository.existsByPhone(req.phone())) {
            throw new ResourceAlreadyExistsException("رقم الهاتف مستخدم بالفعل");
        }

        Staff staff = Staff.builder()
                .fullName(req.fullName())
                .phone(req.phone())
                .password(passwordEncoder.encode(req.password()))
                .teacherId(teacherId)
                .permissions(req.permissions() != null ? req.permissions() : new java.util.HashSet<>())
                .notes(req.notes())
                .active(true)
                .build();

        Staff saved = staffRepository.save(staff);
        log.info("Staff created — id={}, teacherId={}, phone={}", saved.getId(), teacherId, req.phone());
        return StaffResponse.from(saved);
    }

    // ─────────────────────────────────────────────────────────────
    // Read
    // ─────────────────────────────────────────────────────────────

    public List<StaffResponse> getStaffByTeacher(Long teacherId) {
        return staffRepository.findByTeacherIdOrderByCreatedAtDesc(teacherId)
                .stream()
                .map(StaffResponse::from)
                .collect(Collectors.toList());
    }

    public StaffResponse getStaffById(Long teacherId, Long staffId) {
        return StaffResponse.from(findOwnedStaff(teacherId, staffId));
    }

    /**
     * قائمة كل الصلاحيات المتاحة — بيرجع اسم الـ enum + الـ label العربي.
     * المدرس بيحتاجها عشان يعرض الـ checkboxes في الـ UI.
     */
    public List<PermissionInfo> getAllAvailablePermissions() {
        return Arrays.stream(StaffPermission.values())
                .map(p -> new PermissionInfo(p.name(), p.arabicLabel))
                .collect(Collectors.toList());
    }

    // ─────────────────────────────────────────────────────────────
    // Update — basic info
    // ─────────────────────────────────────────────────────────────

    @Transactional
    public StaffResponse updateStaff(Long teacherId, Long staffId, StaffUpdateRequest req) {
        Staff staff = findOwnedStaff(teacherId, staffId);

        if (req.fullName()    != null) staff.setFullName(req.fullName());
        if (req.notes()       != null) staff.setNotes(req.notes());
        if (req.active()      != null) staff.setActive(req.active());
        if (req.newPassword() != null && !req.newPassword().isBlank()) {
            staff.setPassword(passwordEncoder.encode(req.newPassword()));
        }

        Staff saved = staffRepository.save(staff);
        log.info("Staff updated — id={}, teacherId={}", staffId, teacherId);
        return StaffResponse.from(saved);
    }

    // ─────────────────────────────────────────────────────────────
    // Update — permissions only
    // ─────────────────────────────────────────────────────────────

    @Transactional
    public StaffResponse updatePermissions(Long teacherId, Long staffId, UpdatePermissionsRequest req) {
        Staff staff = findOwnedStaff(teacherId, staffId);

        staff.getPermissions().clear();
        staff.getPermissions().addAll(req.permissions());

        Staff saved = staffRepository.save(staff);

        // نحدث الـ cache عشان التغيير يطبّق فوراً
        permissionService.invalidateCache(staffId);

        log.info("Staff permissions updated — id={}, teacherId={}, permissions={}",
                staffId, teacherId, req.permissions());
        return StaffResponse.from(saved);
    }

    // ─────────────────────────────────────────────────────────────
    // Delete / Deactivate
    // ─────────────────────────────────────────────────────────────

    @Transactional
    public void deleteStaff(Long teacherId, Long staffId) {
        Staff staff = findOwnedStaff(teacherId, staffId);
        staffRepository.delete(staff);
        permissionService.invalidateCache(staffId);
        log.info("Staff deleted — id={}, teacherId={}", staffId, teacherId);
    }

    @Transactional
    public StaffResponse toggleActive(Long teacherId, Long staffId) {
        Staff staff = findOwnedStaff(teacherId, staffId);
        staff.setActive(!staff.isActive());
        Staff saved = staffRepository.save(staff);
        permissionService.invalidateCache(staffId);
        log.info("Staff toggled — id={}, active={}", staffId, saved.isActive());
        return StaffResponse.from(saved);
    }

    // ─────────────────────────────────────────────────────────────
    // Helper — ensure teacher owns this staff member
    // ─────────────────────────────────────────────────────────────

    private Staff findOwnedStaff(Long teacherId, Long staffId) {
        Staff staff = staffRepository.findById(staffId)
                .orElseThrow(() -> new ResourceNotFoundException("الموظف غير موجود"));

        if (!staff.getTeacherId().equals(teacherId)) {
            throw new ResourceNotFoundException("الموظف غير موجود");
        }

        return staff;
    }

    // ─────────────────────────────────────────────────────────────
    // Inner record — UI permission list
    // ─────────────────────────────────────────────────────────────

    public record PermissionInfo(String name, String arabicLabel) {}
}
