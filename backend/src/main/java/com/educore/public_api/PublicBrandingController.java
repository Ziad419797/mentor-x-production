package com.educore.public_api;

import com.educore.common.GlobalResponse;
import com.educore.teacher.Teacher;
import com.educore.teacher.TeacherRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * GET /api/public/branding  — no auth required
 * Returns teacher logo URLs for use on login / register pages.
 */
@RestController
@RequestMapping("/api/public")
@RequiredArgsConstructor
public class PublicBrandingController {

    private final TeacherRepository teacherRepository;

    @GetMapping("/branding")
    public ResponseEntity<GlobalResponse<Map<String, String>>> getBranding() {
        Teacher teacher = teacherRepository.findFirstByEnabledTrue().orElse(null);
        Map<String, String> data = new HashMap<>();
        if (teacher != null) {
            if (teacher.getLogoUrl()     != null) data.put("logoUrl",     teacher.getLogoUrl());
            if (teacher.getDarkLogoUrl() != null) data.put("darkLogoUrl", teacher.getDarkLogoUrl());
            if (teacher.getName()        != null) data.put("teacherName", teacher.getName());
        }
        return ResponseEntity.ok(GlobalResponse.success("ok", data));
    }
}
