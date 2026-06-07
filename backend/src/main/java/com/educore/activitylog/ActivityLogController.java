package com.educore.activitylog;

import com.educore.common.GlobalResponse;
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
            @PageableDefault(size = 30, sort = "createdAt") Pageable pageable) {

        Page<ActivityLog> page = (actor != null && !actor.isBlank())
                ? service.getByActor(actor, pageable)
                : service.getAll(pageable);

        return ResponseEntity.ok(GlobalResponse.success(page));
    }
}
