package com.educore.session;

import com.educore.student.StudentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class DatabaseSessionService {

    private final UserSessionRepository sessionRepository;
    private final StudentRepository studentRepository;

    /** Sliding-window timeout — kept in sync with TeacherAuthService / AdminAuthService defaults. */
    @Value("${app.session.timeout:30}")
    private int sessionTimeoutMinutes;

    // ─────────────────────────────────────────────────────────────
    // Session Creation
    // ─────────────────────────────────────────────────────────────

    /**
     * Saves a new session for the user.
     * Cleans up expired sessions first to keep the table lean.
     */
    @Transactional
    public UserSession saveSession(Long userId, String userType, String token,
                                   String deviceId, String sessionId, int timeoutMinutes) {
        // Remove expired sessions for this user before adding a new one
        sessionRepository.deleteExpiredSessionsByUserId(userId, LocalDateTime.now());

        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(timeoutMinutes);
        UserSession session = new UserSession(userId, userType, token, deviceId, sessionId, expiresAt);

        return sessionRepository.save(session);
    }

    // ─────────────────────────────────────────────────────────────
    // Token & Session Validation
    // ─────────────────────────────────────────────────────────────

    /**
     * Checks whether a token is valid (exists in DB, not expired, not blacklisted).
     * Also bumps lastActivityAt on a valid token.
     */
    @Transactional
    public boolean isTokenValid(String token) {
        if (token == null || token.isBlank()) return false;

        Optional<UserSession> sessionOpt = sessionRepository.findByToken(token);
        if (sessionOpt.isEmpty()) return false;

        UserSession session = sessionOpt.get();
        if (session.isExpired() || session.isBlacklisted()) return false;

        // Sliding-window: extend expiry on every valid request so active users stay logged in.
        // Also bumps lastActivityAt for audit/analytics purposes.
        LocalDateTime now = LocalDateTime.now();
        sessionRepository.updateActivity(token, now);
        sessionRepository.extendSession(token, now.plusMinutes(sessionTimeoutMinutes));
        return true;
    }

    /**
     * Returns true if the given device has an active, non-blacklisted session for this user.
     */
    public boolean isValidDevice(Long userId, String deviceId) {
        if (userId == null || deviceId == null) return false;

        return sessionRepository
                .findActiveSessionByUserAndDevice(userId, deviceId, LocalDateTime.now())
                .isPresent();
    }

    /**
     * Returns true if the user has NO active sessions (all expired or blacklisted).
     */
    public boolean isSessionExpired(Long userId) {
        return sessionRepository.countActiveSessions(userId, LocalDateTime.now()) == 0;
    }

    /** Returns true if the given token belongs to the given user and is currently valid. */
    public boolean isSessionValid(Long userId, String token) {
        return isTokenValid(token) && sessionRepository.findByToken(token)
                .map(session -> session.getUserId().equals(userId))
                .orElse(false);
    }

    // ─────────────────────────────────────────────────────────────
    // Session Retrieval
    // ─────────────────────────────────────────────────────────────

    /**
     * Returns the first active session for a user as a key-value map.
     * Used when renewing sessions to retrieve the existing token.
     */
    public Optional<Map<String, Object>> getUserSession(Long userId) {
        return sessionRepository
                .findActiveSessionsByUserId(userId, LocalDateTime.now())
                .stream()
                .findFirst()
                .map(session -> Map.<String, Object>of(
                        "userId",         session.getUserId(),
                        "userType",       session.getUserType(),
                        "token",          session.getToken(),
                        "deviceId",       session.getDeviceId(),
                        "sessionId",      session.getSessionId(),
                        "expiresAt",      session.getExpiresAt(),
                        "lastActivityAt", session.getLastActivityAt()
                ));
    }

    /** Returns the raw UserSession entity by token. */
    public Optional<UserSession> getSessionByToken(String token) {
        return sessionRepository.findByToken(token);
    }

    /** Returns all sessions for a user filtered by user type. */
    public List<UserSession> getUserSessionsByType(Long userId, String userType) {
        return sessionRepository.findByUserIdAndUserType(userId, userType);
    }

    /** Returns a list of active sessions with safe metadata (token is truncated). */
    public List<Map<String, Object>> getUserActiveSessions(Long userId) {
        return sessionRepository
                .findActiveSessionsByUserId(userId, LocalDateTime.now())
                .stream()
                .map(session -> Map.<String, Object>of(
                        "id",             session.getId(),
                        "deviceId",       session.getDeviceId(),
                        "userType",       session.getUserType(),
                        "lastActivityAt", session.getLastActivityAt(),
                        "expiresAt",      session.getExpiresAt(),
                        "createdAt",      session.getCreatedAt(),
                        // Never expose the full token in listings
                        "tokenPreview",   session.getToken().substring(0, Math.min(20, session.getToken().length())) + "..."
                ))
                .toList();
    }

    /** Returns the active session for a specific user+device combination. */
    public Optional<UserSession> getSessionByUserAndDevice(Long userId, String deviceId) {
        return sessionRepository.findByUserIdAndDeviceId(userId, deviceId);
    }

    /** Removes expired sessions for a specific user — call before counting active sessions. */
    @Transactional
    public void cleanExpiredSessions(Long userId) {
        sessionRepository.deleteExpiredSessionsByUserId(userId, LocalDateTime.now());
    }

    /** Returns the number of active (non-expired, non-blacklisted) sessions for a user. */
    public int getActiveSessionsCount(Long userId) {
        return sessionRepository.countActiveSessions(userId, LocalDateTime.now());
    }

    /** Returns true if the user has at least one active session. */
    public boolean hasActiveSession(Long userId) {
        return getActiveSessionsCount(userId) > 0;
    }

    // ─────────────────────────────────────────────────────────────
    // Session Updates
    // ─────────────────────────────────────────────────────────────

    /**
     * Updates lastActivityAt for all active sessions of a user.
     * Uses a bulk UPDATE query — no N+1 saves.
     */
    @Transactional
    public void updateUserActivity(Long userId) {
        sessionRepository.updateActivityByUserId(userId, LocalDateTime.now());
    }

    /**
     * Extends the expiry time of a specific token's session.
     * Uses a targeted UPDATE query instead of loading the entity.
     */
    @Transactional
    public void extendSession(String token, int additionalMinutes) {
        LocalDateTime newExpiry = LocalDateTime.now().plusMinutes(additionalMinutes);
        sessionRepository.extendSession(token, newExpiry);
        log.debug("Extended session for token prefix: {}...",
                token.substring(0, Math.min(10, token.length())));
    }

    /**
     * Extends expiry for all active sessions of a user.
     * Uses a bulk UPDATE query — no N+1 saves.
     */
    @Transactional
    public void extendAllActiveSessions(Long userId, int additionalMinutes) {
        LocalDateTime now      = LocalDateTime.now();
        LocalDateTime newExpiry = now.plusMinutes(additionalMinutes);
        sessionRepository.extendActiveSessionsByUserId(userId, newExpiry, now);
        log.debug("Extended all active sessions for userId: {}", userId);
    }

    // ─────────────────────────────────────────────────────────────
    // Session Termination
    // ─────────────────────────────────────────────────────────────

    /**
     * Logs out a user by blacklisting their token and clearing the student session state.
     * Uses targeted queries — no entity loading.
     */
    @Transactional
    public void deleteUserSession(Long userId, String token) {
        sessionRepository.blacklistToken(token, "User logged out");

        // Clear session fields on the Student entity
        studentRepository.findById(userId).ifPresent(student -> {
            student.clearActiveSession();
            studentRepository.save(student);
        });
    }

    /**
     * Force-logs out all sessions for a user by blacklisting them in one bulk UPDATE.
     * Used by admins or when a security event requires immediate session revocation.
     */
    @Transactional
    public void forceLogoutAll(Long userId, String userType) {
        log.info("Force-logout all sessions for userId: {}, type: {}", userId, userType);

        // Bulk blacklist — single UPDATE instead of N individual saves
        // userType filter is critical: prevents accidentally blacklisting sessions of other
        // roles that share the same numeric userId (e.g. Parent id=8 and Student id=8).
        sessionRepository.blacklistAllByUserId(userId, userType, "Force logout all sessions");

        if (com.educore.security.UserRole.STUDENT.name().equals(userType)) {
            studentRepository.findById(userId).ifPresent(student -> {
                student.clearActiveSession();
                studentRepository.save(student);
            });
        }

        log.info("Force-logout completed for userId: {}", userId);
    }

    /** Deletes all sessions for a user+type combination. */
    @Transactional
    public void deleteAllUserSessions(Long userId, String userType) {
        sessionRepository.deleteAllUserSessions(userId, userType);
        log.info("Deleted all sessions for userId: {}, type: {}", userId, userType);
    }

    // ─────────────────────────────────────────────────────────────
    // Blacklist Operations
    // ─────────────────────────────────────────────────────────────

    /** Blacklists a specific token with an explicit reason. */
    @Transactional
    public void blacklistToken(String token, String reason) {
        sessionRepository.blacklistToken(token, reason);
    }

    /** Returns true if the token exists in the DB and is blacklisted. */
    public boolean isTokenBlacklisted(String token) {
        return sessionRepository.findByToken(token)
                .map(UserSession::isBlacklisted)
                .orElse(false);
    }

    /** Blacklists a token if it is expired but not yet blacklisted. */
    @Transactional
    public void blacklistIfExpired(String token) {
        sessionRepository.findByToken(token).ifPresent(session -> {
            if (session.isExpired() && !session.isBlacklisted()) {
                session.blacklist("Token expired");
                sessionRepository.save(session);
            }
        });
    }

    // ─────────────────────────────────────────────────────────────
    // Cleanup (Scheduled)
    // ─────────────────────────────────────────────────────────────

    /**
     * Periodically deletes expired sessions from the database.
     * Runs every 5 minutes. Uses a bulk DELETE query — no in-memory processing.
     */
    @Transactional
    @Scheduled(fixedRate = 300_000)
    public void cleanupExpiredSessions() {
        LocalDateTime now = LocalDateTime.now();
        sessionRepository.deleteExpiredSessions(now);
        log.info("Expired sessions cleaned up at {}", now);
    }

    /**
     * Deletes sessions older than the given number of days.
     * Uses a bulk DELETE query instead of loading all records into memory.
     */
    @Transactional
    public void deleteOldSessions(int daysOld) {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(daysOld);
        sessionRepository.deleteSessionsOlderThan(cutoff);
        log.info("Deleted sessions older than {} days (cutoff: {})", daysOld, cutoff);
    }

    // ─────────────────────────────────────────────────────────────
    // Statistics
    // ─────────────────────────────────────────────────────────────

    /**
     * Returns session statistics using aggregate DB queries.
     * Previously loaded ALL sessions into memory via findAll() — now fully DB-side.
     */
    public Map<String, Object> getSessionStats() {
        LocalDateTime now = LocalDateTime.now();
        return Map.of(
                "totalSessions",       sessionRepository.countAllSessions(),
                "activeSessions",      sessionRepository.countAllActiveSessions(now),
                "expiredSessions",     sessionRepository.countExpiredSessions(now),
                "blacklistedSessions", sessionRepository.countBlacklistedSessions(),
                "uniqueActiveUsers",   sessionRepository.countActiveUsers(now)
        );
    }
}
