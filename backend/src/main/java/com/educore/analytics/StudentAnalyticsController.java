package com.educore.analytics;

import com.educore.common.GlobalResponse;
import com.educore.security.JwtUserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.*;

@RestController
@RequestMapping("/api/analytics/student")
@PreAuthorize("hasRole('STUDENT')")
@RequiredArgsConstructor
public class StudentAnalyticsController {

    private final AnalyticsRepository repo;

    /** My score vs center avg vs all students — per course */
    @GetMapping("/vs-avg")
    public ResponseEntity<GlobalResponse<List<Map<String, Object>>>> vsAvg(
            @AuthenticationPrincipal JwtUserPrincipal p) {
        return ok(buildVsAvg(p.getUserId()));
    }

    /** Monthly quiz score progress */
    @GetMapping("/progress-over-time")
    public ResponseEntity<GlobalResponse<List<Map<String, Object>>>> progress(
            @AuthenticationPrincipal JwtUserPrincipal p) {
        return ok(buildProgress(p.getUserId()));
    }

    /** Login hours heatmap */
    @GetMapping("/login-hours")
    public ResponseEntity<GlobalResponse<Map<String, Object>>> loginHours(
            @AuthenticationPrincipal JwtUserPrincipal p) {
        return ok(buildLoginHours(p.getUserId()));
    }

    /** Achievements */
    @GetMapping("/achievements")
    public ResponseEntity<GlobalResponse<List<Map<String, Object>>>> achievements(
            @AuthenticationPrincipal JwtUserPrincipal p) {
        return ok(buildAchievements(p.getUserId()));
    }

    /** Streak + total active days */
    @GetMapping("/streak")
    public ResponseEntity<GlobalResponse<Map<String, Object>>> streak(
            @AuthenticationPrincipal JwtUserPrincipal p) {
        return ok(buildStreak(p.getUserId()));
    }

    /** Quiz solving speed per quiz */
    @GetMapping("/quiz-speed")
    public ResponseEntity<GlobalResponse<List<Map<String, Object>>>> quizSpeed(
            @AuthenticationPrincipal JwtUserPrincipal p) {
        return ok(buildQuizSpeed(p.getUserId()));
    }

    /** Quiz attempts until passed */
    @GetMapping("/attempts-to-pass")
    public ResponseEntity<GlobalResponse<List<Map<String, Object>>>> attemptsToPass(
            @AuthenticationPrincipal JwtUserPrincipal p) {
        return ok(buildAttemptsToPass(p.getUserId()));
    }

    /** All analytics bundled — one call for the page */
    @GetMapping("/all")
    public ResponseEntity<GlobalResponse<Map<String, Object>>> all(
            @AuthenticationPrincipal JwtUserPrincipal p) {
        Long sid = p.getUserId();
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("vsAvg",          buildVsAvg(sid));
        data.put("progressOverTime", buildProgress(sid));
        data.put("loginHours",     buildLoginHours(sid));
        data.put("achievements",   buildAchievements(sid));
        data.put("streak",         buildStreak(sid));
        data.put("quizSpeed",      buildQuizSpeed(sid));
        data.put("attemptsToPass", buildAttemptsToPass(sid));
        return ok(data);
    }

    // ─────────────────────────────────────────────────────────────
    // Shared builders — reused by ParentAnalyticsController
    // ─────────────────────────────────────────────────────────────

    List<Map<String, Object>> buildVsAvg(Long sid) {
        return repo.studentVsAvgByCourse(sid).stream().map(row -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("course",    row[0]);
            m.put("myScore",   row[1] != null ? Math.round(((Number) row[1]).doubleValue() * 10.0) / 10.0 : 0);
            m.put("centerAvg", row[2] != null ? Math.round(((Number) row[2]).doubleValue() * 10.0) / 10.0 : 0);
            m.put("allAvg",    row[3] != null ? Math.round(((Number) row[3]).doubleValue() * 10.0) / 10.0 : 0);
            return m;
        }).toList();
    }

    List<Map<String, Object>> buildProgress(Long sid) {
        String[] months = {"يناير","فبراير","مارس","أبريل","مايو","يونيو",
                           "يوليو","أغسطس","سبتمبر","أكتوبر","نوفمبر","ديسمبر"};
        return repo.quizScoreProgressOverTime(sid).stream().map(row -> {
            int year  = ((Number) row[0]).intValue();
            int month = ((Number) row[1]).intValue(); // 1-12
            double avg = row[2] != null ? Math.round(((Number) row[2]).doubleValue() * 10.0) / 10.0 : 0;
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("label",    months[month - 1] + " " + year);
            m.put("year",     year);
            m.put("month",    month);
            m.put("avgScore", avg);
            return m;
        }).toList();
    }

    Map<String, Object> buildLoginHours(Long sid) {
        int[] hours = new int[24];
        int[] days  = new int[8]; // 1-7
        for (Object[] r : repo.studentLoginHours(sid))
            hours[((Number) r[0]).intValue()] = ((Number) r[1]).intValue();
        for (Object[] r : repo.studentLoginDays(sid))
            days[((Number) r[0]).intValue()] = ((Number) r[1]).intValue();
        long totalSecs   = repo.studentTotalWatchSeconds(sid);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("byHour", hours);
        result.put("byDay",  Arrays.copyOfRange(days, 1, 8)); // [0]=Sun .. [6]=Sat
        result.put("totalWatchSeconds", totalSecs);
        result.put("totalWatchMinutes", totalSecs / 60);
        result.put("totalWatchHours",   Math.round(totalSecs / 3600.0 * 10.0) / 10.0);
        return result;
    }

    List<Map<String, Object>> buildAchievements(Long sid) {
        Object[] m = repo.studentMilestones(sid);
        long completedLessons = m[0] != null ? ((Number) m[0]).longValue() : 0;
        long passedQuizzes    = m[1] != null ? ((Number) m[1]).longValue() : 0;
        long enrollments      = m[2] != null ? ((Number) m[2]).longValue() : 0;
        long watchHours       = repo.studentTotalWatchSeconds(sid) / 3600;

        List<Map<String, Object>> achievements = new ArrayList<>();
        achievements.add(achievement("🎯", "أول كويز",        "أجبت على أول كويز",        passedQuizzes >= 1));
        achievements.add(achievement("📚", "5 دروس",          "أكملت 5 دروس",              completedLessons >= 5));
        achievements.add(achievement("📚", "20 درس",          "أكملت 20 درساً",            completedLessons >= 20));
        achievements.add(achievement("📚", "50 درس",          "أكملت 50 درساً",            completedLessons >= 50));
        achievements.add(achievement("🏆", "10 كويز",         "اجتزت 10 اختبارات",         passedQuizzes >= 10));
        achievements.add(achievement("🏆", "50 كويز",         "اجتزت 50 اختباراً",         passedQuizzes >= 50));
        achievements.add(achievement("⏱️", "ساعة دراسة",      "درست ساعة كاملة",           watchHours >= 1));
        achievements.add(achievement("⏱️", "10 ساعات",        "درست 10 ساعات",             watchHours >= 10));
        achievements.add(achievement("⏱️", "50 ساعة",         "درست 50 ساعة",              watchHours >= 50));
        achievements.add(achievement("🎓", "3 كورسات",        "اشتركت في 3 كورسات",        enrollments >= 3));
        achievements.add(achievement("🌟", "100 درس",         "أتممت 100 درس — متميز!",    completedLessons >= 100));
        return achievements;
    }

    Map<String, Object> buildStreak(Long sid) {
        List<Object[]> rows = repo.studentActiveDates(sid);
        List<LocalDate> dates = rows.stream()
            .map(r -> {
                // r[0] may be java.sql.Date or String
                try { return LocalDate.parse(r[0].toString()); }
                catch (Exception e) {
                    if (r[0] instanceof java.sql.Date d) return d.toLocalDate();
                    return null;
                }
            })
            .filter(Objects::nonNull)
            .sorted(Comparator.reverseOrder())
            .toList();

        int streak = 0;
        if (!dates.isEmpty()) {
            LocalDate expected = LocalDate.now();
            // allow yesterday as current streak
            if (dates.get(0).equals(LocalDate.now().minusDays(1))) expected = LocalDate.now().minusDays(1);
            if (dates.get(0).equals(LocalDate.now())) expected = LocalDate.now();
            for (LocalDate d : dates) {
                if (d.equals(expected)) { streak++; expected = expected.minusDays(1); }
                else break;
            }
        }
        return Map.of(
            "currentStreak", streak,
            "totalActiveDays", dates.size()
        );
    }

    List<Map<String, Object>> buildQuizSpeed(Long sid) {
        return repo.studentQuizSpeed(sid).stream().map(row -> {
            long secs = row[1] != null ? ((Number) row[1]).longValue() : 0;
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("quiz",       row[0]);
            m.put("avgSeconds", secs);
            m.put("avgMinutes", secs / 60);
            return m;
        }).toList();
    }

    List<Map<String, Object>> buildAttemptsToPass(Long sid) {
        return repo.studentQuizAttemptsToPass(sid).stream().map(row -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("quiz",     row[0]);
            m.put("attempts", row[1]);
            return m;
        }).toList();
    }

    private Map<String, Object> achievement(String icon, String name, String desc, boolean unlocked) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("icon", icon); m.put("name", name); m.put("description", desc); m.put("unlocked", unlocked);
        return m;
    }

    private <T> ResponseEntity<GlobalResponse<T>> ok(T data) {
        return ResponseEntity.ok(GlobalResponse.success("ok", data));
    }
}
