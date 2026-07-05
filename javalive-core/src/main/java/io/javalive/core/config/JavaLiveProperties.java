package io.javalive.core.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Externalized configuration properties for JavaLive.
 *
 * <p>All properties are prefixed with {@code javalive.} in {@code application.properties}
 * or {@code application.yml}.
 *
 * <h3>Full example application.properties:</h3>
 * <pre>
 * # Core
 * javalive.enabled=true
 * javalive.debug=false
 *
 * # WebSocket
 * javalive.websocket.endpoint=/javalive-ws
 * javalive.websocket.allowed-origins=*
 * javalive.websocket.compression=true
 * javalive.websocket.message-size-limit=524288
 *
 * # Session
 * javalive.session.timeout-minutes=30
 * javalive.session.max-per-user=5
 *
 * # State
 * javalive.state.max-size-kb=1024
 *
 * # SSR
 * javalive.ssr.enabled=true
 * javalive.ssr.cache-size=100
 *
 * # Dev tools
 * javalive.dev.hot-reload=true
 * </pre>
 *
 * @author JavaLive Team
 * @since 1.0.0
 */
@ConfigurationProperties(prefix = "javalive")
public class JavaLiveProperties {

    /** Whether JavaLive is enabled. Set to false to disable all processing. */
    private boolean enabled = true;

    /** Enable debug logging for JavaLive internals. */
    private boolean debug = false;

    private final WebSocketProperties websocket = new WebSocketProperties();
    private final SessionProperties session = new SessionProperties();
    private final StateProperties state = new StateProperties();
    private final SsrProperties ssr = new SsrProperties();
    private final DevProperties dev = new DevProperties();

    // ─────────────────────────────────────────────────────────────────────────
    // Nested configuration classes
    // ─────────────────────────────────────────────────────────────────────────

    public static class WebSocketProperties {
        /** WebSocket endpoint path. */
        private String endpoint = "/javalive-ws";
        /** Allowed CORS origins. */
        private String allowedOrigins = "*";
        /** Enable per-message compression. */
        private boolean compression = true;
        /** Maximum message size in bytes. */
        private int messageSizeLimit = 512 * 1024;
        /** Send buffer size in bytes. */
        private int sendBufferSizeLimit = 1024 * 1024;
        /** Send timeout in milliseconds. */
        private int sendTimeLimit = 20_000;
        /** Heartbeat interval (incoming) in ms. */
        private int heartbeatIncoming = 4_000;
        /** Heartbeat interval (outgoing) in ms. */
        private int heartbeatOutgoing = 4_000;
        /** Reconnect delay in ms. */
        private int reconnectDelay = 2_000;

        // Getters and setters
        public String getEndpoint() { return endpoint; }
        public void setEndpoint(String endpoint) { this.endpoint = endpoint; }
        public String getAllowedOrigins() { return allowedOrigins; }
        public void setAllowedOrigins(String allowedOrigins) { this.allowedOrigins = allowedOrigins; }
        public boolean isCompression() { return compression; }
        public void setCompression(boolean compression) { this.compression = compression; }
        public int getMessageSizeLimit() { return messageSizeLimit; }
        public void setMessageSizeLimit(int messageSizeLimit) { this.messageSizeLimit = messageSizeLimit; }
        public int getSendBufferSizeLimit() { return sendBufferSizeLimit; }
        public void setSendBufferSizeLimit(int sendBufferSizeLimit) { this.sendBufferSizeLimit = sendBufferSizeLimit; }
        public int getSendTimeLimit() { return sendTimeLimit; }
        public void setSendTimeLimit(int sendTimeLimit) { this.sendTimeLimit = sendTimeLimit; }
        public int getHeartbeatIncoming() { return heartbeatIncoming; }
        public void setHeartbeatIncoming(int heartbeatIncoming) { this.heartbeatIncoming = heartbeatIncoming; }
        public int getHeartbeatOutgoing() { return heartbeatOutgoing; }
        public void setHeartbeatOutgoing(int heartbeatOutgoing) { this.heartbeatOutgoing = heartbeatOutgoing; }
        public int getReconnectDelay() { return reconnectDelay; }
        public void setReconnectDelay(int reconnectDelay) { this.reconnectDelay = reconnectDelay; }
    }

    public static class SessionProperties {
        /** Session idle timeout in minutes before cleanup. */
        private int timeoutMinutes = 30;
        /** Maximum concurrent sessions per user. */
        private int maxPerUser = 10;

        public int getTimeoutMinutes() { return timeoutMinutes; }
        public void setTimeoutMinutes(int timeoutMinutes) { this.timeoutMinutes = timeoutMinutes; }
        public int getMaxPerUser() { return maxPerUser; }
        public void setMaxPerUser(int maxPerUser) { this.maxPerUser = maxPerUser; }
    }

    public static class StateProperties {
        /** Maximum state size per component per session (KB). */
        private int maxSizeKb = 1024;

        public int getMaxSizeKb() { return maxSizeKb; }
        public void setMaxSizeKb(int maxSizeKb) { this.maxSizeKb = maxSizeKb; }
    }

    public static class SsrProperties {
        /** Enable server-side rendering for first page load. */
        private boolean enabled = true;
        /** Number of SSR results to cache. */
        private int cacheSize = 100;

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public int getCacheSize() { return cacheSize; }
        public void setCacheSize(int cacheSize) { this.cacheSize = cacheSize; }
    }

    public static class DevProperties {
        /** Enable hot-reload in development. */
        private boolean hotReload = false;

        public boolean isHotReload() { return hotReload; }
        public void setHotReload(boolean hotReload) { this.hotReload = hotReload; }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Root-level getters and setters
    // ─────────────────────────────────────────────────────────────────────────

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public boolean isDebug() { return debug; }
    public void setDebug(boolean debug) { this.debug = debug; }

    public WebSocketProperties getWebsocket() { return websocket; }
    public SessionProperties getSession() { return session; }
    public StateProperties getState() { return state; }
    public SsrProperties getSsr() { return ssr; }
    public DevProperties getDev() { return dev; }
}
