package io.javalive.core.websocket;

import java.util.List;
import java.util.Map;

/**
 * Represents a message sent from the browser to the JavaLive server.
 *
 * <p>All WebSocket messages from the client follow this structure:
 * <pre>
 * {
 *   "method": "increment",
 *   "args": [42, "hello"],
 *   "currentState": { "count": 5, "title": "Dashboard" }
 * }
 * </pre>
 *
 * <p>The {@code currentState} field is included as a consistency check —
 * the server uses its own authoritative state, but the client's view
 * helps detect any synchronization issues.
 *
 * @author JavaLive Team
 * @since 1.0.0
 */
public class ClientMessage {

    /** The Java method name to invoke (must match a @VueMethod). */
    private String method;

    /** Arguments to pass to the method. May be null or empty for no-arg methods. */
    private List<Object> args;

    /** The client's current view of the state (used for sync verification). */
    private Map<String, Object> currentState;

    /** Client-generated correlation ID for request tracking (optional). */
    private String correlationId;

    /** Component name (optional, for routing validation). */
    private String component;

    // ─────────────────────────────────────────────────────────────────────────
    // Getters and setters (Jackson-compatible)
    // ─────────────────────────────────────────────────────────────────────────

    public String getMethod()                   { return method; }
    public void setMethod(String method)         { this.method = method; }

    public List<Object> getArgs()               { return args != null ? args : List.of(); }
    public void setArgs(List<Object> args)       { this.args = args; }

    public Map<String, Object> getCurrentState() { return currentState; }
    public void setCurrentState(Map<String, Object> s) { this.currentState = s; }

    public String getCorrelationId()             { return correlationId; }
    public void setCorrelationId(String id)      { this.correlationId = id; }

    public String getComponent()                 { return component; }
    public void setComponent(String component)   { this.component = component; }

    @Override
    public String toString() {
        return "ClientMessage{method='" + method + "', args=" + args + "}";
    }
}
