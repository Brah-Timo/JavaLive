package io.javalive.core.websocket;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link ServerMessage} factory methods.
 * Covers fullState, patch, error (1-arg and 2-arg), emit, reload, setters.
 */
@DisplayName("ServerMessage")
class ServerMessageTest {

    // ─────────────────────────────────────────────────────────────────────────
    // fullState()
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("fullState()")
    class FullState {

        @Test
        @DisplayName("type is 'full'")
        void typeIsFull() {
            ServerMessage msg = ServerMessage.fullState(Map.of("count", 5));
            assertEquals("full", msg.getType());
        }

        @Test
        @DisplayName("changed contains the state")
        void changedContainsState() {
            Map<String, Object> state = Map.of("count", 5, "name", "Alice");
            ServerMessage msg = ServerMessage.fullState(state);
            assertEquals(state, msg.getChanged());
        }

        @Test
        @DisplayName("removed is empty")
        void removedIsEmpty() {
            ServerMessage msg = ServerMessage.fullState(Map.of("x", 1));
            assertNotNull(msg.getRemoved());
            assertTrue(msg.getRemoved().isEmpty());
        }

        @Test
        @DisplayName("error is null")
        void errorIsNull() {
            ServerMessage msg = ServerMessage.fullState(Map.of("x", 1));
            assertNull(msg.getError());
        }

        @Test
        @DisplayName("event is null")
        void eventIsNull() {
            ServerMessage msg = ServerMessage.fullState(Map.of("x", 1));
            assertNull(msg.getEvent());
        }

        @Test
        @DisplayName("ts is set and not null")
        void tsIsNotNull() {
            ServerMessage msg = ServerMessage.fullState(Map.of("x", 1));
            assertNotNull(msg.getTs());
            assertFalse(msg.getTs().isBlank());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // patch()
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("patch()")
    class Patch {

        @Test
        @DisplayName("type is 'patch'")
        void typeIsPatch() {
            ServerMessage msg = ServerMessage.patch("patch", Map.of("count", 6), List.of());
            assertEquals("patch", msg.getType());
        }

        @Test
        @DisplayName("changed contains the changed fields")
        void changedContainsFields() {
            Map<String, Object> changed = Map.of("count", 6);
            ServerMessage msg = ServerMessage.patch("patch", changed, List.of());
            assertEquals(changed, msg.getChanged());
        }

        @Test
        @DisplayName("removed contains the removed fields")
        void removedContainsFields() {
            List<String> removed = List.of("oldField", "anotherOld");
            ServerMessage msg = ServerMessage.patch("patch", Map.of(), removed);
            assertEquals(2, msg.getRemoved().size());
            assertTrue(msg.getRemoved().contains("oldField"));
        }

        @Test
        @DisplayName("null removed list defaults to empty list")
        void nullRemovedDefaultsToEmpty() {
            ServerMessage msg = ServerMessage.patch("patch", Map.of("a", 1), null);
            assertNotNull(msg.getRemoved());
            assertTrue(msg.getRemoved().isEmpty());
        }

        @Test
        @DisplayName("can accept 'full' type via patch factory")
        void acceptsFullType() {
            ServerMessage msg = ServerMessage.patch("full", Map.of("a", 1), List.of());
            assertEquals("full", msg.getType());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // error() — single argument
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("error() — single arg")
    class ErrorSingleArg {

        @Test
        @DisplayName("type is 'error'")
        void typeIsError() {
            ServerMessage msg = ServerMessage.error("Something went wrong");
            assertEquals("error", msg.getType());
        }

        @Test
        @DisplayName("error message is stored")
        void errorMessageStored() {
            ServerMessage msg = ServerMessage.error("Something went wrong");
            assertEquals("Something went wrong", msg.getError());
        }

        @Test
        @DisplayName("errorCode defaults to 'ERROR'")
        void errorCodeDefaultsToError() {
            ServerMessage msg = ServerMessage.error("Something went wrong");
            assertEquals("ERROR", msg.getErrorCode());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // error() — two arguments
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("error() — two args")
    class ErrorTwoArgs {

        @Test
        @DisplayName("type is 'error'")
        void typeIsError() {
            ServerMessage msg = ServerMessage.error("Not found", "NOT_FOUND");
            assertEquals("error", msg.getType());
        }

        @Test
        @DisplayName("error message is stored")
        void errorMessageStored() {
            ServerMessage msg = ServerMessage.error("Not found", "NOT_FOUND");
            assertEquals("Not found", msg.getError());
        }

        @Test
        @DisplayName("errorCode is stored correctly")
        void errorCodeStored() {
            ServerMessage msg = ServerMessage.error("Not found", "NOT_FOUND");
            assertEquals("NOT_FOUND", msg.getErrorCode());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // emit()
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("emit()")
    class Emit {

        @Test
        @DisplayName("type is 'emit'")
        void typeIsEmit() {
            ServerMessage msg = ServerMessage.emit("user-saved", Map.of("id", 42));
            assertEquals("emit", msg.getType());
        }

        @Test
        @DisplayName("event name is stored")
        void eventNameStored() {
            ServerMessage msg = ServerMessage.emit("user-saved", Map.of("id", 42));
            assertEquals("user-saved", msg.getEvent());
        }

        @Test
        @DisplayName("payload is stored")
        void payloadStored() {
            Map<String, Object> payload = Map.of("id", 42, "status", "ok");
            ServerMessage msg = ServerMessage.emit("user-saved", payload);
            assertEquals(payload, msg.getPayload());
        }

        @Test
        @DisplayName("payload can be null")
        void payloadCanBeNull() {
            ServerMessage msg = ServerMessage.emit("my-event", null);
            assertNull(msg.getPayload());
        }

        @Test
        @DisplayName("payload can be a simple string")
        void payloadCanBeString() {
            ServerMessage msg = ServerMessage.emit("click", "button-1");
            assertEquals("button-1", msg.getPayload());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // reload()
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("reload()")
    class Reload {

        @Test
        @DisplayName("type is 'reload'")
        void typeIsReload() {
            ServerMessage msg = ServerMessage.reload();
            assertEquals("reload", msg.getType());
        }

        @Test
        @DisplayName("error is null")
        void errorIsNull() {
            ServerMessage msg = ServerMessage.reload();
            assertNull(msg.getError());
        }

        @Test
        @DisplayName("event is null")
        void eventIsNull() {
            ServerMessage msg = ServerMessage.reload();
            assertNull(msg.getEvent());
        }

        @Test
        @DisplayName("ts is not null")
        void tsIsNotNull() {
            ServerMessage msg = ServerMessage.reload();
            assertNotNull(msg.getTs());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Setters (mutability tests)
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Setters")
    class SetterTests {

        @Test
        @DisplayName("setType and getType round-trip")
        void setTypeRoundTrip() {
            ServerMessage msg = ServerMessage.reload();
            msg.setType("custom");
            assertEquals("custom", msg.getType());
        }

        @Test
        @DisplayName("setCorrelationId and getCorrelationId round-trip")
        void correlationIdRoundTrip() {
            ServerMessage msg = ServerMessage.reload();
            msg.setCorrelationId("corr-123");
            assertEquals("corr-123", msg.getCorrelationId());
        }

        @Test
        @DisplayName("setTs and getTs round-trip")
        void tsRoundTrip() {
            ServerMessage msg = ServerMessage.reload();
            msg.setTs("2025-01-01T00:00:00Z");
            assertEquals("2025-01-01T00:00:00Z", msg.getTs());
        }

        @Test
        @DisplayName("setError and getError round-trip")
        void errorRoundTrip() {
            ServerMessage msg = ServerMessage.reload();
            msg.setError("test error");
            assertEquals("test error", msg.getError());
        }

        @Test
        @DisplayName("setEvent and getEvent round-trip")
        void eventRoundTrip() {
            ServerMessage msg = ServerMessage.reload();
            msg.setEvent("test-event");
            assertEquals("test-event", msg.getEvent());
        }
    }
}
