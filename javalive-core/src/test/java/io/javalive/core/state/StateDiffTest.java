package io.javalive.core.state;

import io.javalive.core.websocket.ServerMessage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link StateDiff}.
 * Covers fullState, compute, computeGlobal, isEmpty, hasGlobalChanges, toJson, toServerMessage.
 */
@DisplayName("StateDiff")
class StateDiffTest {

    // ─────────────────────────────────────────────────────────────────────────
    // fullState()
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("fullState()")
    class FullState {

        @Test
        @DisplayName("type is 'full'")
        void typeIsFull() {
            Map<String, Object> state = Map.of("count", 5, "name", "Alice");
            StateDiff diff = StateDiff.fullState(state);
            assertEquals("full", diff.getType());
        }

        @Test
        @DisplayName("all state fields are in changed map")
        void allFieldsInChanged() {
            Map<String, Object> state = Map.of("count", 5, "name", "Alice");
            StateDiff diff = StateDiff.fullState(state);
            assertEquals(5, diff.getChanged().get("count"));
            assertEquals("Alice", diff.getChanged().get("name"));
        }

        @Test
        @DisplayName("removed list is empty for fullState")
        void removedIsEmpty() {
            StateDiff diff = StateDiff.fullState(Map.of("x", 1));
            assertTrue(diff.getRemoved().isEmpty());
        }

        @Test
        @DisplayName("fullState with empty map still produces 'full' type")
        void emptyStateProducesFullType() {
            StateDiff diff = StateDiff.fullState(Collections.emptyMap());
            assertEquals("full", diff.getType());
            assertTrue(diff.getChanged().isEmpty());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // compute()
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("compute()")
    class Compute {

        @Test
        @DisplayName("type is 'patch'")
        void typeIsPatch() {
            StateDiff diff = StateDiff.compute(Map.of("a", 1), Map.of("a", 2));
            assertEquals("patch", diff.getType());
        }

        @Test
        @DisplayName("changed field is detected")
        void changedFieldDetected() {
            Map<String, Object> oldState = new HashMap<>();
            oldState.put("count", 5);
            oldState.put("name", "Alice");

            Map<String, Object> newState = new HashMap<>();
            newState.put("count", 6);
            newState.put("name", "Alice");

            StateDiff diff = StateDiff.compute(oldState, newState);
            assertTrue(diff.getChanged().containsKey("count"));
            assertEquals(6, diff.getChanged().get("count"));
            assertFalse(diff.getChanged().containsKey("name")); // unchanged
        }

        @Test
        @DisplayName("unchanged fields are NOT in changed map")
        void unchangedFieldsOmitted() {
            Map<String, Object> oldState = Map.of("count", 5, "name", "Alice");
            Map<String, Object> newState = Map.of("count", 5, "name", "Alice");

            StateDiff diff = StateDiff.compute(oldState, newState);
            assertTrue(diff.getChanged().isEmpty());
            assertTrue(diff.getRemoved().isEmpty());
        }

        @Test
        @DisplayName("new field addition is detected")
        void newFieldAdditionDetected() {
            Map<String, Object> oldState = Map.of("count", 5);
            Map<String, Object> newState = Map.of("count", 5, "newField", "hello");

            StateDiff diff = StateDiff.compute(oldState, newState);
            assertTrue(diff.getChanged().containsKey("newField"));
            assertEquals("hello", diff.getChanged().get("newField"));
        }

        @Test
        @DisplayName("removed field is detected")
        void removedFieldDetected() {
            Map<String, Object> oldState = new HashMap<>();
            oldState.put("count", 5);
            oldState.put("toBeRemoved", "bye");

            Map<String, Object> newState = new HashMap<>();
            newState.put("count", 5);

            StateDiff diff = StateDiff.compute(oldState, newState);
            assertTrue(diff.getRemoved().contains("toBeRemoved"));
        }

        @Test
        @DisplayName("null oldState treated as empty map")
        void nullOldStateIsEmpty() {
            StateDiff diff = StateDiff.compute(null, Map.of("a", 1));
            assertTrue(diff.getChanged().containsKey("a"));
        }

        @Test
        @DisplayName("null newState treated as empty map")
        void nullNewStateIsEmpty() {
            StateDiff diff = StateDiff.compute(Map.of("a", 1), null);
            assertTrue(diff.getRemoved().contains("a"));
        }

        @Test
        @DisplayName("list with same elements is NOT in changed")
        void sameListIsUnchanged() {
            Map<String, Object> oldState = new HashMap<>();
            oldState.put("items", Arrays.asList(1, 2, 3));

            Map<String, Object> newState = new HashMap<>();
            newState.put("items", Arrays.asList(1, 2, 3));

            StateDiff diff = StateDiff.compute(oldState, newState);
            assertFalse(diff.getChanged().containsKey("items"));
        }

        @Test
        @DisplayName("list with different elements IS in changed")
        void differentListIsChanged() {
            Map<String, Object> oldState = new HashMap<>();
            oldState.put("items", Arrays.asList(1, 2, 3));

            Map<String, Object> newState = new HashMap<>();
            newState.put("items", Arrays.asList(1, 2, 4));

            StateDiff diff = StateDiff.compute(oldState, newState);
            assertTrue(diff.getChanged().containsKey("items"));
        }

        @Test
        @DisplayName("nested map with same values is NOT in changed")
        void sameNestedMapIsUnchanged() {
            Map<String, Object> inner = Map.of("x", 1, "y", 2);
            Map<String, Object> oldState = new HashMap<>();
            oldState.put("nested", inner);
            Map<String, Object> newState = new HashMap<>();
            newState.put("nested", new HashMap<>(inner));

            StateDiff diff = StateDiff.compute(oldState, newState);
            assertFalse(diff.getChanged().containsKey("nested"));
        }

        @Test
        @DisplayName("both states empty → empty diff")
        void bothEmptyStateIsEmptyDiff() {
            StateDiff diff = StateDiff.compute(Collections.emptyMap(), Collections.emptyMap());
            assertTrue(diff.isEmpty());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // computeGlobal()
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("computeGlobal()")
    class ComputeGlobal {

        @Test
        @DisplayName("returns patch type diff")
        void returnsPatchType() {
            StateDiff diff = StateDiff.computeGlobal(
                Map.of("globalCount", 1),
                Map.of("globalCount", 2));
            assertEquals("patch", diff.getType());
        }

        @Test
        @DisplayName("detects changed fields")
        void detectsChanges() {
            StateDiff diff = StateDiff.computeGlobal(
                Map.of("globalCount", 10),
                Map.of("globalCount", 20));
            assertTrue(diff.getChanged().containsKey("globalCount"));
            assertEquals(20, diff.getChanged().get("globalCount"));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // isEmpty()
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("isEmpty()")
    class IsEmpty {

        @Test
        @DisplayName("no changes → isEmpty is true")
        void noChangesIsEmpty() {
            StateDiff diff = StateDiff.compute(Map.of("a", 1), Map.of("a", 1));
            assertTrue(diff.isEmpty());
        }

        @Test
        @DisplayName("changed field → isEmpty is false")
        void changedIsNotEmpty() {
            StateDiff diff = StateDiff.compute(Map.of("a", 1), Map.of("a", 2));
            assertFalse(diff.isEmpty());
        }

        @Test
        @DisplayName("removed field → isEmpty is false")
        void removedIsNotEmpty() {
            Map<String, Object> old = new HashMap<>();
            old.put("a", 1);
            old.put("b", 2);
            StateDiff diff = StateDiff.compute(old, Map.of("a", 1));
            assertFalse(diff.isEmpty());
        }

        @Test
        @DisplayName("fullState is never empty (has all state)")
        void fullStateWithDataIsNotEmpty() {
            StateDiff diff = StateDiff.fullState(Map.of("a", 1));
            assertFalse(diff.isEmpty());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // hasGlobalChanges()
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("hasGlobalChanges()")
    class HasGlobalChanges {

        @Test
        @DisplayName("returns true when changed field is in globalFieldNames")
        void trueWhenGlobalFieldChanged() {
            StateDiff diff = StateDiff.compute(
                Map.of("globalCount", 1, "localField", "x"),
                Map.of("globalCount", 2, "localField", "x"));

            Set<String> globalFields = Set.of("globalCount");
            assertTrue(diff.hasGlobalChanges(globalFields));
        }

        @Test
        @DisplayName("returns false when only non-global fields changed")
        void falseWhenOnlyLocalChanged() {
            StateDiff diff = StateDiff.compute(
                Map.of("globalCount", 1, "localField", "x"),
                Map.of("globalCount", 1, "localField", "y"));

            Set<String> globalFields = Set.of("globalCount");
            assertFalse(diff.hasGlobalChanges(globalFields));
        }

        @Test
        @DisplayName("returns true when global field is removed")
        void trueWhenGlobalFieldRemoved() {
            Map<String, Object> oldState = new HashMap<>();
            oldState.put("globalCount", 1);
            oldState.put("localField", "x");

            Map<String, Object> newState = new HashMap<>();
            newState.put("localField", "x");

            StateDiff diff = StateDiff.compute(oldState, newState);
            assertTrue(diff.hasGlobalChanges(Set.of("globalCount")));
        }

        @Test
        @DisplayName("returns false for empty global field set")
        void falseForEmptyGlobalFieldSet() {
            StateDiff diff = StateDiff.compute(Map.of("a", 1), Map.of("a", 2));
            assertFalse(diff.hasGlobalChanges(Collections.emptySet()));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // toJson()
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("toJson()")
    class ToJson {

        @Test
        @DisplayName("JSON contains 'type' field")
        void jsonContainsType() {
            String json = StateDiff.fullState(Map.of("a", 1)).toJson();
            assertTrue(json.contains("\"type\""));
            assertTrue(json.contains("\"full\""));
        }

        @Test
        @DisplayName("JSON contains 'changed' field")
        void jsonContainsChanged() {
            String json = StateDiff.fullState(Map.of("count", 5)).toJson();
            assertTrue(json.contains("\"changed\""));
            assertTrue(json.contains("\"count\""));
        }

        @Test
        @DisplayName("JSON contains 'removed' field")
        void jsonContainsRemoved() {
            String json = StateDiff.fullState(Map.of("a", 1)).toJson();
            assertTrue(json.contains("\"removed\""));
        }

        @Test
        @DisplayName("JSON is valid (starts with { and ends with })")
        void jsonIsValidObject() {
            String json = StateDiff.compute(Map.of("a", 1), Map.of("a", 2)).toJson();
            assertTrue(json.startsWith("{"));
            assertTrue(json.endsWith("}"));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // toServerMessage()
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("toServerMessage()")
    class ToServerMessage {

        @Test
        @DisplayName("returns non-null ServerMessage")
        void returnsNonNull() {
            StateDiff diff = StateDiff.compute(Map.of("a", 1), Map.of("a", 2));
            assertNotNull(diff.toServerMessage());
        }

        @Test
        @DisplayName("server message type matches diff type")
        void typeMatches() {
            StateDiff diff = StateDiff.fullState(Map.of("a", 1));
            ServerMessage msg = diff.toServerMessage();
            assertEquals("full", msg.getType());
        }

        @Test
        @DisplayName("server message contains changed fields")
        void changedFieldsPresent() {
            StateDiff diff = StateDiff.compute(Map.of("count", 1), Map.of("count", 99));
            ServerMessage msg = diff.toServerMessage();
            assertNotNull(msg.getChanged());
            assertEquals(99, msg.getChanged().get("count"));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // toString()
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("toString() contains type and changed keys")
    void toStringContainsTypeAndKeys() {
        StateDiff diff = StateDiff.compute(Map.of("count", 1), Map.of("count", 2));
        String str = diff.toString();
        assertTrue(str.contains("patch"));
        assertTrue(str.contains("count"));
    }
}
