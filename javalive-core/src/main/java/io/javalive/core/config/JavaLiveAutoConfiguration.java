package io.javalive.core.config;

import io.javalive.core.dispatch.MethodDispatcher;
import io.javalive.core.dispatch.SecurityGuard;
import io.javalive.core.rendering.PageComposer;
import io.javalive.core.rendering.SsrRenderer;
import io.javalive.core.routing.PageRegistry;
import io.javalive.core.routing.RouteLoader;
import io.javalive.core.session.LiveSessionManager;
import io.javalive.core.state.SchemaLoader;
import io.javalive.core.state.StateManager;
import io.javalive.core.state.StateSerializer;
import io.javalive.core.websocket.LiveWebSocketHandler;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

/**
 * Spring Boot AutoConfiguration for JavaLive.
 *
 * <p>This class is referenced from:
 * {@code META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports}
 *
 * <p>It wires up all JavaLive beans when:
 * <ul>
 *   <li>The {@code javalive-core} jar is on the classpath (auto-detected)</li>
 *   <li>{@code javalive.enabled} is not set to {@code false}</li>
 * </ul>
 *
 * <p>All beans use {@code @ConditionalOnMissingBean} so applications can
 * override any individual bean by declaring their own implementation.
 *
 * @author JavaLive Team
 * @since 1.0.0
 */
@AutoConfiguration
@EnableConfigurationProperties(JavaLiveProperties.class)
@ConditionalOnProperty(prefix = "javalive", name = "enabled", havingValue = "true", matchIfMissing = true)
@Import({WebSocketConfig.class})
public class JavaLiveAutoConfiguration {

    /**
     * Central state manager — stores reactive state per session and component.
     */
    @Bean
    @ConditionalOnMissingBean
    public StateManager stateManager() {
        return new StateManager();
    }

    /**
     * Serializes state objects to JSON for WebSocket transmission.
     */
    @Bean
    @ConditionalOnMissingBean
    public StateSerializer stateSerializer() {
        return new StateSerializer();
    }

    /**
     * Manages all connected WebSocket sessions.
     */
    @Bean
    @ConditionalOnMissingBean
    public LiveSessionManager liveSessionManager() {
        return new LiveSessionManager();
    }

    /**
     * Dispatches @VueMethod calls from the browser to Java methods via reflection.
     */
    @Bean
    @ConditionalOnMissingBean
    public MethodDispatcher methodDispatcher() {
        return new MethodDispatcher();
    }

    /**
     * Security guard — prevents calling unannotated methods.
     */
    @Bean
    @ConditionalOnMissingBean
    public SecurityGuard securityGuard() {
        return new SecurityGuard();
    }

    /**
     * Registry of all @VuePage route definitions.
     */
    @Bean
    @ConditionalOnMissingBean
    public PageRegistry pageRegistry() {
        return new PageRegistry();
    }

    /**
     * Server-side renderer for first-page-load HTML generation.
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "javalive.ssr", name = "enabled", havingValue = "true", matchIfMissing = true)
    public SsrRenderer ssrRenderer(StateManager stateManager, StateSerializer stateSerializer) {
        return new SsrRenderer(stateManager, stateSerializer);
    }

    /**
     * Composes full HTML pages with injected hydration data.
     */
    @Bean
    @ConditionalOnMissingBean
    public PageComposer pageComposer(SsrRenderer ssrRenderer) {
        return new PageComposer(ssrRenderer);
    }

    /**
     * Main WebSocket event handler for connection/disconnection events.
     */
    @Bean
    @ConditionalOnMissingBean
    public LiveWebSocketHandler liveWebSocketHandler(
            LiveSessionManager sessionManager,
            StateManager stateManager) {
        return new LiveWebSocketHandler(sessionManager, stateManager);
    }

    /**
     * Loads state schemas from generated JSON files at startup.
     */
    @Bean
    @ConditionalOnMissingBean
    public SchemaLoader schemaLoader() {
        return new SchemaLoader();
    }

    /**
     * Loads @VuePage routes from generated schema files into the PageRegistry.
     */
    @Bean
    @ConditionalOnMissingBean
    public RouteLoader routeLoader() {
        return new RouteLoader();
    }
}
