package com.educore.analytics;

import com.educore.common.GlobalResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;

@RestController
@RequestMapping("/api/analytics/teacher")
@PreAuthorize("hasAnyRole('TEACHER','ADMIN','STAFF')")
@RequiredArgsConstructor
public class TeacherAnalyticsController {

    private final AnalyticsRepository repo;

    // ── Heatmap: student locations ──────────────────────────────
    @GetMapping("/student-locations")
    public ResponseEntity<GlobalResponse<List<Map<String, Object>>>> studentLocations() {
        List<Map<String, Object>> data = repo.studentCountByGovernorate().stream().map(row -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("governorate", row[0]);
            m.put("count", row[1]);
            return m;
        }).toList();
        return ok(data);
    }

    // ── Purchase hours ──────────────────────────────────────────
    @GetMapping("/purchase-hours")
    public ResponseEntity<GlobalResponse<List<Map<String, Object>>>> purchaseHours() {
        int[] counts = new int[24];
        for (Object[] row : repo.purchaseCountByHour())
            counts[((Number) row[0]).intValue()] = ((Number) row[1]).intValue();
        List<Map<String, Object>> data = new ArrayList<>();
        for (int h = 0; h < 24; h++) {
            data.add(Map.of("hour", h, "count", counts[h]));
        }
        return ok(data);
    }

    // ── Purchase days ───────────────────────────────────────────
    @GetMapping("/purchase-days")
    public ResponseEntity<GlobalResponse<List<Map<String, Object>>>> purchaseDays() {
        // DAYOFWEEK: 1=Sun, 2=Mon ... 7=Sat
        String[] arabic = {"الأحد","الاثنين","الثلاثاء","الأربعاء","الخميس","الجمعة","السبت"};
        int[] counts = new int[8]; // index 1–7
        for (Object[] row : repo.purchaseCountByDayOfWeek())
            counts[((Number) row[0]).intValue()] = ((Number) row[1]).intValue();
        List<Map<String, Object>> data = new ArrayList<>();
        for (int d = 1; d <= 7; d++) {
            data.add(Map.of("day", arabic[d - 1], "dayIndex", d, "count", counts[d]));
        }
        return ok(data);
    }

    // ── Sales by product type ───────────────────────────────────
    @GetMapping("/sales-by-type")
    public ResponseEntity<GlobalResponse<List<Map<String, Object>>>> salesByType() {
        List<Map<String, Object>> data = repo.salesByProductType().stream().map(row -> {
            Map<String, Object> m = new LinkedHashMap<>();
            String type = row[0] != null ? row[0].toString() : "OTHER";
            m.put("type", type);
            m.put("label", switch (type) {
                case "COURSE" -> "حصة فردية";
                case "CATEGORY" -> "باقة";
                default -> type;
            });
            m.put("count", row[1]);
            return m;
        }).toList();
        return ok(data);
    }

    // ── Login heatmap ───────────────────────────────────────────
    @GetMapping("/login-heatmap")
    public ResponseEntity<GlobalResponse<List<Map<String, Object>>>> loginHeatmap() {
        List<Map<String, Object>> data = repo.studentLoginHeatmap().stream().map(row -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("hour", row[0]);
            m.put("dayOfWeek", row[1]);
            m.put("count", row[2]);
            return m;
        }).toList();
        return ok(data);
    }

    // ── Avg scores by course ────────────────────────────────────
    @GetMapping("/avg-score-by-course")
    public ResponseEntity<GlobalResponse<List<Map<String, Object>>>> avgScoreByCourse() {
        List<Map<String, Object>> data = repo.avgScoreByCourse().stream().map(row -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("course", row[0]);
            m.put("avgScore", row[1] != null ? Math.round(((Number) row[1]).doubleValue() * 10.0) / 10.0 : 0);
            return m;
        }).toList();
        return ok(data);
    }

    // ── Avg scores by center ────────────────────────────────────
    @GetMapping("/avg-score-by-center")
    public ResponseEntity<GlobalResponse<List<Map<String, Object>>>> avgScoreByCenter() {
        List<Map<String, Object>> data = repo.avgScoreByCenter().stream().map(row -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("center", row[0]);
            m.put("avgScore", row[1] != null ? Math.round(((Number) row[1]).doubleValue() * 10.0) / 10.0 : 0);
            return m;
        }).toList();
        return ok(data);
    }

    // ── Hardest topics ──────────────────────────────────────────
    @GetMapping("/hardest-topics")
    public ResponseEntity<GlobalResponse<List<Map<String, Object>>>> hardestTopics() {
        List<Map<String, Object>> data = repo.hardestTopics().stream().map(row -> {
            long total = ((Number) row[1]).longValue();
            long wrong = ((Number) row[2]).longValue();
            double errorRate = total > 0 ? Math.round((wrong * 100.0 / total) * 10.0) / 10.0 : 0;
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("topic", row[0]);
            m.put("totalAnswers", total);
            m.put("wrongAnswers", wrong);
            m.put("errorRate", errorRate);
            return m;
        }).toList();
        return ok(data);
    }

    // ── Avg platform time ───────────────────────────────────────
    @GetMapping("/avg-platform-time")
    public ResponseEntity<GlobalResponse<Map<String, Object>>> avgPlatformTime() {
        double secs = repo.avgPlatformTimeSeconds();
        return ok(Map.of(
            "avgSeconds", (long) secs,
            "avgMinutes", (long) (secs / 60),
            "avgHours",   Math.round(secs / 3600.0 * 10.0) / 10.0
        ));
    }

    // ── Avg attempts to pass ────────────────────────────────────
    @GetMapping("/avg-attempts-to-pass")
    public ResponseEntity<GlobalResponse<Map<String, Object>>> avgAttemptsToPass() {
        double avg = repo.avgAttemptsToPass();
        return ok(Map.of("avgAttempts", Math.round(avg * 10.0) / 10.0));
    }

    // ── All teacher analytics bundled ───────────────────────────
    @GetMapping("/all")
    public ResponseEntity<GlobalResponse<Map<String, Object>>> all() {
        // locations
        var locations = repo.studentCountByGovernorate().stream().map(row -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("governorate", row[0]); m.put("count", row[1]);
            return m;
        }).toList();

        // purchase hours
        int[] hCounts = new int[24];
        for (Object[] row : repo.purchaseCountByHour())
            hCounts[((Number) row[0]).intValue()] = ((Number) row[1]).intValue();
        var purchaseHours = new ArrayList<Map<String, Object>>();
        for (int h = 0; h < 24; h++) purchaseHours.add(Map.of("hour", h, "count", hCounts[h]));

        // purchase days (DOW: 0=Sun in PG)
        String[] arabic = {"الأحد","الاثنين","الثلاثاء","الأربعاء","الخميس","الجمعة","السبت"};
        int[] dCounts = new int[7];
        for (Object[] row : repo.purchaseCountByDayOfWeek())
            dCounts[((Number) row[0]).intValue()] = ((Number) row[1]).intValue();
        var purchaseDays = new ArrayList<Map<String, Object>>();
        for (int d = 0; d < 7; d++) purchaseDays.add(Map.of("day", arabic[d], "dayIndex", d, "count", dCounts[d]));

        // sales by type
        var salesByType = repo.salesByProductType().stream().map(row -> {
            String type = row[0] != null ? row[0].toString() : "OTHER";
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("type", type);
            m.put("label", switch (type) { case "COURSE" -> "حصة فردية"; case "CATEGORY" -> "باقة"; default -> type; });
            m.put("count", row[1]);
            return m;
        }).toList();

        // login heatmap
        var loginHeatmap = repo.studentLoginHeatmap().stream().map(row -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("hour", row[0]); m.put("dayOfWeek", row[1]); m.put("count", row[2]);
            return m;
        }).toList();

        // avg score by course
        var avgScoreByCourse = repo.avgScoreByCourse().stream().map(row -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("course", row[0]);
            m.put("avgScore", row[1] != null ? Math.round(((Number) row[1]).doubleValue() * 10.0) / 10.0 : 0);
            return m;
        }).toList();

        // avg score by center
        var avgScoreByCenter = repo.avgScoreByCenter().stream().map(row -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("center", row[0]);
            m.put("avgScore", row[1] != null ? Math.round(((Number) row[1]).doubleValue() * 10.0) / 10.0 : 0);
            return m;
        }).toList();

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("locations",       locations);
        data.put("purchaseHours",   purchaseHours);
        data.put("purchaseDays",    purchaseDays);
        data.put("salesByType",     salesByType);
        data.put("loginHeatmap",    loginHeatmap);
        data.put("avgScoreByCourse", avgScoreByCourse);
        data.put("avgScoreByCenter", avgScoreByCenter);
        return ok(data);
    }

    private <T> ResponseEntity<GlobalResponse<T>> ok(T data) {
        return ResponseEntity.ok(GlobalResponse.success("ok", data));
    }
}
