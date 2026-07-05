package io.javalive.core.routing;

import io.javalive.core.rendering.PageComposer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpSession;
import java.util.Map;

/**
 * Catch-all Spring MVC controller that handles initial page load requests
 * for all @VuePage routes.
 *
 * <p>When a browser navigates to any registered @VuePage path (e.g., {@code /dashboard}),
 * this controller:
 * <ol>
 *   <li>Looks up the route definition in {@link PageRegistry}</li>
 *   <li>Builds the initial HTML with embedded hydration data</li>
 *   <li>Returns the HTML response</li>
 * </ol>
 *
 * <p>All subsequent navigation (within the SPA) is handled by Vue Router on the client.
 *
 * @author JavaLive Team
 * @since 1.0.0
 */
@Controller
public class JavaLivePageController {

    private static final Logger log = LoggerFactory.getLogger(JavaLivePageController.class);

    @Autowired
    private PageRegistry pageRegistry;

    @Autowired
    private PageComposer pageComposer;

    /**
     * Handles all GET requests. Returns HTML for @VuePage routes,
     * and 404 for unknown paths.
     */
    @GetMapping(value = {"/**"},
                produces = MediaType.TEXT_HTML_VALUE)
    @ResponseBody
    public ResponseEntity<String> handlePageRequest(
            @RequestAttribute(name = "requestPath", required = false) String requestPath,
            HttpSession httpSession,
            jakarta.servlet.http.HttpServletRequest request) {

        String path = request.getRequestURI();

        // Skip static resources
        if (isStaticResource(path)) {
            return ResponseEntity.notFound().build();
        }

        // Look up the route
        return pageRegistry.findByPath(path)
            .map(route -> {
                String sessionId = httpSession.getId();
                String html = pageComposer.composeHtmlString(
                    route.getComponentName(),
                    route.getTitle() != null ? route.getTitle() : route.getName(),
                    sessionId
                );
                log.debug("JavaLive: Serving page [{} → {}]", path, route.getComponentName());
                return ResponseEntity.ok()
                    .contentType(MediaType.TEXT_HTML)
                    .body(html);
            })
            .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Health check and JavaLive status endpoint.
     * Declared with a more specific path so it takes precedence over the /** catch-all.
     */
    @GetMapping(value = "/api/javalive/status", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getStatus() {
        return ResponseEntity.ok(Map.of(
            "status", "running",
            "version", "0.1.0-alpha",
            "registeredRoutes", pageRegistry.getRouteCount(),
            "routes", pageRegistry.getAllRoutes().stream()
                .map(r -> r.getPath() + " → " + r.getComponentName())
                .toList()
        ));
    }

    /**
     * Alias at original path for backwards compatibility.
     */
    @GetMapping(value = "/javalive/status")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getStatusAlias() {
        return getStatus();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Private helpers
    // ─────────────────────────────────────────────────────────────────────────

    private boolean isStaticResource(String path) {
        return path.startsWith("/static/")
            || path.startsWith("/javalive/")
            || path.startsWith("/actuator/")
            || path.startsWith("/api/")
            || path.endsWith(".js")
            || path.endsWith(".css")
            || path.endsWith(".png")
            || path.endsWith(".ico")
            || path.endsWith(".json")
            || path.equals("/favicon.ico");
    }
}
