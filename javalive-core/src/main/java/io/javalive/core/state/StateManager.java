package io.javalive.core.state;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

/**
 * Central state management hub for JavaLive.
 *
 * <p>Maintains three distinct storage layers:
 * <ol>
 *   <li><b>Session state</b>: per-user, per-component state (ConcurrentHashMap)</li>
 *   <li><b>Global state</b>: shared across all sessions for global-scoped fields</li>
 *   <li><b>Schema registry</b>: component schemas loaded from generated JSON files</li>
 * </ol>
 *
 * <p>Thread-safe by design. All public methods can be called concurrently
 * from multiple WebSocket handler threads.
 *
 * <h3>State structure:</h3>
 * <pre>
 * sessionStates: {
 *   "session-abc-123": {
 *     "dashboard": { "count": 5, "title": "Hello" },
 *     "user-list": { "users": [...], "page": 1 }
 *   },
 *   "session-def-456": {
 *     "dashboard": { "count": 0, "title": "Hello" }
 *   }
 * }
 *
 * globalStates: {
 *   "live-dashboard": { "activeUsers": 42, "totalOrders": 1337 }
 * }
 * </pre>
 *
 * @author JavaLive Team
 * @since 1.0.0
 */
@Component
public class StateManager {

    private static final Logger log = LoggerFactory.getLogger(StateManager.class);

    /**
     * Session-scoped state: sessionId → componentName → state map.
     * Uses nested ConcurrentHashMaps for thread-safe access.
     */
    private final Map<String, Map<String, Map<String, Object>>> sessionStates
        = new ConcurrentHashMap<>();

    /**
     * Global-scoped state: componentName → state map.
     * Shared across ALL connected sessions.
     */
    private final Map<String, Map<String, Object>> globalStates
        = new ConcurrentHashMap<>();

    /**
     * State schemas: componentName → StateSchema.
     * Loaded at startup from generated JSON schema files.
     */
    private final Map<String, StateSchema> schemas = new ConcurrentHashMap<>();

    /**
     * State change listeners: notified after any state mutation.
     */
    private final List<BiConsumer<String, StateDiff>> listeners = new ArrayList<>();

    // ─────────────────────────────────────────────────────────────────────────
    // Read operations
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Gets the current state for a session-component pair.
     * Returns an empty map if no state exists yet.
     *
     * @param sessionId     the STOMP session ID
     * @param componentName the component's kebab-case name
     * @return the current state (never null, may be empty)
     */
    public Map<String, Object> getState(String sessionId, String componentName) {
        return sessionStates
            .getOrDefault(sessionId, Collections.emptyMap())
            .getOrDefault(componentName, Collections.emptyMap());
    }

    /**
     * Gets or creates state for a session-component pair.
     * If no state exists, initializes it from the component's schema.
     *
     * @param sessionId     the STOMP session ID
     * @param componentName the component's kebab-case name
     * @return the existing or newly created state map
     */
    public Map<String, Object> getOrCreate(String sessionId, String componentName) {
        return sessionStates
            .computeIfAbsent(sessionId, k -> new ConcurrentHashMap<>())
            .computeIfAbsent(componentName, k -> {
                // Initialize from schema (no reflection needed!)
                StateSchema schema = schemas.get(componentName);
                if (schema != null) {
                    Map<String, Object> initial = schema.createInitialState();
                    // Merge global state into session state for global fields
                    mergeGlobalState(componentName, initial);
                    log.debug("JavaLive: Created initial state for [{}/{}] from schema",
                              sessionId, componentName);
                    return new ConcurrentHashMap<>(initial);
                }
                log.debug("JavaLive: No schema found for component '{}', using empty state",
                          componentName);
                return new ConcurrentHashMap<>();
            });
    }

    /**
     * Gets the global state for a component.
     * Used for @Reactive(scope="global") fields.
     */
    public Map<String, Object> getGlobalState(String componentName) {
        return globalStates.getOrDefault(componentName, Collections.emptyMap());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Write operations
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Updates state for a specific session-component pair and notifies listeners.
     *
     * @param sessionId     the STOMP session ID
     * @param componentName the component name
     * @param newState      the complete new state to apply
     */
    public void setState(String sessionId, String componentName,
                         Map<String, Object> newState) {
        if (newState == null) return;

        Map<String, Object> current = getOrCreate(sessionId, componentName);
        Map<String, Object> oldState = new HashMap<>(current);

        // Apply new state
        current.putAll(newState);

        // Remove keys that are no longer in newState
        current.keySet().retainAll(newState.keySet());

        // Notify listeners
        StateDiff diff = StateDiff.compute(oldState, current);
        if (!diff.isEmpty()) {
            listeners.forEach(l -> l.accept(sessionId, diff));
        }

        // Update global state for global-scoped fields
        StateSchema schema = schemas.get(componentName);
        if (schema != null && schema.hasGlobalFields()) {
            updateGlobalState(componentName, newState, schema);
        }
    }

    /**
     * Patches (merges) state for a specific session-component pair.
     * Only the provided keys are updated; others remain unchanged.
     *
     * @param sessionId     the STOMP session ID
     * @param componentName the component name
     * @param patch         the partial state to merge
     */
    public void patchState(String sessionId, String componentName,
                           Map<String, Object> patch) {
        if (patch == null || patch.isEmpty()) return;

        Map<String, Object> current = getOrCreate(sessionId, componentName);
        Map<String, Object> oldState = new HashMap<>(current);
        current.putAll(patch);

        StateDiff diff = StateDiff.compute(oldState, current);
        if (!diff.isEmpty()) {
            listeners.forEach(l -> l.accept(sessionId, diff));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Cleanup
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Removes all state associated with a disconnected session.
     * Called by {@link io.javalive.core.websocket.LiveWebSocketHandler} on disconnect.
     *
     * @param sessionId the session to clean up
     */
    public void cleanupSession(String sessionId) {
        sessionStates.remove(sessionId);
        log.debug("JavaLive: Cleaned up state for session [{}]", sessionId);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Schema registry
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Registers a component's state schema.
     * Called at application startup for each generated schema JSON file.
     *
     * @param componentName the component name
     * @param schema        the schema to register
     */
    public void registerSchema(String componentName, StateSchema schema) {
        schemas.put(componentName, schema);
        log.debug("JavaLive: Registered state schema for component '{}'", componentName);

        // Pre-initialize global state from schema defaults
        if (schema.hasGlobalFields()) {
            globalStates.computeIfAbsent(componentName, k -> {
                Map<String, Object> globalInitial = new ConcurrentHashMap<>();
                schema.getGlobalFields().forEach(f ->
                    globalInitial.put(f.getName(), f.getDefaultValue()));
                return globalInitial;
            });
        }
    }

    public Optional<StateSchema> getSchema(String componentName) {
        return Optional.ofNullable(schemas.get(componentName));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Listeners
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Registers a listener that is notified of state changes.
     *
     * @param listener a BiConsumer receiving (sessionId, StateDiff)
     */
    public void addListener(BiConsumer<String, StateDiff> listener) {
        listeners.add(listener);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Private helpers
    // ─────────────────────────────────────────────────────────────────────────

    /** Merges global state values into a session state map for global-scoped fields. */
    private void mergeGlobalState(String componentName, Map<String, Object> sessionState) {
        Map<String, Object> global = globalStates.get(componentName);
        if (global != null) {
            sessionState.putAll(global);
        }
    }

    /** Updates the global state when global-scoped fields change. */
    private void updateGlobalState(String componentName,
                                   Map<String, Object> newState,
                                   StateSchema schema) {
        Map<String, Object> global = globalStates.computeIfAbsent(
            componentName, k -> new ConcurrentHashMap<>());

        schema.getGlobalFields().forEach(field -> {
            if (newState.containsKey(field.getName())) {
                global.put(field.getName(), newState.get(field.getName()));
            }
        });
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Monitoring
    // ─────────────────────────────────────────────────────────────────────────

    public int getActiveSessionCount() {
        return sessionStates.size();
    }

    public int getRegisteredSchemaCount() {
        return schemas.size();
    }

    public Set<String> getRegisteredComponentNames() {
        return Collections.unmodifiableSet(schemas.keySet());
    }
}
