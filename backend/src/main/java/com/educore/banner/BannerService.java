package com.educore.banner;

import com.educore.banner.dto.request.CreateBannerRequest;
import com.educore.banner.dto.request.UpdateBannerRequest;
import com.educore.banner.dto.response.BannerResponse;
import com.educore.common.CacheNames;
import com.educore.common.FileUploadService;

import com.educore.exception.ResourceNotFoundException;
import com.educore.security.JwtUserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class BannerService {

    private final BannerRepository bannerRepository;
    private final FileUploadService fileUploadService;

    private static final String BANNER_FOLDER = "banners";

    // ================= CREATE =================
    @Caching(evict = {
            @CacheEvict(value = "banners", allEntries = true),
            @CacheEvict(value = "banners_active", allEntries = true)
    })
    public BannerResponse createBanner(CreateBannerRequest request, JwtUserPrincipal principal) {
        log.info("Creating banner: {}", request.getTitle());

        String imageUrl = null;
        if (request.getImageFile() != null && !request.getImageFile().isEmpty()) {
            imageUrl = fileUploadService.uploadImage(request.getImageFile(), BANNER_FOLDER);
        }

        Banner banner = Banner.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .imageUrl(imageUrl)
                .linkUrl(request.getLinkUrl())
                .displayOrder(request.getDisplayOrder() != null ? request.getDisplayOrder() : 0)
                .active(true)
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .createdBy(principal.getUserId())
                .build();

        Banner saved = bannerRepository.save(banner);
        log.info("Banner created with id: {}", saved.getId());

        return toResponse(saved);
    }

    // ================= GET ALL (للمدرس - مع Pagination) =================
    @Cacheable(value = "banners", key = "#pageable.pageNumber")
    @Transactional(readOnly = true)
    public Page<BannerResponse> getAllBanners(Pageable pageable) {
        return bannerRepository.findAllByOrderByDisplayOrderAscCreatedAtDesc(pageable)
                .map(this::toResponse);
    }

    // ================= GET ACTIVE (للطالب) =================
    @Cacheable(value = "banners_active", key = "#pageable.pageNumber + '-' + #pageable.sort")
    @Transactional(readOnly = true)
    public Page<BannerResponse> getActiveBanners(Pageable pageable) {
        return bannerRepository.findActiveBanners(LocalDateTime.now(), pageable)
                .map(this::toResponse);
    }

    // ================= GET BY ID =================
    @Cacheable(value = "banners", key = "#id")
    @Transactional(readOnly = true)
    public BannerResponse getBannerById(Long id) {
        Banner banner = bannerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("البانر غير موجود: " + id));
        return toResponse(banner);
    }

    // ================= UPDATE =================
    @Caching(evict = {
            @CacheEvict(value = "banners", key = "#id"),
            @CacheEvict(value = "banners", allEntries = true),
            @CacheEvict(value = "banners_active", allEntries = true)
    })
    public BannerResponse updateBanner(Long id, UpdateBannerRequest request) {
        Banner banner = bannerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("البانر غير موجود: " + id));

        if (request.getTitle() != null) banner.setTitle(request.getTitle());
        if (request.getDescription() != null) banner.setDescription(request.getDescription());
        if (request.getLinkUrl() != null) banner.setLinkUrl(request.getLinkUrl());
        if (request.getDisplayOrder() != null) banner.setDisplayOrder(request.getDisplayOrder());
        if (request.getStartDate() != null) banner.setStartDate(request.getStartDate());
        if (request.getEndDate() != null) banner.setEndDate(request.getEndDate());
        if (request.getActive() != null) banner.setActive(request.getActive());

        // حذف الصورة إذا طلب
        if (request.isRemoveImage() && banner.getImageUrl() != null) {
            fileUploadService.deleteFile(banner.getImageUrl());
            banner.setImageUrl(null);
        }

        // رفع صورة جديدة
        if (request.getImageFile() != null && !request.getImageFile().isEmpty()) {
            if (banner.getImageUrl() != null) {
                fileUploadService.deleteFile(banner.getImageUrl());
            }
            banner.setImageUrl(fileUploadService.uploadImage(request.getImageFile(), BANNER_FOLDER));
        }

        Banner updated = bannerRepository.save(banner);
        return toResponse(updated);
    }

    // ================= DELETE =================
    @Caching(evict = {
            @CacheEvict(value = "banners", key = "#id"),
            @CacheEvict(value = "banners", allEntries = true),
            @CacheEvict(value = "banners_active", allEntries = true)
    })
    public void deleteBanner(Long id) {
        Banner banner = bannerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("البانر غير موجود: " + id));

        if (banner.getImageUrl() != null) {
            fileUploadService.deleteFile(banner.getImageUrl());
        }

        bannerRepository.delete(banner);
        log.info("Banner {} permanently deleted", id);
    }

    // ================= TOGGLE STATUS =================
    @Caching(evict = {
            @CacheEvict(value = "banners", key = "#id"),
            @CacheEvict(value = "banners_active", allEntries = true)
    })
    public BannerResponse toggleBannerStatus(Long id) {
        Banner banner = bannerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("البانر غير موجود: " + id));

        banner.setActive(!banner.getActive());
        return toResponse(bannerRepository.save(banner));
    }

    // ================= PRIVATE HELPERS =================
    private BannerResponse toResponse(Banner banner) {
        return BannerResponse.builder()
                .id(banner.getId())
                .title(banner.getTitle())
                .description(banner.getDescription())
                .imageUrl(banner.getImageUrl())
                .linkUrl(banner.getLinkUrl())
                .displayOrder(banner.getDisplayOrder())
                .active(banner.isCurrentlyActive())  // ✅ استخدام isCurrentlyActive()
                .startDate(banner.getStartDate())
                .endDate(banner.getEndDate())
                .createdAt(banner.getCreatedAt())
                .build();
    }
}