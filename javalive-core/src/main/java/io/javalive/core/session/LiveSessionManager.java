package io.javalive.core.session;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages the lifecycle of all active {@link LiveSession} objects.
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>Create sessions on WebSocket connect</li>
 *   <li>Remove sessions on WebSocket disconnect</li>
 *   <li>Periodically clean up idle sessions</li>
 *   <li>Provide lookup by session ID or username</li>
 *   <li>Track active session count for monitoring</li>
 * </ul>
 *
 * <p>Thread-safe: uses {@link ConcurrentHashMap} internally.
 *
 * @author JavaLive Team
 * @since 1.0.0
 */
@Component
public class LiveSessionManager {

    private static final Logger log = LoggerFactory.getLogger(LiveSessionManager.class);

    /** All active sessions: sessionId → LiveSession */
    private final Map<String, LiveSession> sessions = new ConcurrentHashMap<>();

    @Autowired(required = false)
    private JavaLiveSessionListener sessionListener;

    // ─────────────────────────────────────────────────────────────────────────
    // Session lifecycle
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Creates and registers a new session for an incoming WebSocket connection.
     *
     * @param sessionId     the STOMP session ID
     * @param username      the authenticated username (or null for anonymous)
     * @param remoteAddress the client's IP address
     * @return the created LiveSession
     */
    public LiveSession createSession(String sessionId, String username, String remoteAddress) {
        LiveSession session = new LiveSession(sessionId, username, remoteAddress);
        sessions.put(sessionId, session);

        log.info("JavaLive: New session connected [id={}, user={}, ip={}]",
                 sessionId, session.getUsername(), remoteAddress);

        if (sessionListener != null) {
            sessionListener.onSessionCreated(session);
        }

        return session;
    }

    /**
     * Removes and cleans up a disconnected session.
     *
     * @param sessionId the session to remove
     * @return the removed session, or empty if not found
     */
    public Optional<LiveSession> removeSession(String sessionId) {
        LiveSession session = sessions.remove(sessionId);
        if (session != null) {
            session.disconnect();
            log.info("JavaLive: Session disconnected [id={}, user={}, duration={}s]",
                     sessionId, session.getUsername(),
                     java.time.Duration.between(session.getConnectedAt(),
                                                java.time.Instant.now()).getSeconds());

            if (sessionListener != null) {
                sessionListener.onSessionRemoved(session);
            }
        }
        return Optional.ofNullable(session);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Lookup
    // ─────────────────────────────────────────────────────────────────────────

    public Optional<LiveSession> findById(String sessionId) {
        return Optional.ofNullable(sessions.get(sessionId));
    }

    public Collection<LiveSession> findByUsername(String username) {
        return sessions.values().stream()
            .filter(s -> username.equals(s.getUsername()))
            .toList();
    }

    public Collection<LiveSession> getAllSessions() {
        return Collections.unmodifiableCollection(sessions.values());
    }

    public int getActiveSessionCount() {
        return sessions.size();
    }

    public boolean sessionExists(String sessionId) {
        return sessions.containsKey(sessionId);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Component tracking
    // ─────────────────────────────────────────────────────────────────────────

    public void markComponentMounted(String sessionId, String componentName) {
        findById(sessionId).ifPresent(s -> s.mountComponent(componentName));
    }

    public void markComponentUnmounted(String sessionId, String componentName) {
        findById(sessionId).ifPresent(s -> s.unmountComponent(componentName));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Idle session cleanup (runs every 5 minutes)
    // ─────────────────────────────────────────────────────────────────────────

    @Scheduled(fixedDelay = 5 * 60 * 1000)
    public void cleanupIdleSessions() {
        int timeout = 30; // minutes — in production, read from JavaLiveProperties
        int removed = 0;

        for (Map.Entry<String, LiveSession> entry : sessions.entrySet()) {
            if (entry.getValue().isIdle(timeout)) {
                sessions.remove(entry.getKey());
                removed++;
                log.debug("JavaLive: Cleaned up idle session [id={}]", entry.getKey());
            }
        }

        if (removed > 0) {
            log.info("JavaLive: Cleaned up {} idle sessions. Active: {}",
                     removed, sessions.size());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Monitoring
    // ─────────────────────────────────────────────────────────────────────────

    public Map<String, Object> getStats() {
        return Map.of(
            "activeSessions", sessions.size(),
            "sessions", sessions.values().stream().map(s -> Map.of(
                "id", s.getSessionId(),
                "user", s.getUsername(),
                "connectedAt", s.getConnectedAt().toString(),
                "mountedComponents", s.getMountedComponents()
            )).toList()
        );
    }
}
