package io.javalive.core.websocket;

import io.javalive.core.session.LiveSession;
import io.javalive.core.session.LiveSessionManager;
import io.javalive.core.state.StateManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.*;

import java.security.Principal;

/**
 * Listens to WebSocket connection lifecycle events (connect / disconnect).
 *
 * <p>Translates Spring's WebSocket application events into JavaLive session operations:
 * <ul>
 *   <li>{@link SessionConnectedEvent} → creates a new {@link LiveSession}</li>
 *   <li>{@link SessionDisconnectEvent} → cleans up session state and live session</li>
 *   <li>{@link SessionSubscribeEvent} → marks components as mounted</li>
 *   <li>{@link SessionUnsubscribeEvent} → marks components as unmounted</li>
 * </ul>
 *
 * @author JavaLive Team
 * @since 1.0.0
 */
@Component
public class LiveWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(LiveWebSocketHandler.class);

    private final LiveSessionManager sessionManager;
    private final StateManager stateManager;

    public LiveWebSocketHandler(LiveSessionManager sessionManager, StateManager stateManager) {
        this.sessionManager = sessionManager;
        this.stateManager   = stateManager;
    }

    /**
     * Fired when a new STOMP connection is established.
     * Creates a LiveSession for the new browser connection.
     */
    @EventListener
    public void handleSessionConnected(SessionConnectedEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = accessor.getSessionId();
        if (sessionId == null) return;

        Principal principal = accessor.getUser();
        String username = (principal != null) ? principal.getName() : "anonymous";

        // Extract remote address from native headers if available
        String remoteAddress = extractRemoteAddress(accessor);

        sessionManager.createSession(sessionId, username, remoteAddress);
    }

    /**
     * Fired when a STOMP connection is terminated (browser closed, navigated away, etc.)
     * Cleans up all state associated with this session.
     */
    @EventListener
    public void handleSessionDisconnect(SessionDisconnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = accessor.getSessionId();
        if (sessionId == null) return;

        // Clean up state in StateManager (removes session from all maps)
        stateManager.cleanupSession(sessionId);

        // Remove from session manager
        sessionManager.removeSession(sessionId);
    }

    /**
     * Fired when a client subscribes to a topic.
     * Used to track which components are mounted in which session.
     */
    @EventListener
    public void handleSubscribe(SessionSubscribeEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId    = accessor.getSessionId();
        String destination  = accessor.getDestination();

        if (sessionId != null && destination != null) {
            // Extract component name from subscription: "/user/topic/dashboard.state" → "dashboard"
            String componentName = extractComponentFromDestination(destination);
            if (componentName != null) {
                sessionManager.markComponentMounted(sessionId, componentName);
                log.debug("JavaLive: Component mounted [{}/{}]", sessionId, componentName);
            }
        }
    }

    /**
     * Fired when a client unsubscribes from a topic.
     */
    @EventListener
    public void handleUnsubscribe(SessionUnsubscribeEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId    = accessor.getSessionId();
        String destination  = accessor.getDestination();

        if (sessionId != null && destination != null) {
            String componentName = extractComponentFromDestination(destination);
            if (componentName != null) {
                sessionManager.markComponentUnmounted(sessionId, componentName);
                log.debug("JavaLive: Component unmounted [{}/{}]", sessionId, componentName);
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Private helpers
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Extracts the component name from a subscription destination.
     * "/user/topic/dashboard.state" → "dashboard"
     * "/topic/user-list.state.global" → "user-list"
     */
    private String extractComponentFromDestination(String destination) {
        if (destination == null) return null;

        // Strip prefix
        String path = destination;
        if (path.startsWith("/user/topic/")) path = path.substring("/user/topic/".length());
        else if (path.startsWith("/topic/"))   path = path.substring("/topic/".length());
        else return null;

        // Strip ".state" suffix
        int dotIndex = path.indexOf(".state");
        if (dotIndex > 0) {
            return path.substring(0, dotIndex);
        }

        return null;
    }

    private String extractRemoteAddress(StompHeaderAccessor accessor) {
        // Try to get from native headers (set by Spring's WebSocket transport)
        try {
            Object attr = accessor.getSessionAttributes();
            if (attr instanceof java.util.Map<?, ?> map) {
                Object addr = map.get("REMOTE_ADDRESS");
                if (addr != null) return addr.toString();
            }
        } catch (Exception ignored) {}
        return "unknown";
    }
}
