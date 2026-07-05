package io.javalive.core.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.*;

/**
 * Configures the Spring WebSocket message broker for JavaLive.
 *
 * <p>Sets up STOMP protocol support with SockJS fallback, configuring:
 * <ul>
 *   <li>The broker endpoint ({@code /javalive-ws})</li>
 *   <li>Application destination prefix ({@code /app}) for client → server messages</li>
 *   <li>Broker destination prefixes ({@code /topic}, {@code /queue})</li>
 *   <li>User destination prefix ({@code /user}) for session-specific messages</li>
 *   <li>Message size limits and timeout settings</li>
 *   <li>SockJS fallback for browsers without native WebSocket</li>
 * </ul>
 *
 * <h3>Message routing summary:</h3>
 * <pre>
 * Client → Server:
 *   /app/dashboard.init        → handled by DashboardLiveController
 *   /app/dashboard.increment   → handled by DashboardLiveController
 *
 * Server → Specific Client:
 *   /user/topic/dashboard.state → sent to the originating session only
 *
 * Server → All Clients (global state):
 *   /topic/dashboard.state.global → broadcast to all connected sessions
 * </pre>
 *
 * @author JavaLive Team
 * @since 1.0.0
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Autowired
    private JavaLiveProperties properties;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // Prefixes for messages TO the server (client-initiated)
        registry.setApplicationDestinationPrefixes("/app");

        // Simple in-memory broker for messages FROM the server
        // /topic  → broadcast to all subscribers
        // /queue  → point-to-point (not widely used in JavaLive but useful for extensions)
        registry.enableSimpleBroker("/topic", "/queue");

        // Prefix for user-specific (session-scoped) messages
        // /user/topic/dashboard.state → only the specific session receives this
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry
            .addEndpoint(properties.getWebsocket().getEndpoint())
            .setAllowedOriginPatterns(properties.getWebsocket().getAllowedOrigins())
            .withSockJS()  // SockJS fallback for older browsers or proxies blocking WS
                .setHeartbeatTime(properties.getWebsocket().getHeartbeatIncoming())
                .setDisconnectDelay(5_000)
                .setSessionCookieNeeded(false);
    }

    @Override
    public void configureWebSocketTransport(WebSocketTransportRegistration registration) {
        registration
            .setMessageSizeLimit(properties.getWebsocket().getMessageSizeLimit())
            .setSendBufferSizeLimit(properties.getWebsocket().getSendBufferSizeLimit())
            .setSendTimeLimit(properties.getWebsocket().getSendTimeLimit());
    }
}
