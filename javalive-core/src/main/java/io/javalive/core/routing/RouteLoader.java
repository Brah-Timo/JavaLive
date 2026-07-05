package io.javalive.core.routing;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

/**
 * Loads all generated route definitions into the {@link PageRegistry} at startup.
 *
 * <p>Scans the classpath for {@code *.schema.json} files in
 * {@code /static/javalive/schemas/} and registers any that contain route
 * information (i.e., generated from {@code @VuePage} classes) into the
 * {@link PageRegistry}.
 *
 * <p>This enables the {@link JavaLivePageController} to serve the correct
 * component for each URL without any reflection or classpath scanning of
 * Java classes.
 *
 * @author JavaLive Team
 * @since 1.0.0
 */
@Component
public class RouteLoader {

    private static final Logger log = LoggerFactory.getLogger(RouteLoader.class);
    private static final String SCHEMA_PATTERN = "classpath*:/static/javalive/schemas/*.schema.json";

    @Autowired
    private PageRegistry pageRegistry;

    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * Called after the full Spring context starts.
     * Reads all schema files and registers page routes.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void loadRoutes() {
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();

        try {
            Resource[] resources = resolver.getResources(SCHEMA_PATTERN);
            int registered = 0;

            for (Resource resource : resources) {
                try {
                    registered += loadRouteFromSchema(resource);
                } catch (Exception e) {
                    log.warn("JavaLive RouteLoader: Failed to load route from {}: {}",
                             resource.getFilename(), e.getMessage());
                }
            }

            if (registered > 0) {
                log.info("JavaLive RouteLoader: Registered {} route(s). Routes: {}",
                         registered,
                         pageRegistry.getAllRoutes().stream()
                             .map(r -> r.getPath() + " → " + r.getComponentName())
                             .toList());
            } else {
                log.info("JavaLive RouteLoader: No @VuePage routes found in schemas. " +
                         "If you have @VuePage components, ensure the annotation processor ran.");
            }

        } catch (IOException e) {
            log.warn("JavaLive RouteLoader: Could not scan for schemas: {}", e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Private helpers
    // ─────────────────────────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private int loadRouteFromSchema(Resource resource) throws IOException {
        try (InputStream is = resource.getInputStream()) {
            Map<String, Object> json = mapper.readValue(is, Map.class);

            // Only register if this schema has a route section (i.e., @VuePage)
            Object routeObj = json.get("route");
            if (routeObj == null || routeObj.toString().equals("null")) {
                return 0;
            }

            Map<String, Object> routeMap = (Map<String, Object>) routeObj;
            String path          = (String) routeMap.get("path");
            String name          = (String) routeMap.getOrDefault("name", "");
            String componentName = (String) json.get("name");
            boolean requiresAuth = Boolean.TRUE.equals(routeMap.get("requiresAuth"));
            String layout        = (String) routeMap.getOrDefault("layout", "default");
            String title         = (String) json.getOrDefault("title", name);

            if (path == null || path.isEmpty()) {
                log.warn("JavaLive RouteLoader: Schema {} has empty route path, skipping.",
                         resource.getFilename());
                return 0;
            }

            PageRegistry.RouteDefinition route = new PageRegistry.RouteDefinition(
                path, name, componentName, requiresAuth, layout, title
            );
            pageRegistry.register(route);
            log.debug("JavaLive RouteLoader: Registered route [{} → {}]", path, componentName);
            return 1;
        }
    }
}
