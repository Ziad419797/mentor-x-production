package com.educore.analytics;

import com.educore.common.GlobalResponse;
import com.educore.exception.ResourceNotFoundException;
import com.educore.parent.Parent;
import com.educore.parent.ParentRepository;
import com.educore.security.JwtUserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/analytics/parent")
@PreAuthorize("hasRole('PARENT')")
@RequiredArgsConstructor
public class ParentAnalyticsController {

    private final AnalyticsRepository       repo;
    private final StudentAnalyticsController student;
    private final ParentRepository          parentRepo;

    /** All child analytics bundled — one call */
    @GetMapping("/child/{studentId}/all")
    public ResponseEntity<GlobalResponse<Map<String, Object>>> all(
            @AuthenticationPrincipal JwtUserPrincipal p,
            @PathVariable Long studentId) {

        validateOwnership(p.getUserId(), studentId);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("vsAvg",            student.buildVsAvg(studentId));
        data.put("progressOverTime", student.buildProgress(studentId));
        data.put("loginHours",       student.buildLoginHours(studentId));
        data.put("achievements",     student.buildAchievements(studentId));
        data.put("streak",           student.buildStreak(studentId));
        data.put("quizSpeed",        student.buildQuizSpeed(studentId));
        data.put("attemptsToPass",   student.buildAttemptsToPass(studentId));
        return ok(data);
    }

    private void validateOwnership(Long parentId, Long studentId) {
        Parent parent = parentRepo.findById(parentId)
            .orElseThrow(() -> new ResourceNotFoundException("ولي الأمر غير موجود"));
        parent.getStudents().stream()
            .filter(s -> s.getId().equals(studentId))
            .findFirst()
            .orElseThrow(() -> new SecurityException("هذا الطالب غير مرتبط بحسابك"));
    }

    private <T> ResponseEntity<GlobalResponse<T>> ok(T data) {
        return ResponseEntity.ok(GlobalResponse.success("ok", data));
    }
}
