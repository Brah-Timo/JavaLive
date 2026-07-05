package io.javalive.core.routing;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link PageRegistry}.
 * Covers register, findByPath (exact + pattern), getAllRoutes, getRouteCount.
 */
@DisplayName("PageRegistry")
class PageRegistryTest {

    private PageRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new PageRegistry();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // register() and getRouteCount()
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("register() and getRouteCount()")
    class RegisterAndCount {

        @Test
        @DisplayName("initially empty")
        void initiallyEmpty() {
            assertEquals(0, registry.getRouteCount());
        }

        @Test
        @DisplayName("count increases after registration")
        void countIncreasesAfterRegister() {
            registry.register(makeRoute("/dashboard", "dashboard", "dashboard"));
            registry.register(makeRoute("/users", "users", "user-management"));
            assertEquals(2, registry.getRouteCount());
        }

        @Test
        @DisplayName("registering same path overwrites previous route")
        void samePathOverwritesPrevious() {
            registry.register(makeRoute("/page", "old-name", "old-comp"));
            registry.register(makeRoute("/page", "new-name", "new-comp"));
            assertEquals(1, registry.getRouteCount());
            assertEquals("new-comp",
                registry.findByPath("/page").orElseThrow().getComponentName());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // findByPath() — exact match
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("findByPath() — exact match")
    class FindByPathExact {

        @BeforeEach
        void registerRoutes() {
            registry.register(makeRoute("/dashboard", "dashboard", "dashboard-comp"));
            registry.register(makeRoute("/users", "users", "user-management"));
        }

        @Test
        @DisplayName("finds registered path exactly")
        void findsExactPath() {
            Optional<PageRegistry.RouteDefinition> result = registry.findByPath("/dashboard");
            assertTrue(result.isPresent());
            assertEquals("dashboard-comp", result.get().getComponentName());
        }

        @Test
        @DisplayName("returns empty for unknown path")
        void returnsEmptyForUnknownPath() {
            Optional<PageRegistry.RouteDefinition> result = registry.findByPath("/unknown");
            assertFalse(result.isPresent());
        }

        @Test
        @DisplayName("path is case-sensitive")
        void pathIsCaseSensitive() {
            Optional<PageRegistry.RouteDefinition> result = registry.findByPath("/DASHBOARD");
            assertFalse(result.isPresent());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // findByPath() — pattern match (:param)
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("findByPath() — pattern match")
    class FindByPathPattern {

        @BeforeEach
        void registerPatternRoute() {
            registry.register(makeRoute("/users/:id", "user-detail", "user-detail-comp"));
        }

        @Test
        @DisplayName("matches /users/:id with concrete /users/42")
        void matchesConcreteId() {
            Optional<PageRegistry.RouteDefinition> result = registry.findByPath("/users/42");
            assertTrue(result.isPresent());
            assertEquals("user-detail-comp", result.get().getComponentName());
        }

        @Test
        @DisplayName("matches /users/:id with string id /users/alice")
        void matchesStringId() {
            Optional<PageRegistry.RouteDefinition> result = registry.findByPath("/users/alice");
            assertTrue(result.isPresent());
        }

        @Test
        @DisplayName("does NOT match path with too many segments")
        void doesNotMatchTooManySegments() {
            Optional<PageRegistry.RouteDefinition> result = registry.findByPath("/users/42/profile");
            assertFalse(result.isPresent());
        }

        @Test
        @DisplayName("exact match takes precedence over pattern")
        void exactMatchTakesPrecedence() {
            registry.register(makeRoute("/users/special", "special-user", "special-comp"));
            // /users/:id pattern is already registered, but /users/special is exact

            Optional<PageRegistry.RouteDefinition> result = registry.findByPath("/users/special");
            assertTrue(result.isPresent());
            // Should find the exact match "special-comp"
            assertEquals("special-comp", result.get().getComponentName());
        }

        @Test
        @DisplayName("multi-segment pattern /a/:x/:y matches /a/1/2")
        void multiSegmentPattern() {
            registry.register(makeRoute("/items/:cat/:id", "item-detail", "item-comp"));
            Optional<PageRegistry.RouteDefinition> result = registry.findByPath("/items/books/99");
            assertTrue(result.isPresent());
            assertEquals("item-comp", result.get().getComponentName());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // getAllRoutes()
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getAllRoutes()")
    class GetAllRoutes {

        @Test
        @DisplayName("returns empty collection when no routes registered")
        void emptyWhenNoRoutes() {
            assertTrue(registry.getAllRoutes().isEmpty());
        }

        @Test
        @DisplayName("returns all registered routes")
        void returnsAllRoutes() {
            registry.register(makeRoute("/a", "a", "comp-a"));
            registry.register(makeRoute("/b", "b", "comp-b"));
            registry.register(makeRoute("/c", "c", "comp-c"));

            Collection<PageRegistry.RouteDefinition> all = registry.getAllRoutes();
            assertEquals(3, all.size());
        }

        @Test
        @DisplayName("returned collection is unmodifiable")
        void returnedCollectionIsUnmodifiable() {
            registry.register(makeRoute("/a", "a", "comp-a"));
            Collection<PageRegistry.RouteDefinition> all = registry.getAllRoutes();
            assertThrows(UnsupportedOperationException.class,
                () -> all.add(makeRoute("/injected", "x", "x")));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // RouteDefinition inner class
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("RouteDefinition")
    class RouteDefinitionTest {

        @Test
        @DisplayName("all fields are set via constructor and retrievable")
        void constructorAndGetters() {
            PageRegistry.RouteDefinition route = new PageRegistry.RouteDefinition(
                "/test", "test-route", "test-comp", true, "admin", "Test Page");

            assertEquals("/test", route.getPath());
            assertEquals("test-route", route.getName());
            assertEquals("test-comp", route.getComponentName());
            assertTrue(route.isRequiresAuth());
            assertEquals("admin", route.getLayout());
            assertEquals("Test Page", route.getTitle());
        }

        @Test
        @DisplayName("all setters work correctly")
        void settersWork() {
            PageRegistry.RouteDefinition route = new PageRegistry.RouteDefinition();
            route.setPath("/new");
            route.setName("new-route");
            route.setComponentName("new-comp");
            route.setRequiresAuth(false);
            route.setLayout("default");
            route.setTitle("New Page");

            assertEquals("/new", route.getPath());
            assertEquals("new-route", route.getName());
            assertEquals("new-comp", route.getComponentName());
            assertFalse(route.isRequiresAuth());
            assertEquals("default", route.getLayout());
            assertEquals("New Page", route.getTitle());
        }

        @Test
        @DisplayName("toString contains path and component")
        void toStringContainsPathAndComp() {
            PageRegistry.RouteDefinition route = makeRoute("/dashboard", "dash", "dash-comp");
            String str = route.toString();
            assertTrue(str.contains("/dashboard"));
            assertTrue(str.contains("dash-comp"));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helper
    // ─────────────────────────────────────────────────────────────────────────

    private PageRegistry.RouteDefinition makeRoute(String path, String name, String componentName) {
        return new PageRegistry.RouteDefinition(path, name, componentName, false, "default", name);
    }
}
