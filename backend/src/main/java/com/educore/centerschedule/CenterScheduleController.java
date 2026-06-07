package com.educore.centerschedule;

import com.educore.centerschedule.dto.CenterScheduleRequest;
import com.educore.common.GlobalResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/center-schedule")
@RequiredArgsConstructor
public class CenterScheduleController {

    private final CenterScheduleRepository repo;

    @GetMapping
    public ResponseEntity<GlobalResponse<List<CenterSchedule>>> getAll() {
        return ResponseEntity.ok(GlobalResponse.success(repo.findByActiveTrueOrderByDayOfWeekAscStartTimeAsc()));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public ResponseEntity<GlobalResponse<CenterSchedule>> create(@RequestBody CenterScheduleRequest req) {
        CenterSchedule entry = CenterSchedule.builder()
            .centerName(req.getCenterName())
            .groupName(req.getGroupName())
            .gradeLevel(req.getGradeLevel())
            .dayOfWeek(req.getDayOfWeek())
            .startTime(req.getStartTime())
            .endTime(req.getEndTime())
            .notes(req.getNotes())
            .active(true)
            .build();
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(GlobalResponse.success("تم الإضافة", repo.save(entry)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public ResponseEntity<GlobalResponse<CenterSchedule>> update(@PathVariable Long id, @RequestBody CenterScheduleRequest req) {
        CenterSchedule entry = repo.findById(id).orElseThrow();
        entry.setCenterName(req.getCenterName());
        entry.setGroupName(req.getGroupName());
        entry.setGradeLevel(req.getGradeLevel());
        entry.setDayOfWeek(req.getDayOfWeek());
        entry.setStartTime(req.getStartTime());
        entry.setEndTime(req.getEndTime());
        entry.setNotes(req.getNotes());
        return ResponseEntity.ok(GlobalResponse.success("تم التحديث", repo.save(entry)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public ResponseEntity<GlobalResponse<Void>> delete(@PathVariable Long id) {
        CenterSchedule entry = repo.findById(id).orElseThrow();
        entry.setActive(false);
        repo.save(entry);
        return ResponseEntity.ok(GlobalResponse.success("تم الحذف", null));
    }
}
