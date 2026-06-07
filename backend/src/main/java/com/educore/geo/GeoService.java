package com.educore.geo;

import com.educore.geo.dto.AreaDto;
import com.educore.geo.dto.GovernorateDto;
import com.educore.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GeoService {

    private final GovernorateRepository governorateRepository;
    private final AreaRepository        areaRepository;

    // ─── Governorates ─────────────────────────────────────────────

    /** قائمة المحافظات بدون مناطق — للـ dropdown الأول */
    @Transactional(readOnly = true)
    public List<GovernorateDto> getAllGovernorates() {
        return governorateRepository.findAllByOrderByDisplayOrderAscNameArAsc()
                .stream()
                .map(g -> GovernorateDto.builder()
                        .id(g.getId())
                        .nameAr(g.getNameAr())
                        .nameEn(g.getNameEn())
                        .displayOrder(g.getDisplayOrder())
                        .build())
                .toList();
    }

    /** قائمة المحافظات مع مناطقها (كل شيء في طلب واحد) */
    @Transactional(readOnly = true)
    public List<GovernorateDto> getAllWithAreas() {
        return governorateRepository.findAllByOrderByDisplayOrderAscNameArAsc()
                .stream()
                .map(g -> GovernorateDto.builder()
                        .id(g.getId())
                        .nameAr(g.getNameAr())
                        .nameEn(g.getNameEn())
                        .displayOrder(g.getDisplayOrder())
                        .areas(g.getAreas().stream().map(this::toAreaDto).toList())
                        .build())
                .toList();
    }

    // ─── Areas by governorate ──────────────────────────────────────

    /** مناطق محافظة معينة (حسب الـ ID) */
    @Transactional(readOnly = true)
    public List<AreaDto> getAreasByGovernorateId(Long governorateId) {
        if (!governorateRepository.existsById(governorateId)) {
            throw new ResourceNotFoundException("المحافظة غير موجودة: " + governorateId);
        }
        return areaRepository
                .findByGovernorateIdOrderByDisplayOrderAscNameAsc(governorateId)
                .stream().map(this::toAreaDto).toList();
    }

    /** مناطق محافظة معينة (حسب الاسم العربي) */
    @Transactional(readOnly = true)
    public List<AreaDto> getAreasByGovernorateName(String nameAr) {
        return areaRepository
                .findByGovernorateNameArIgnoreCaseOrderByDisplayOrderAscNameAsc(nameAr)
                .stream().map(this::toAreaDto).toList();
    }

    // ─── Admin: bulk seed ─────────────────────────────────────────

    /**
     * يستورد مناطق لمحافظة معينة بالاسم.
     * لو المحافظة مش موجودة بتتنشأ تلقائياً.
     * الاستخدام: يُستدعى من DataLoader أو Admin API بعد ما الـ user يبعت الفايل.
     */
    @Transactional
    public void seedAreas(String governorateNameAr, String governorateNameEn,
                          int govDisplayOrder, List<String> areaNames) {
        Governorate gov = governorateRepository.findByNameArIgnoreCase(governorateNameAr)
                .orElseGet(() -> governorateRepository.save(
                        Governorate.builder()
                                .nameAr(governorateNameAr)
                                .nameEn(governorateNameEn)
                                .displayOrder(govDisplayOrder)
                                .build()
                ));

        int order = 1;
        for (String areaName : areaNames) {
            String trimmed = areaName.trim();
            if (trimmed.isBlank()) continue;
            // تجنب الإضافة المكررة
            boolean exists = gov.getAreas().stream()
                    .anyMatch(a -> a.getName().equalsIgnoreCase(trimmed));
            if (!exists) {
                Area area = Area.builder()
                        .name(trimmed)
                        .governorate(gov)
                        .displayOrder(order++)
                        .build();
                gov.getAreas().add(area);
            }
        }
        governorateRepository.save(gov);
    }

    // ─── Helpers ──────────────────────────────────────────────────

    private AreaDto toAreaDto(Area a) {
        return AreaDto.builder()
                .id(a.getId())
                .name(a.getName())
                .displayOrder(a.getDisplayOrder())
                .build();
    }
}
