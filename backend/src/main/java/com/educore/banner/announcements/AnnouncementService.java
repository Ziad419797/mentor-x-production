package com.educore.banner.announcements;

import com.educore.banner.announcements.dto.request.CreateAnnouncementRequest;
import com.educore.banner.announcements.dto.request.UpdateAnnouncementRequest;
import com.educore.banner.announcements.dto.response.AnnouncementResponse;
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

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AnnouncementService {

    private final AnnouncementRepository announcementRepository;

    // ================= CREATE =================
    @Caching(evict = {
            @CacheEvict(value = "announcements", allEntries = true),
            @CacheEvict(value = "announcements_active", allEntries = true)
    })
    public AnnouncementResponse createAnnouncement(CreateAnnouncementRequest request, JwtUserPrincipal principal) {
        log.info("Creating announcement: {}", request.getTitle());

        Announcement announcement = Announcement.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .announcementDate(request.getAnnouncementDate())
                .expiryDate(request.getExpiryDate())
                .active(true)
                .createdBy(principal != null ? principal.getUserId() : null)
                .build();

        Announcement saved = announcementRepository.save(announcement);
        log.info("Announcement created with id: {}", saved.getId());

        return toResponse(saved);
    }

    // ================= GET ALL (للمدرس) =================
    @Cacheable(value = "announcements", key = "#pageable.pageNumber + '-' + #pageable.sort")
    @Transactional(readOnly = true)
    public Page<AnnouncementResponse> getAllAnnouncements(Pageable pageable) {
        return announcementRepository.findAllByOrderByCreatedAtDesc(pageable)
                .map(this::toResponse);
    }

    // ================= GET ACTIVE (للطالب) - مع Pagination =================
    @Cacheable(value = "announcements_active", key = "#pageable.pageNumber + '-' + #pageable.sort")
    @Transactional(readOnly = true)
    public Page<AnnouncementResponse> getActiveAnnouncements(Pageable pageable) {
        return announcementRepository.findActiveAnnouncements(LocalDateTime.now(), pageable)
                .map(this::toResponse);
    }

    // ================= GET BY ID =================
    @Cacheable(value = "announcements", key = "#id")
    @Transactional(readOnly = true)
    public AnnouncementResponse getAnnouncementById(Long id) {
        Announcement announcement = announcementRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("الإعلان غير موجود: " + id));
        return toResponse(announcement);
    }

    // ================= UPDATE =================
    @Caching(evict = {
            @CacheEvict(value = "announcements", key = "#id"),
            @CacheEvict(value = "announcements", allEntries = true),
            @CacheEvict(value = "announcements_active", allEntries = true)
    })
    public AnnouncementResponse updateAnnouncement(Long id, UpdateAnnouncementRequest request) {
        Announcement announcement = announcementRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("الإعلان غير موجود: " + id));

        if (request.getTitle() != null) announcement.setTitle(request.getTitle());
        if (request.getDescription() != null) announcement.setDescription(request.getDescription());
        if (request.getAnnouncementDate() != null) announcement.setAnnouncementDate(request.getAnnouncementDate());
        if (request.getExpiryDate() != null) announcement.setExpiryDate(request.getExpiryDate());
        if (request.getActive() != null) announcement.setActive(request.getActive());

        Announcement updated = announcementRepository.save(announcement);
        return toResponse(updated);
    }

    // ================= DELETE =================
    @Caching(evict = {
            @CacheEvict(value = "announcements", key = "#id"),
            @CacheEvict(value = "announcements", allEntries = true),
            @CacheEvict(value = "announcements_active", allEntries = true)
    })
    public void deleteAnnouncement(Long id) {
        Announcement announcement = announcementRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("الإعلان غير موجود: " + id));
        announcementRepository.delete(announcement);
        log.info("Announcement {} permanently deleted", id);
    }

    // ================= TOGGLE STATUS =================
    @Caching(evict = {
            @CacheEvict(value = "announcements", key = "#id"),
            @CacheEvict(value = "announcements_active", allEntries = true)
    })
    public AnnouncementResponse toggleAnnouncementStatus(Long id) {
        Announcement announcement = announcementRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("الإعلان غير موجود: " + id));
        announcement.setActive(!announcement.getActive());
        return toResponse(announcementRepository.save(announcement));
    }

    // ================= PRIVATE HELPERS =================
    private AnnouncementResponse toResponse(Announcement announcement) {
        return AnnouncementResponse.builder()
                .id(announcement.getId())
                .title(announcement.getTitle())
                .description(announcement.getDescription())
                .announcementDate(announcement.getAnnouncementDate())
                .expiryDate(announcement.getExpiryDate())
                .active(announcement.isCurrentlyActive())
                .createdAt(announcement.getCreatedAt())
                .build();
    }
}