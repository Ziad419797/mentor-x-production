package com.educore.session;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserSessionRepository extends JpaRepository<UserSession, String> {

    Optional<UserSession> findByToken(String token);

    List<UserSession> findByUserId(Long userId);

    Optional<UserSession> findByUserIdAndDeviceId(Long userId, String deviceId);

    List<UserSession> findByUserIdAndUserType(Long userId, String userType);

    @Query("SELECT s FROM UserSession s WHERE s.userId = :userId AND s.userType = :userType AND s.blacklisted = false AND s.expiresAt > :now")
    Optional<UserSession> findActiveSession(@Param("userId") Long userId,
                                            @Param("userType") String userType,
                                            @Param("now") LocalDateTime now);

    @Query("SELECT COUNT(s) FROM UserSession s WHERE s.userId = :userId AND s.blacklisted = false AND s.expiresAt > :now")
    int countActiveSessions(@Param("userId") Long userId,
                            @Param("now") LocalDateTime now);

    @Modifying
    @Query("UPDATE UserSession s SET s.blacklisted = true, s.blacklistReason = :reason WHERE s.token = :token")
    void blacklistToken(@Param("token") String token,
                        @Param("reason") String reason);

    @Modifying
    @Query("DELETE FROM UserSession s WHERE s.expiresAt < :now")
    void deleteExpiredSessions(@Param("now") LocalDateTime now);

    @Modifying
    @Query("DELETE FROM UserSession s WHERE s.userId = :userId AND s.userType = :userType")
    void deleteAllUserSessions(@Param("userId") Long userId,
                               @Param("userType") String userType);

    @Modifying
    @Query("UPDATE UserSession s SET s.lastActivityAt = :now WHERE s.token = :token")
    void updateActivity(@Param("token") String token,
                        @Param("now") LocalDateTime now);

    @Modifying
    @Query("UPDATE UserSession s SET s.expiresAt = :newExpiry WHERE s.token = :token")
    void extendSession(@Param("token") String token,
                       @Param("newExpiry") LocalDateTime newExpiry);

    boolean existsByTokenAndBlacklistedFalseAndExpiresAtAfter(String token, LocalDateTime now);

    // ⭐⭐ استعلامات جديدة مطلوبة
    @Query("SELECT s FROM UserSession s WHERE s.userId = :userId AND s.blacklisted = false AND s.expiresAt > :now")
    List<UserSession> findActiveSessionsByUserId(@Param("userId") Long userId,
                                                 @Param("now") LocalDateTime now);

    @Query("SELECT s FROM UserSession s WHERE s.userId = :userId AND s.deviceId = :deviceId AND s.blacklisted = false AND s.expiresAt > :now")
    Optional<UserSession> findActiveSessionByUserAndDevice(@Param("userId") Long userId,
                                                           @Param("deviceId") String deviceId,
                                                           @Param("now") LocalDateTime now);

    @Query("SELECT COUNT(s) FROM UserSession s WHERE s.blacklisted = false AND s.expiresAt > :now")
    long countAllActiveSessions(@Param("now") LocalDateTime now);

    @Query("SELECT COUNT(DISTINCT s.userId) FROM UserSession s WHERE s.blacklisted = false AND s.expiresAt > :now")
    long countActiveUsers(@Param("now") LocalDateTime now);

    // ── Bulk update queries — avoid N+1 saves in service layer ──────────────

    /** Updates lastActivityAt for all active sessions of a user in one query. */
    @Modifying
    @Query("UPDATE UserSession s SET s.lastActivityAt = :now " +
           "WHERE s.userId = :userId AND s.blacklisted = false AND s.expiresAt > :now")
    void updateActivityByUserId(@Param("userId") Long userId, @Param("now") LocalDateTime now);

    /** Blacklists all active sessions for a user in one query (force logout). */
    @Modifying
    @Query("UPDATE UserSession s SET s.blacklisted = true, s.blacklistReason = :reason " +
           "WHERE s.userId = :userId AND s.blacklisted = false")
    void blacklistAllByUserId(@Param("userId") Long userId, @Param("reason") String reason);

    /** Extends expiry for all active sessions of a user in one query. */
    @Modifying
    @Query("UPDATE UserSession s SET s.expiresAt = :newExpiry " +
           "WHERE s.userId = :userId AND s.blacklisted = false AND s.expiresAt > :now")
    void extendActiveSessionsByUserId(@Param("userId") Long userId,
                                      @Param("newExpiry") LocalDateTime newExpiry,
                                      @Param("now") LocalDateTime now);

    /** Deletes only expired sessions for a specific user (cleanup on login). */
    @Modifying
    @Query("DELETE FROM UserSession s WHERE s.userId = :userId AND s.expiresAt < :now")
    void deleteExpiredSessionsByUserId(@Param("userId") Long userId, @Param("now") LocalDateTime now);

    /** Deletes sessions older than a given cutoff date. */
    @Modifying
    @Query("DELETE FROM UserSession s WHERE s.createdAt < :cutoff")
    void deleteSessionsOlderThan(@Param("cutoff") LocalDateTime cutoff);

    /** Counts expired sessions (for stats — runs in DB, not in Java). */
    @Query("SELECT COUNT(s) FROM UserSession s WHERE s.expiresAt < :now AND s.blacklisted = false")
    long countExpiredSessions(@Param("now") LocalDateTime now);

    /** Counts blacklisted sessions (for stats). */
    @Query("SELECT COUNT(s) FROM UserSession s WHERE s.blacklisted = true")
    long countBlacklistedSessions();

    /** Counts total sessions (for stats). */
    @Query("SELECT COUNT(s) FROM UserSession s")
    long countAllSessions();

    /** Login heatmap for students: [dow 0-6, hour 0-23, count] */
    @Query(value = """
        SELECT EXTRACT(DOW FROM created_at)::int  AS dow,
               EXTRACT(HOUR FROM created_at)::int AS hr,
               COUNT(*) AS cnt
        FROM user_sessions
        WHERE user_type = 'STUDENT'
        GROUP BY dow, hr
        ORDER BY dow, hr
    """, nativeQuery = true)
    List<Object[]> getStudentLoginHeatmap();

    /** Platform-time stats for students: [avg_minutes, max_minutes, total_sessions] */
    @Query(value = """
        SELECT COALESCE(AVG(EXTRACT(EPOCH FROM (last_activity_at - created_at)) / 60), 0) AS avg_min,
               COALESCE(MAX(EXTRACT(EPOCH FROM (last_activity_at - created_at)) / 60), 0) AS max_min,
               COUNT(*) AS total
        FROM user_sessions
        WHERE user_type = 'STUDENT'
          AND last_activity_at IS NOT NULL
          AND last_activity_at > created_at
    """, nativeQuery = true)
    Object[] getStudentSessionDurationStats();

    /** Active hours for one student: [hour 0-23, count] */
    /**
     * Active hours for one student: [hour 0-23, count]
     */
    @Query(value = """
    SELECT 
        CAST(EXTRACT(HOUR FROM created_at) AS INTEGER) AS hour,
        COUNT(*) AS count
    FROM user_sessions
    WHERE user_id = :userId
    GROUP BY CAST(EXTRACT(HOUR FROM created_at) AS INTEGER)
    ORDER BY hour
    """, nativeQuery = true)
    List<Object[]> getStudentActiveHours(@Param("userId") Long userId);

    /** Count distinct days a student was active */

    @Query(value = """
    SELECT COUNT(DISTINCT CAST(created_at AS DATE)) AS active_days
    FROM user_sessions
    WHERE user_id = :userId
    """, nativeQuery = true)
    Long countDistinctActiveDays(@Param("userId") Long userId);

}