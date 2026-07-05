package io.javalive.core.routing;

import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Runtime registry of all @VuePage route definitions.
 *
 * <p>Populated at startup from the generated router schema JSON file.
 * Used by {@link JavaLivePageController} to handle initial page loads
 * and serve the correct component for each URL path.
 *
 * @author JavaLive Team
 * @since 1.0.0
 */
@Component
public class PageRegistry {

    /** All registered routes: path → RouteDefinition. */
    private final Map<String, RouteDefinition> routes = new LinkedHashMap<>();

    // ─────────────────────────────────────────────────────────────────────────
    // Registration
    // ─────────────────────────────────────────────────────────────────────────

    public void register(RouteDefinition route) {
        routes.put(route.getPath(), route);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Lookup
    // ─────────────────────────────────────────────────────────────────────────

    public Optional<RouteDefinition> findByPath(String path) {
        // Exact match first
        if (routes.containsKey(path)) {
            return Optional.of(routes.get(path));
        }

        // Pattern match (for paths like /users/:id)
        for (RouteDefinition route : routes.values()) {
            if (pathMatches(route.getPath(), path)) {
                return Optional.of(route);
            }
        }

        return Optional.empty();
    }

    public Collection<RouteDefinition> getAllRoutes() {
        return Collections.unmodifiableCollection(routes.values());
    }

    public int getRouteCount() {
        return routes.size();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Private helpers
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Checks if a concrete path matches a route pattern.
     * Supports single-level dynamic segments ({@code :param}).
     * e.g., "/users/:id" matches "/users/42"
     */
    private boolean pathMatches(String pattern, String concrete) {
        String[] patternParts = pattern.split("/");
        String[] concreteParts = concrete.split("/");

        if (patternParts.length != concreteParts.length) return false;

        for (int i = 0; i < patternParts.length; i++) {
            String pp = patternParts[i];
            String cp = concreteParts[i];
            if (!pp.startsWith(":") && !pp.equals(cp)) {
                return false;
            }
        }

        return true;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Nested: RouteDefinition
    // ─────────────────────────────────────────────────────────────────────────

    /** Represents a single @VuePage route entry. */
    public static class RouteDefinition {
        private String path;
        private String name;
        private String componentName;
        private boolean requiresAuth;
        private String layout;
        private String title;

        public RouteDefinition() {}

        public RouteDefinition(String path, String name, String componentName,
                               boolean requiresAuth, String layout, String title) {
            this.path          = path;
            this.name          = name;
            this.componentName = componentName;
            this.requiresAuth  = requiresAuth;
            this.layout        = layout;
            this.title         = title;
        }

        // Getters and setters
        public String getPath()          { return path; }
        public void setPath(String p)    { this.path = p; }
        public String getName()          { return name; }
        public void setName(String n)    { this.name = n; }
        public String getComponentName() { return componentName; }
        public void setComponentName(String c) { this.componentName = c; }
        public boolean isRequiresAuth()  { return requiresAuth; }
        public void setRequiresAuth(boolean a) { this.requiresAuth = a; }
        public String getLayout()        { return layout; }
        public void setLayout(String l)  { this.layout = l; }
        public String getTitle()         { return title; }
        public void setTitle(String t)   { this.title = t; }

        @Override
        public String toString() {
            return "RouteDefinition{path='" + path + "', component='" + componentName + "'}";
        }
    }
}
