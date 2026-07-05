package io.javalive.core.websocket;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Represents a message sent from the JavaLive server to the browser.
 *
 * <p>All messages follow a consistent envelope structure so the client runtime
 * can handle them uniformly:
 * <pre>
 * {
 *   "type": "patch",
 *   "changed": { "count": 6 },
 *   "removed": [],
 *   "ts": "2024-01-01T12:00:00Z"
 * }
 * </pre>
 *
 * <h3>Message types:</h3>
 * <ul>
 *   <li><b>full</b>: Complete state snapshot (sent on component init)</li>
 *   <li><b>patch</b>: Only changed + removed fields (sent after method execution)</li>
 *   <li><b>error</b>: Error response (method threw exception)</li>
 *   <li><b>emit</b>: Vue event emission (from @VueEmit methods)</li>
 *   <li><b>reload</b>: Hot-reload signal in development</li>
 * </ul>
 *
 * @author JavaLive Team
 * @since 1.0.0
 */
public class ServerMessage {

    /** Message type: "full", "patch", "error", "emit", "reload". */
    private String type;

    /** Changed fields (for "full" and "patch" types). */
    private Map<String, Object> changed;

    /** Removed field names (for "patch" type). */
    private List<String> removed;

    /** Error message (for "error" type). */
    private String error;

    /** Error code (for "error" type). */
    private String errorCode;

    /** Event name (for "emit" type). */
    private String event;

    /** Event payload (for "emit" type). */
    private Object payload;

    /** Server timestamp for latency measurement. */
    private String ts;

    /** Correlation ID (mirrors the client's correlationId if provided). */
    private String correlationId;

    private ServerMessage() {
        this.ts = Instant.now().toString();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Factory methods
    // ─────────────────────────────────────────────────────────────────────────

    /** Creates a "full" state message (sent on component init). */
    public static ServerMessage fullState(Map<String, Object> state) {
        ServerMessage msg = new ServerMessage();
        msg.type    = "full";
        msg.changed = state;
        msg.removed = List.of();
        return msg;
    }

    /** Creates a "patch" message (sent after method execution). */
    public static ServerMessage patch(String type, Map<String, Object> changed,
                                      List<String> removed) {
        ServerMessage msg = new ServerMessage();
        msg.type    = type;
        msg.changed = changed;
        msg.removed = removed != null ? removed : List.of();
        return msg;
    }

    /** Creates an "error" message (sent when a method throws). */
    public static ServerMessage error(String errorMessage) {
        return error(errorMessage, "ERROR");
    }

    /** Creates an "error" message with a code. */
    public static ServerMessage error(String errorMessage, String errorCode) {
        ServerMessage msg = new ServerMessage();
        msg.type      = "error";
        msg.error     = errorMessage;
        msg.errorCode = errorCode;
        return msg;
    }

    /** Creates an "emit" message (from @VueEmit methods). */
    public static ServerMessage emit(String eventName, Object eventPayload) {
        ServerMessage msg = new ServerMessage();
        msg.type    = "emit";
        msg.event   = eventName;
        msg.payload = eventPayload;
        return msg;
    }

    /** Creates a "reload" message for hot-reload in development. */
    public static ServerMessage reload() {
        ServerMessage msg = new ServerMessage();
        msg.type = "reload";
        return msg;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Getters and setters
    // ─────────────────────────────────────────────────────────────────────────

    public String getType()                    { return type; }
    public void setType(String type)            { this.type = type; }

    public Map<String, Object> getChanged()    { return changed; }
    public void setChanged(Map<String, Object> c) { this.changed = c; }

    public List<String> getRemoved()           { return removed; }
    public void setRemoved(List<String> r)      { this.removed = r; }

    public String getError()                   { return error; }
    public void setError(String error)          { this.error = error; }

    public String getErrorCode()               { return errorCode; }
    public void setErrorCode(String ec)         { this.errorCode = ec; }

    public String getEvent()                   { return event; }
    public void setEvent(String event)          { this.event = event; }

    public Object getPayload()                 { return payload; }
    public void setPayload(Object payload)      { this.payload = payload; }

    public String getTs()                      { return ts; }
    public void setTs(String ts)               { this.ts = ts; }

    public String getCorrelationId()           { return correlationId; }
    public void setCorrelationId(String id)    { this.correlationId = id; }
}
