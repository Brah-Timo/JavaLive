package io.javalive.core.state;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link StateManager}.
 * Covers getOrCreate, getState, setState, patchState, cleanupSession,
 * registerSchema, addListener, and monitoring methods.
 */
@DisplayName("StateManager")
class StateManagerTest {

    private StateManager stateManager;

    @BeforeEach
    void setUp() {
        stateManager = new StateManager();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // getState() — before any state is set
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getState()")
    class GetState {

        @Test
        @DisplayName("returns empty map for unknown session")
        void returnsEmptyForUnknownSession() {
            Map<String, Object> state = stateManager.getState("unknown-session", "component");
            assertNotNull(state);
            assertTrue(state.isEmpty());
        }

        @Test
        @DisplayName("returns empty map for unknown component in existing session")
        void returnsEmptyForUnknownComponent() {
            stateManager.getOrCreate("sess-1", "comp-a");
            Map<String, Object> state = stateManager.getState("sess-1", "comp-b");
            assertTrue(state.isEmpty());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // getOrCreate()
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getOrCreate()")
    class GetOrCreate {

        @Test
        @DisplayName("creates empty state when no schema is registered")
        void createsEmptyStateWithoutSchema() {
            Map<String, Object> state = stateManager.getOrCreate("sess-1", "unknown-component");
            assertNotNull(state);
            assertTrue(state.isEmpty());
        }

        @Test
        @DisplayName("initializes state from schema when schema is registered")
        void initializesFromSchema() {
            StateSchema schema = buildSchema("counter", "count", "int", 0, "session");
            stateManager.registerSchema("counter", schema);

            Map<String, Object> state = stateManager.getOrCreate("sess-1", "counter");
            assertEquals(0, state.get("count"));
        }

        @Test
        @DisplayName("returns same map on second call (idempotent)")
        void idempotentOnSecondCall() {
            Map<String, Object> first  = stateManager.getOrCreate("sess-1", "counter");
            first.put("key", "value");
            Map<String, Object> second = stateManager.getOrCreate("sess-1", "counter");
            assertEquals("value", second.get("key"));
            assertSame(first, second);
        }

        @Test
        @DisplayName("different sessions get independent state objects")
        void differentSessionsHaveIndependentState() {
            StateSchema schema = buildSchema("counter", "count", "int", 0, "session");
            stateManager.registerSchema("counter", schema);

            Map<String, Object> state1 = stateManager.getOrCreate("sess-1", "counter");
            Map<String, Object> state2 = stateManager.getOrCreate("sess-2", "counter");

            state1.put("count", 99);
            assertEquals(0, state2.get("count")); // session 2 unaffected
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // setState()
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("setState()")
    class SetState {

        @Test
        @DisplayName("sets state correctly")
        void setsStateCorrectly() {
            Map<String, Object> newState = Map.of("count", 10, "name", "Bob");
            stateManager.setState("sess-1", "comp", newState);

            Map<String, Object> state = stateManager.getState("sess-1", "comp");
            assertEquals(10, state.get("count"));
            assertEquals("Bob", state.get("name"));
        }

        @Test
        @DisplayName("null newState is a no-op")
        void nullNewStateIsNoOp() {
            stateManager.getOrCreate("sess-1", "comp"); // initialize
            stateManager.setState("sess-1", "comp", null); // should not throw
            assertDoesNotThrow(() -> stateManager.getState("sess-1", "comp"));
        }

        @Test
        @DisplayName("replaces entire state (removes old keys not in new state)")
        void replacesEntireState() {
            stateManager.setState("sess-1", "comp", Map.of("a", 1, "b", 2));
            stateManager.setState("sess-1", "comp", Map.of("a", 99));

            Map<String, Object> state = stateManager.getState("sess-1", "comp");
            assertEquals(99, state.get("a"));
            assertFalse(state.containsKey("b")); // b was removed
        }

        @Test
        @DisplayName("notifies listeners when state changes")
        void notifiesListeners() {
            AtomicReference<String> notifiedSession = new AtomicReference<>();
            AtomicReference<StateDiff> notifiedDiff = new AtomicReference<>();

            stateManager.addListener((sessionId, diff) -> {
                notifiedSession.set(sessionId);
                notifiedDiff.set(diff);
            });

            stateManager.setState("sess-1", "comp", Map.of("count", 5));
            stateManager.setState("sess-1", "comp", Map.of("count", 10));

            assertEquals("sess-1", notifiedSession.get());
            assertNotNull(notifiedDiff.get());
            assertTrue(notifiedDiff.get().getChanged().containsKey("count"));
        }

        @Test
        @DisplayName("does NOT notify listeners when nothing changed")
        void doesNotNotifyWhenUnchanged() {
            stateManager.setState("sess-1", "comp", Map.of("count", 5));

            AtomicReference<StateDiff> notifiedDiff = new AtomicReference<>(null);
            stateManager.addListener((sessionId, diff) -> notifiedDiff.set(diff));

            // same state again — should not trigger listener
            stateManager.setState("sess-1", "comp", Map.of("count", 5));
            assertNull(notifiedDiff.get());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // patchState()
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("patchState()")
    class PatchState {

        @Test
        @DisplayName("merges partial state without removing other keys")
        void mergesPartialState() {
            stateManager.setState("sess-1", "comp", new HashMap<>(Map.of("a", 1, "b", 2)));
            stateManager.patchState("sess-1", "comp", Map.of("b", 99));

            Map<String, Object> state = stateManager.getState("sess-1", "comp");
            assertEquals(1, state.get("a")); // a preserved
            assertEquals(99, state.get("b")); // b updated
        }

        @Test
        @DisplayName("null patch is a no-op")
        void nullPatchIsNoOp() {
            stateManager.setState("sess-1", "comp", Map.of("count", 5));
            assertDoesNotThrow(() -> stateManager.patchState("sess-1", "comp", null));
            assertEquals(5, stateManager.getState("sess-1", "comp").get("count"));
        }

        @Test
        @DisplayName("empty patch is a no-op")
        void emptyPatchIsNoOp() {
            stateManager.setState("sess-1", "comp", Map.of("count", 5));
            stateManager.patchState("sess-1", "comp", Collections.emptyMap());
            assertEquals(5, stateManager.getState("sess-1", "comp").get("count"));
        }

        @Test
        @DisplayName("notifies listeners on actual change")
        void notifiesOnChange() {
            stateManager.setState("sess-1", "comp", new HashMap<>(Map.of("count", 0)));

            AtomicReference<StateDiff> captured = new AtomicReference<>();
            stateManager.addListener((sid, diff) -> captured.set(diff));

            stateManager.patchState("sess-1", "comp", Map.of("count", 1));
            assertNotNull(captured.get());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // cleanupSession()
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("cleanupSession()")
    class CleanupSession {

        @Test
        @DisplayName("removes all state for the session")
        void removesSessionState() {
            stateManager.setState("sess-1", "comp", Map.of("count", 5));
            stateManager.cleanupSession("sess-1");

            Map<String, Object> state = stateManager.getState("sess-1", "comp");
            assertTrue(state.isEmpty());
        }

        @Test
        @DisplayName("cleanup of non-existent session is safe")
        void cleanupNonExistentIsSafe() {
            assertDoesNotThrow(() -> stateManager.cleanupSession("never-existed"));
        }

        @Test
        @DisplayName("cleanup of one session does not affect others")
        void doesNotAffectOtherSessions() {
            stateManager.setState("sess-1", "comp", Map.of("count", 5));
            stateManager.setState("sess-2", "comp", Map.of("count", 99));

            stateManager.cleanupSession("sess-1");

            assertEquals(99, stateManager.getState("sess-2", "comp").get("count"));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // registerSchema()
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("registerSchema()")
    class RegisterSchema {

        @Test
        @DisplayName("getSchema returns registered schema")
        void registeredSchemaIsRetrievable() {
            StateSchema schema = new StateSchema();
            schema.setComponentName("my-comp");
            stateManager.registerSchema("my-comp", schema);

            Optional<StateSchema> result = stateManager.getSchema("my-comp");
            assertTrue(result.isPresent());
            assertEquals("my-comp", result.get().getComponentName());
        }

        @Test
        @DisplayName("getSchema returns empty for unregistered component")
        void unregisteredReturnsEmpty() {
            assertTrue(stateManager.getSchema("nope").isEmpty());
        }

        @Test
        @DisplayName("getRegisteredSchemaCount increments")
        void schemaCountIncrements() {
            assertEquals(0, stateManager.getRegisteredSchemaCount());
            stateManager.registerSchema("c1", new StateSchema());
            stateManager.registerSchema("c2", new StateSchema());
            assertEquals(2, stateManager.getRegisteredSchemaCount());
        }

        @Test
        @DisplayName("registered component name appears in getRegisteredComponentNames")
        void componentNameAppears() {
            stateManager.registerSchema("widget", new StateSchema());
            assertTrue(stateManager.getRegisteredComponentNames().contains("widget"));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Monitoring
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Monitoring methods")
    class Monitoring {

        @Test
        @DisplayName("getActiveSessionCount increases as sessions are added")
        void sessionCountTracked() {
            assertEquals(0, stateManager.getActiveSessionCount());
            stateManager.setState("s1", "comp", Map.of("a", 1));
            stateManager.setState("s2", "comp", Map.of("a", 1));
            assertEquals(2, stateManager.getActiveSessionCount());
        }

        @Test
        @DisplayName("getActiveSessionCount decreases after cleanup")
        void sessionCountDecreasesAfterCleanup() {
            stateManager.setState("s1", "comp", Map.of("a", 1));
            stateManager.cleanupSession("s1");
            assertEquals(0, stateManager.getActiveSessionCount());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────

    private StateSchema buildSchema(String componentName, String fieldName,
                                    String javaType, Object defaultValue, String scope) {
        StateSchema schema = new StateSchema();
        schema.setComponentName(componentName);

        StateSchema.FieldSchema field = new StateSchema.FieldSchema();
        field.setName(fieldName);
        field.setJavaType(javaType);
        field.setDefaultValue(defaultValue);
        field.setScope(scope);

        schema.setFields(List.of(field));
        return schema;
    }
}
