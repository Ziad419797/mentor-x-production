package com.educore.lessonmaterial;

import com.educore.ai.AiClient;
import com.educore.common.CacheNames;
import com.educore.common.SortFields;
import com.educore.common.SortValidator;
import com.educore.dtocourse.request.LessonMaterialCreateRequest;
import com.educore.dtocourse.request.LessonMaterialUpdateRequest;
import com.educore.dtocourse.request.ReorderRequest;
import com.educore.dtocourse.response.LessonMaterialResponse;
import com.educore.exception.ResourceNotFoundException;
import com.educore.lesson.LessonRepository;
import com.educore.dtocourse.mapper.LessonMaterialMapper;
import com.educore.lesson.Week;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@CacheConfig(cacheNames = CacheNames.MATERIALS)
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class LessonMaterialService {

    private final LessonMaterialRepository materialRepository;
    private final AiClient aiClient;
    private final LessonRepository lessonRepository;
    private final LessonMaterialMapper materialMapper;
    private final SortValidator sortValidator;

    // ================= CREATE =================
    @CacheEvict(value = {
            CacheNames.MATERIALS,
            CacheNames.MATERIALS_PAGES,
            CacheNames.LESSONS,        // ⚠️ لأن المواد تظهر في الدروس
            CacheNames.LESSONS_PAGES,
            CacheNames.LESSONS_BY_SESSION
    }, allEntries = true)
    public LessonMaterialResponse createMaterial(LessonMaterialCreateRequest request) {
        log.info("Creating material '{}' for weeks {}", request.getFileName(), request.getWeekIds());

        var weeksList = lessonRepository.findAllById(request.getWeekIds());
        if (weeksList.isEmpty()) {
            throw new ResourceNotFoundException("No valid weeks found for ids " + request.getWeekIds());
        }

        // ── Auto-detect YouTube URLs ──────────────────────────────
        // لو المدرس حط YouTube URL، نتحقق ونضبط النوع والـ videoId تلقائياً
        String fileUrl = request.getFileUrl();
        String youtubeVideoId = YoutubeUrlUtil.extractVideoId(fileUrl);

        if (youtubeVideoId != null) {
            // لينك يوتيوب → نضبط النوع تلقائياً لو المدرس ما اختارش YOUTUBE
            request.setMaterialType(MaterialType.YOUTUBE);
            log.info("YouTube URL detected — videoId: {}", youtubeVideoId);
        } else if (request.getMaterialType() == MaterialType.YOUTUBE) {
            // المدرس اختار YOUTUBE لكن الـ URL مش YouTube
            throw new IllegalArgumentException(
                "الـ URL المُدخل ليس رابط YouTube صحيح. " +
                "مثال صحيح: https://www.youtube.com/watch?v=VIDEO_ID"
            );
        }

        var material = materialMapper.toEntity(request);

        // نحفظ الـ videoId في الـ entity
        if (youtubeVideoId != null) {
            material.setYoutubeVideoId(youtubeVideoId);
        }

        for (Week week : weeksList) {
            material.getWeeks().add(week);
            week.getMaterials().add(material);
        }

        materialRepository.save(material);

        // ── AI Ingest (async — لا يعطّل الـ response) ─────────
        if (material.getMaterialType() != null && material.getMaterialType().isDocument()) {
            triggerIngest(material.getId(), material.getFileUrl(), material.getFileName());
        }

        return materialMapper.toResponse(material);
    }

    @Async
    protected void triggerIngest(Long materialId, String fileUrl, String fileName) {
        try {
            // Ensure fileName has a proper extension — Python's document_processor uses it.
            // Special cases:
            //   • Google Drive sharing links have no file extension in the URL path → use .pdf
            //   • Other URLs: read extension from the last path segment only (not the whole URL)
            String effectiveName = fileName;
            if (fileName != null && !fileName.contains(".") && fileUrl != null) {
                String ext = null;

                // Google Drive: /file/d/FILE_ID/view — always treat as PDF
                if (fileUrl.contains("drive.google.com")) {
                    ext = ".pdf";
                } else {
                    // Generic fallback: look at the URL's last path segment (before any query string)
                    String urlPath = fileUrl.split("\\?")[0];
                    String lastSegment = urlPath.substring(urlPath.lastIndexOf('/') + 1);
                    int dotIdx = lastSegment.lastIndexOf('.');
                    if (dotIdx > 0) {
                        ext = lastSegment.substring(dotIdx).toLowerCase();
                    }
                }

                if (ext != null) {
                    effectiveName = fileName + ext;
                }
            }
            log.info("Triggering AI ingest for materialId={} fileName={}", materialId, effectiveName);
            aiClient.ingestMaterial(fileUrl, effectiveName, materialId, null);
        } catch (Exception e) {
            log.warn("AI ingest trigger failed for materialId={}: {}", materialId, e.getMessage());
        }
    }

    // ================= UPDATE =================
    @CacheEvict(value = {
            CacheNames.MATERIALS,
            CacheNames.MATERIALS_PAGES,
            CacheNames.LESSONS,        // ⚠️ لأن المواد تظهر في الدروس
            CacheNames.LESSONS_PAGES,
            CacheNames.LESSONS_BY_SESSION
    }, allEntries = true)
    public LessonMaterialResponse updateMaterial(Long id, LessonMaterialUpdateRequest request) {
        log.info("Updating material id {}", id);

        var material = materialRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Material not found with id " + id));

        materialMapper.updateEntityFromRequest(request, material);

        return materialMapper.toResponse(material);
    }

    // ================= DELETE =================

    @CacheEvict(value = {
            CacheNames.MATERIALS,
            CacheNames.MATERIALS_PAGES,
            CacheNames.LESSONS,        // ⚠️ لأن المواد تظهر في الدروس
            CacheNames.LESSONS_PAGES,
            CacheNames.LESSONS_BY_SESSION
    }, allEntries = true)
    public void deleteMaterial(Long id) {
        log.info("Deleting material id {}", id);

        var material = materialRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Material not found with id " + id));

        material.getWeeks().forEach(week -> week.getMaterials().remove(material));

        Long materialId = material.getId();
        boolean wasDocument = material.getMaterialType() != null && material.getMaterialType().isDocument();

        materialRepository.delete(material);

        // ── Remove from AI vector store (async) ───────────────
        if (wasDocument) {
            try {
                aiClient.deleteIngested(materialId);
            } catch (Exception e) {
                log.warn("AI delete ingested failed for materialId={}: {}", materialId, e.getMessage());
            }
        }
    }

    // ================= GET BY ID =================
    @Transactional(readOnly = true)
    @Cacheable(value = CacheNames.MATERIALS, key = "#id")
    public LessonMaterialResponse getMaterialById(Long id) {
        log.info("Fetching material id {}", id);

        var material = materialRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Material not found with id " + id));

        return materialMapper.toResponse(material);
    }

    // ================= GET BY WEEK =================
    @Transactional(readOnly = true)
    @Cacheable(value = CacheNames.MATERIALS_PAGES, key = "'week-' + #weekId + '-' + #pageable.pageNumber")
    public Page<LessonMaterialResponse> getMaterialsByWeek(Long weekId, Pageable pageable) {
        sortValidator.validate(pageable, SortFields.LESSONMaterial);

        if (!lessonRepository.existsById(weekId)) {
            throw new ResourceNotFoundException("Week not found with id " + weekId);
        }

        return materialRepository.findByWeeksId(weekId, pageable)
                .map(materialMapper::toResponse);
    }

    @Transactional
    @CacheEvict(value = {
            CacheNames.MATERIALS,
            CacheNames.MATERIALS_PAGES,
            CacheNames.LESSONS,        // ⚠️ لأن المواد تظهر في الدروس
            CacheNames.LESSONS_PAGES,
            CacheNames.LESSONS_BY_SESSION
    }, allEntries = true)
    public void toggleMaterialStatus(Long id) {
        var material = materialRepository.findByIdIncludingInactive(id)
                .orElseThrow(() -> new ResourceNotFoundException("Material not found"));

        material.setActive(!material.isActive());
        materialRepository.save(material);
    }

    @CacheEvict(cacheNames = {
            CacheNames.MATERIALS,
            CacheNames.MATERIALS_PAGES
    }, allEntries = true)
    public void reorderMaterials(ReorderRequest request) {
        if (request.getItems() == null) return;
        for (ReorderRequest.Item item : request.getItems()) {
            materialRepository.findById(item.getId()).ifPresent(m -> {
                m.setOrderNumber(item.getOrderNumber());
                materialRepository.save(m);
            });
        }
    }
}
