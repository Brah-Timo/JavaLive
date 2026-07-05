package io.javalive.core.rendering;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Map;

/**
 * Encapsulates the data embedded in the HTML page for client-side hydration.
 *
 * <p>When JavaLive serves a page via SSR, it embeds the initial server state
 * directly in the HTML as a JSON script tag:
 * <pre>
 * {@code
 * <script id="javalive-state" type="application/json">
 *   {
 *     "component": "dashboard",
 *     "state": { "count": 0, "title": "Dashboard" },
 *     "sessionId": "abc-123",
 *     "wsEndpoint": "/javalive-ws"
 *   }
 * </script>
 * }
 * </pre>
 *
 * <p>The client runtime reads this on startup to initialize Vue's reactive state
 * before the WebSocket connects, preventing any visual "flash" on first load.
 *
 * @author JavaLive Team
 * @since 1.0.0
 */
public class HydrationData {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final String componentName;
    private final Map<String, Object> state;
    private String sessionId;
    private String wsEndpoint = "/javalive-ws";

    public HydrationData(String componentName, Map<String, Object> state) {
        this.componentName = componentName;
        this.state         = state;
    }

    /**
     * Serializes this hydration data to a JSON string for embedding in HTML.
     *
     * @return JSON string safe for embedding in a {@code <script type="application/json">} tag
     */
    public String toJson() {
        try {
            java.util.Map<String, Object> payload = new java.util.LinkedHashMap<>();
            payload.put("component",  componentName);
            payload.put("state",      state);
            payload.put("sessionId",  sessionId);
            payload.put("wsEndpoint", wsEndpoint);
            payload.put("ts",         System.currentTimeMillis());
            return MAPPER.writeValueAsString(payload);
        } catch (Exception e) {
            throw new RuntimeException("JavaLive: Failed to serialize hydration data", e);
        }
    }

    /**
     * Returns the full {@code <script>} HTML tag ready for insertion into the page.
     *
     * @return the complete script tag with embedded JSON
     */
    public String toScriptTag() {
        return "<script id=\"javalive-state\" type=\"application/json\">\n" +
               toJson() + "\n</script>";
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Getters and setters
    // ─────────────────────────────────────────────────────────────────────────

    public String getComponentName() { return componentName; }
    public Map<String, Object> getState() { return state; }
    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    public String getWsEndpoint() { return wsEndpoint; }
    public void setWsEndpoint(String wsEndpoint) { this.wsEndpoint = wsEndpoint; }
}
