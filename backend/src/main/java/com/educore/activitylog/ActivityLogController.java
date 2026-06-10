package com.educore.activitylog;

import com.educore.common.GlobalResponse;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/activity-logs")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
public class ActivityLogController {

    private final ActivityLogService service;

    @GetMapping
    public ResponseEntity<GlobalResponse<Page<ActivityLog>>> getAll(
            @RequestParam(required = false) String actor,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @PageableDefault(size = 30, sort = "createdAt") Pageable pageable) {

        LocalDateTime fromDt = (from != null && !from.isBlank()) ? LocalDate.parse(from).atStartOfDay() : null;
        LocalDateTime toDt   = (to   != null && !to.isBlank())   ? LocalDate.parse(to).atTime(23, 59, 59) : null;
        Page<ActivityLog> page = service.search(actor, action, fromDt, toDt, pageable);
        return ResponseEntity.ok(GlobalResponse.success(page));
    }
}
