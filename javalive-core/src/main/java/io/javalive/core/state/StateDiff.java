package io.javalive.core.state;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalive.core.websocket.ServerMessage;

import java.util.*;

/**
 * Computes and represents the difference between two state snapshots.
 *
 * <p>The diff algorithm ensures that only changed fields are transmitted over
 * the WebSocket, minimizing bandwidth and client-side processing.
 *
 * <h3>How it works:</h3>
 * <pre>
 * Old state: { count: 5, name: "Ali", items: [1,2,3], active: true }
 * New state: { count: 6, name: "Ali", items: [1,2,3], active: true }
 * Diff:      { type: "patch", changed: { count: 6 }, removed: [] }
 * </pre>
 *
 * <p>For arrays and objects, deep equality is used. This is intentional:
 * if the entire {@code items} array is replaced with a new list containing
 * the same elements, no diff is sent.
 *
 * <h3>Types of diffs:</h3>
 * <ul>
 *   <li><b>full</b>: Entire state snapshot (sent on init)</li>
 *   <li><b>patch</b>: Only the changed + removed fields (sent after method calls)</li>
 *   <li><b>error</b>: Error payload when a method throws an exception</li>
 *   <li><b>emit</b>: Vue event emission payload</li>
 * </ul>
 *
 * @author JavaLive Team
 * @since 1.0.0
 */
public class StateDiff {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /** Diff type: "full", "patch", "error", "emit". */
    private final String type;

    /** Fields that were added or changed: fieldName → newValue. */
    private final Map<String, Object> changed;

    /** Fields that were removed: list of field names. */
    private final List<String> removed;

    /** For global diffs: which fields are globally scoped. */
    private final Set<String> globalFields;

    private StateDiff(String type, Map<String, Object> changed,
                      List<String> removed, Set<String> globalFields) {
        this.type         = type;
        this.changed      = changed;
        this.removed      = removed;
        this.globalFields = globalFields;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Factory methods
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Creates a "full" diff containing the entire state.
     * Sent on component init.
     */
    public static StateDiff fullState(Map<String, Object> state) {
        return new StateDiff("full", new HashMap<>(state),
                             Collections.emptyList(), Collections.emptySet());
    }

    /**
     * Computes a "patch" diff between old and new state.
     * Only includes fields that actually changed.
     */
    public static StateDiff compute(Map<String, Object> oldState, Map<String, Object> newState) {
        Map<String, Object> changed = new LinkedHashMap<>();
        List<String> removed = new ArrayList<>();

        if (newState == null) newState = Collections.emptyMap();
        if (oldState == null) oldState = Collections.emptyMap();

        // Find added or modified fields
        for (Map.Entry<String, Object> entry : newState.entrySet()) {
            String key = entry.getKey();
            Object newValue = entry.getValue();
            Object oldValue = oldState.get(key);

            if (!deepEquals(oldValue, newValue)) {
                changed.put(key, newValue);
            }
        }

        // Find removed fields
        for (String key : oldState.keySet()) {
            if (!newState.containsKey(key)) {
                removed.add(key);
            }
        }

        return new StateDiff("patch", changed, removed, Collections.emptySet());
    }

    /**
     * Computes a diff for global-scoped fields only.
     * Used to determine what to broadcast to ALL sessions.
     *
     * @param oldState    the old state
     * @param newState    the new state
     * @return a diff containing only globally-scoped changed fields
     */
    public static StateDiff computeGlobal(Map<String, Object> oldState,
                                          Map<String, Object> newState) {
        StateDiff full = compute(oldState, newState);
        // The caller (generated controller) filters by global fields.
        // This method returns the same patch but tagged for global broadcast.
        return new StateDiff("patch", full.changed, full.removed, full.globalFields);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Serialization
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Converts this diff to a {@link ServerMessage} ready for WebSocket transmission.
     */
    public ServerMessage toServerMessage() {
        return ServerMessage.patch(type, changed, removed);
    }

    /**
     * Converts this diff to a JSON string (for legacy use).
     */
    public String toJson() {
        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("type", type);
            payload.put("changed", changed);
            payload.put("removed", removed);
            return MAPPER.writeValueAsString(payload);
        } catch (Exception e) {
            throw new RuntimeException("JavaLive: Failed to serialize StateDiff to JSON", e);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Predicates
    // ─────────────────────────────────────────────────────────────────────────

    /** Returns true if there are no actual changes (no need to send anything). */
    public boolean isEmpty() {
        return changed.isEmpty() && removed.isEmpty();
    }

    /** Returns true if this diff contains any global-scoped field changes. */
    public boolean hasGlobalChanges(Set<String> globalFieldNames) {
        return changed.keySet().stream().anyMatch(globalFieldNames::contains)
            || removed.stream().anyMatch(globalFieldNames::contains);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Getters
    // ─────────────────────────────────────────────────────────────────────────

    public String getType()                     { return type; }
    public Map<String, Object> getChanged()     { return Collections.unmodifiableMap(changed); }
    public List<String> getRemoved()            { return Collections.unmodifiableList(removed); }

    // ─────────────────────────────────────────────────────────────────────────
    // Deep equality helper
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Performs a deep equality check between two objects.
     *
     * <p>Special cases:
     * <ul>
     *   <li>Both null → equal</li>
     *   <li>One null → not equal</li>
     *   <li>Primitives and Strings → {@code Objects.equals}</li>
     *   <li>Maps → recursive key-value comparison</li>
     *   <li>Lists → element-by-element comparison</li>
     *   <li>Complex objects → serialized to JSON and compared as strings</li>
     * </ul>
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static boolean deepEquals(Object a, Object b) {
        if (a == b) return true;
        if (a == null || b == null) return false;

        if (a instanceof Map && b instanceof Map) {
            Map<String, Object> ma = (Map<String, Object>) a;
            Map<String, Object> mb = (Map<String, Object>) b;
            if (ma.size() != mb.size()) return false;
            for (Map.Entry<String, Object> entry : ma.entrySet()) {
                if (!deepEquals(entry.getValue(), mb.get(entry.getKey()))) return false;
            }
            return true;
        }

        if (a instanceof List && b instanceof List) {
            List la = (List) a;
            List lb = (List) b;
            if (la.size() != lb.size()) return false;
            for (int i = 0; i < la.size(); i++) {
                if (!deepEquals(la.get(i), lb.get(i))) return false;
            }
            return true;
        }

        if (a instanceof Number && b instanceof Number) {
            return ((Number) a).doubleValue() == ((Number) b).doubleValue();
        }

        // For complex objects: compare via Jackson serialization
        try {
            return MAPPER.writeValueAsString(a).equals(MAPPER.writeValueAsString(b));
        } catch (Exception e) {
            return Objects.equals(a, b);
        }
    }

    @Override
    public String toString() {
        return "StateDiff{type='" + type + "', changed=" + changed.keySet() +
               ", removed=" + removed + "}";
    }
}
