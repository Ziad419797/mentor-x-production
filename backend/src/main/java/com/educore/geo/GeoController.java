package com.educore.geo;

import com.educore.common.GlobalResponse;
import com.educore.geo.dto.AreaDto;
import com.educore.geo.dto.GovernorateDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Geographic Lookup API (Public — no auth required)
 *
 * GET /api/public/governorates                    → قائمة المحافظات
 * GET /api/public/governorates/with-areas         → المحافظات + مناطقها دفعة واحدة
 * GET /api/public/governorates/{id}/areas         → مناطق محافظة بالـ ID
 * GET /api/public/governorates/by-name/{name}/areas → مناطق محافظة بالاسم العربي
 *
 * Admin:
 * POST /api/admin/geo/seed                        → استيراد بيانات دفعي
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "Geo Lookup", description = "محافظات ومناطق — للتسجيل والفلترة")
public class GeoController {

    private final GeoService geoService;

    // ─── Public ───────────────────────────────────────────────────

    @Operation(summary = "قائمة المحافظات")
    @GetMapping("/api/public/governorates")
    public ResponseEntity<GlobalResponse<List<GovernorateDto>>> governorates() {
        return ResponseEntity.ok(GlobalResponse.success(geoService.getAllGovernorates()));
    }

    @Operation(summary = "المحافظات مع كل مناطقها — طلب واحد بدل اتنين")
    @GetMapping("/api/public/governorates/with-areas")
    public ResponseEntity<GlobalResponse<List<GovernorateDto>>> governoratesWithAreas() {
        return ResponseEntity.ok(GlobalResponse.success(geoService.getAllWithAreas()));
    }

    @Operation(summary = "مناطق محافظة بالـ ID")
    @GetMapping("/api/public/governorates/{id}/areas")
    public ResponseEntity<GlobalResponse<List<AreaDto>>> areasByGovId(@PathVariable Long id) {
        return ResponseEntity.ok(GlobalResponse.success(geoService.getAreasByGovernorateId(id)));
    }

    @Operation(summary = "مناطق محافظة بالاسم العربي")
    @GetMapping("/api/public/governorates/by-name/{nameAr}/areas")
    public ResponseEntity<GlobalResponse<List<AreaDto>>> areasByGovName(@PathVariable String nameAr) {
        return ResponseEntity.ok(GlobalResponse.success(geoService.getAreasByGovernorateName(nameAr)));
    }

    // ─── Admin: seed data ─────────────────────────────────────────

    @Operation(summary = "استيراد مناطق دفعي لمحافظة — ADMIN فقط")
    @PostMapping("/api/admin/geo/seed")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<GlobalResponse<Void>> seed(@RequestBody SeedRequest request) {
        geoService.seedAreas(
                request.getGovernorateNameAr(),
                request.getGovernorateNameEn(),
                request.getDisplayOrder() != null ? request.getDisplayOrder() : 99,
                request.getAreas()
        );
        return ResponseEntity.ok(GlobalResponse.success(
                "تم استيراد " + request.getAreas().size() + " منطقة لـ " + request.getGovernorateNameAr(), null));
    }

    // ─── Nested request DTO ───────────────────────────────────────

    @lombok.Data
    public static class SeedRequest {
        private String governorateNameAr;
        private String governorateNameEn;
        private Integer displayOrder;
        private List<String> areas;
    }
}
