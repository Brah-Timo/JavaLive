package io.javalive.core.session;

/**
 * Callback interface for session lifecycle events.
 *
 * <p>Implement this interface and register it as a Spring bean to receive
 * notifications when sessions connect or disconnect.
 *
 * <h3>Usage:</h3>
 * <pre>{@code
 * @Component
 * public class MySessionAuditor implements JavaLiveSessionListener {
 *
 *     @Override
 *     public void onSessionCreated(LiveSession session) {
 *         auditLog.record("CONNECTED: " + session.getUsername());
 *     }
 *
 *     @Override
 *     public void onSessionRemoved(LiveSession session) {
 *         auditLog.record("DISCONNECTED: " + session.getUsername());
 *     }
 * }
 * }</pre>
 *
 * @author JavaLive Team
 * @since 1.0.0
 */
public interface JavaLiveSessionListener {

    /**
     * Called when a new WebSocket session is established.
     *
     * @param session the newly created session
     */
    void onSessionCreated(LiveSession session);

    /**
     * Called when a WebSocket session is terminated (disconnect or timeout).
     *
     * @param session the session that was removed
     */
    void onSessionRemoved(LiveSession session);
}
