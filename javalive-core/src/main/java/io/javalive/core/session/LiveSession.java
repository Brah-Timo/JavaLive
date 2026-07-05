package io.javalive.core.session;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

/**
 * Represents a single connected browser session.
 *
 * <p>One {@code LiveSession} is created for every WebSocket connection.
 * It tracks:
 * <ul>
 *   <li>The STOMP session ID (unique per connection)</li>
 *   <li>The authenticated user (if Spring Security is present)</li>
 *   <li>Which components are currently mounted in this browser tab</li>
 *   <li>Connection timestamps for monitoring and cleanup</li>
 *   <li>IP address for audit logging</li>
 * </ul>
 *
 * @author JavaLive Team
 * @since 1.0.0
 */
public class LiveSession {

    /** STOMP WebSocket session ID (assigned by Spring). */
    private final String sessionId;

    /** Authenticated username (from Spring Security principal), or "anonymous". */
    private String username;

    /** IP address of the connected client. */
    private String remoteAddress;

    /** When this session was first created. */
    private final Instant connectedAt;

    /** When this session last received any activity. */
    private volatile Instant lastActivityAt;

    /**
     * Set of component names currently mounted in this browser session.
     * Used to know which components to clean up on disconnect.
     */
    private final Set<String> mountedComponents = new HashSet<>();

    /** Whether the session is still connected. */
    private volatile boolean connected = true;

    public LiveSession(String sessionId, String username, String remoteAddress) {
        this.sessionId     = sessionId;
        this.username      = username != null ? username : "anonymous";
        this.remoteAddress = remoteAddress;
        this.connectedAt   = Instant.now();
        this.lastActivityAt = Instant.now();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Component tracking
    // ─────────────────────────────────────────────────────────────────────────

    public void mountComponent(String componentName) {
        mountedComponents.add(componentName);
        touch();
    }

    public void unmountComponent(String componentName) {
        mountedComponents.remove(componentName);
    }

    public boolean hasComponent(String componentName) {
        return mountedComponents.contains(componentName);
    }

    public Set<String> getMountedComponents() {
        return new HashSet<>(mountedComponents); // defensive copy
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Activity tracking
    // ─────────────────────────────────────────────────────────────────────────

    public void touch() {
        this.lastActivityAt = Instant.now();
    }

    public boolean isIdle(int timeoutMinutes) {
        Instant cutoff = Instant.now().minusSeconds(timeoutMinutes * 60L);
        return lastActivityAt.isBefore(cutoff);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Getters and setters
    // ─────────────────────────────────────────────────────────────────────────

    public String getSessionId()       { return sessionId; }
    public String getUsername()        { return username; }
    public void setUsername(String u)  { this.username = u; }
    public String getRemoteAddress()   { return remoteAddress; }
    public Instant getConnectedAt()    { return connectedAt; }
    public Instant getLastActivityAt() { return lastActivityAt; }
    public boolean isConnected()       { return connected; }
    public void disconnect()           { this.connected = false; }

    @Override
    public String toString() {
        return "LiveSession{id=" + sessionId + ", user=" + username +
               ", mounted=" + mountedComponents + "}";
    }
}
