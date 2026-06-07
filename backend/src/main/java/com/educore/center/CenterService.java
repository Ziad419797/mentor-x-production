package com.educore.center;

import com.educore.center.dto.CenterRequest;
import com.educore.center.dto.CenterResponse;
import com.educore.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CenterService {

    private final CenterRepository centerRepository;

    // ─── Public: list all active centers ─────────────────────────

    @Transactional(readOnly = true)
    public List<CenterResponse> getAllActive() {
        return centerRepository.findByActiveTrueOrderByGovernorateAscNameAsc()
                .stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<CenterResponse> getByGovernorate(String governorate) {
        return centerRepository.findByGovernorateAndActiveTrueOrderByNameAsc(governorate)
                .stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public CenterResponse getById(Long id) {
        return toResponse(findOrThrow(id));
    }

    // ─── Admin/Teacher management ─────────────────────────────────

    @Transactional(readOnly = true)
    public List<CenterResponse> getAll() {
        return centerRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional
    public CenterResponse create(CenterRequest request, String createdBy) {
        Center center = Center.builder()
                .name(request.getName())
                .governorate(request.getGovernorate())
                .area(request.getArea())
                .address(request.getAddress())
                .phone(request.getPhone())
                .active(request.isActive())
                .sellsBooks(request.isSellsBooks())
                .sellsCodes(request.isSellsCodes())
                .mapsLink(request.getMapsLink())
                .whatsappGroupLink(request.getWhatsappGroupLink())
                .instagramLink(request.getInstagramLink())
                .facebookLink(request.getFacebookLink())
                .telegramLink(request.getTelegramLink())
                .youtubeLink(request.getYoutubeLink())
                .tiktokLink(request.getTiktokLink())
                .createdBy(createdBy)
                .build();
        Center saved = centerRepository.save(center);
        log.info("Center created: {} by {}", saved.getName(), createdBy);
        return toResponse(saved);
    }

    @Transactional
    public CenterResponse update(Long id, CenterRequest request) {
        Center center = findOrThrow(id);
        center.setName(request.getName());
        center.setGovernorate(request.getGovernorate());
        center.setArea(request.getArea());
        center.setAddress(request.getAddress());
        center.setPhone(request.getPhone());
        center.setActive(request.isActive());
        center.setSellsBooks(request.isSellsBooks());
        center.setSellsCodes(request.isSellsCodes());
        center.setMapsLink(request.getMapsLink());
        center.setWhatsappGroupLink(request.getWhatsappGroupLink());
        center.setInstagramLink(request.getInstagramLink());
        center.setFacebookLink(request.getFacebookLink());
        center.setTelegramLink(request.getTelegramLink());
        center.setYoutubeLink(request.getYoutubeLink());
        center.setTiktokLink(request.getTiktokLink());
        return toResponse(centerRepository.save(center));
    }

    @Transactional
    public void delete(Long id) {
        Center center = findOrThrow(id);
        // Soft-delete: just deactivate instead of hard delete
        center.setActive(false);
        centerRepository.save(center);
        log.info("Center {} deactivated", id);
    }

    // ─── Helpers ─────────────────────────────────────────────────

    Center findOrThrow(Long id) {
        return centerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("السنتر غير موجود: " + id));
    }

    /**
     * Lookup by name — used by student registration to resolve their center.
     * Returns null if not found (registration allows free-text centerName).
     */
    @Transactional(readOnly = true)
    public CenterResponse findByName(String name) {
        return centerRepository.findByNameIgnoreCase(name)
                .map(this::toResponse)
                .orElse(null);
    }

    CenterResponse toResponse(Center c) {
        return CenterResponse.builder()
                .id(c.getId())
                .name(c.getName())
                .governorate(c.getGovernorate())
                .area(c.getArea())
                .address(c.getAddress())
                .phone(c.getPhone())
                .active(c.isActive())
                .sellsBooks(c.isSellsBooks())
                .sellsCodes(c.isSellsCodes())
                .mapsLink(c.getMapsLink())
                .whatsappGroupLink(c.getWhatsappGroupLink())
                .instagramLink(c.getInstagramLink())
                .facebookLink(c.getFacebookLink())
                .telegramLink(c.getTelegramLink())
                .youtubeLink(c.getYoutubeLink())
                .tiktokLink(c.getTiktokLink())
                .createdAt(c.getCreatedAt())
                .build();
    }
}
