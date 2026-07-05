package io.javalive.core.rendering;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link HydrationData}.
 * Covers toJson(), toScriptTag(), getters/setters.
 */
@DisplayName("HydrationData")
class HydrationDataTest {

    // ─────────────────────────────────────────────────────────────────────────
    // Constructor and getters
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Constructor and getters")
    class ConstructorAndGetters {

        @Test
        @DisplayName("componentName is stored correctly")
        void componentNameStored() {
            HydrationData data = new HydrationData("dashboard", Map.of("count", 5));
            assertEquals("dashboard", data.getComponentName());
        }

        @Test
        @DisplayName("state is stored correctly")
        void stateStored() {
            Map<String, Object> state = Map.of("count", 5, "name", "Alice");
            HydrationData data = new HydrationData("dashboard", state);
            assertEquals(state, data.getState());
        }

        @Test
        @DisplayName("default wsEndpoint is '/javalive-ws'")
        void defaultWsEndpoint() {
            HydrationData data = new HydrationData("comp", Map.of());
            assertEquals("/javalive-ws", data.getWsEndpoint());
        }

        @Test
        @DisplayName("default sessionId is null")
        void defaultSessionIdIsNull() {
            HydrationData data = new HydrationData("comp", Map.of());
            assertNull(data.getSessionId());
        }

        @Test
        @DisplayName("setSessionId and getSessionId round-trip")
        void sessionIdRoundTrip() {
            HydrationData data = new HydrationData("comp", Map.of());
            data.setSessionId("sess-abc-123");
            assertEquals("sess-abc-123", data.getSessionId());
        }

        @Test
        @DisplayName("setWsEndpoint and getWsEndpoint round-trip")
        void wsEndpointRoundTrip() {
            HydrationData data = new HydrationData("comp", Map.of());
            data.setWsEndpoint("/custom-ws");
            assertEquals("/custom-ws", data.getWsEndpoint());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // toJson()
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("toJson()")
    class ToJson {

        @Test
        @DisplayName("JSON contains 'component' field with correct value")
        void jsonContainsComponent() {
            HydrationData data = new HydrationData("dashboard", Map.of());
            String json = data.toJson();
            assertTrue(json.contains("\"component\""));
            assertTrue(json.contains("\"dashboard\""));
        }

        @Test
        @DisplayName("JSON contains 'state' field")
        void jsonContainsState() {
            HydrationData data = new HydrationData("dashboard", Map.of("count", 5));
            String json = data.toJson();
            assertTrue(json.contains("\"state\""));
            assertTrue(json.contains("\"count\""));
        }

        @Test
        @DisplayName("JSON contains 'sessionId' field")
        void jsonContainsSessionId() {
            HydrationData data = new HydrationData("comp", Map.of());
            data.setSessionId("sess-xyz");
            String json = data.toJson();
            assertTrue(json.contains("\"sessionId\""));
            assertTrue(json.contains("\"sess-xyz\""));
        }

        @Test
        @DisplayName("JSON contains 'wsEndpoint' field")
        void jsonContainsWsEndpoint() {
            HydrationData data = new HydrationData("comp", Map.of());
            String json = data.toJson();
            assertTrue(json.contains("\"wsEndpoint\""));
            assertTrue(json.contains("\"/javalive-ws\""));
        }

        @Test
        @DisplayName("JSON contains 'ts' timestamp field")
        void jsonContainsTimestamp() {
            HydrationData data = new HydrationData("comp", Map.of());
            String json = data.toJson();
            assertTrue(json.contains("\"ts\""));
        }

        @Test
        @DisplayName("JSON is a valid JSON object (starts with { ends with })")
        void jsonIsValidObject() {
            HydrationData data = new HydrationData("comp", Map.of("x", 1));
            String json = data.toJson();
            assertTrue(json.startsWith("{"));
            assertTrue(json.endsWith("}"));
        }

        @Test
        @DisplayName("custom wsEndpoint is reflected in JSON")
        void customWsEndpointInJson() {
            HydrationData data = new HydrationData("comp", Map.of());
            data.setWsEndpoint("/my-ws-endpoint");
            String json = data.toJson();
            assertTrue(json.contains("\"/my-ws-endpoint\""));
        }

        @Test
        @DisplayName("empty state produces empty state object in JSON")
        void emptyStateInJson() {
            HydrationData data = new HydrationData("comp", Map.of());
            String json = data.toJson();
            assertTrue(json.contains("\"state\":{}"));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // toScriptTag()
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("toScriptTag()")
    class ToScriptTag {

        @Test
        @DisplayName("contains <script> tag")
        void containsScriptTag() {
            HydrationData data = new HydrationData("comp", Map.of());
            String tag = data.toScriptTag();
            assertTrue(tag.contains("<script"));
        }

        @Test
        @DisplayName("contains id='javalive-state'")
        void containsJavaLiveStateId() {
            HydrationData data = new HydrationData("comp", Map.of());
            String tag = data.toScriptTag();
            assertTrue(tag.contains("id=\"javalive-state\""));
        }

        @Test
        @DisplayName("contains type='application/json'")
        void containsApplicationJsonType() {
            HydrationData data = new HydrationData("comp", Map.of());
            String tag = data.toScriptTag();
            assertTrue(tag.contains("type=\"application/json\""));
        }

        @Test
        @DisplayName("contains closing </script> tag")
        void containsClosingScriptTag() {
            HydrationData data = new HydrationData("comp", Map.of());
            String tag = data.toScriptTag();
            assertTrue(tag.contains("</script>"));
        }

        @Test
        @DisplayName("script tag embeds the JSON content")
        void scriptTagEmbedsJson() {
            HydrationData data = new HydrationData("my-widget", Map.of("value", 42));
            data.setSessionId("test-sess");
            String tag = data.toScriptTag();
            // The JSON should be inside the script tag
            assertTrue(tag.contains("\"component\""));
            assertTrue(tag.contains("\"my-widget\""));
        }

        @Test
        @DisplayName("script tag starts and ends with correct HTML")
        void scriptTagStructure() {
            HydrationData data = new HydrationData("comp", Map.of());
            String tag = data.toScriptTag();
            assertTrue(tag.startsWith("<script"));
            assertTrue(tag.endsWith("</script>"));
        }
    }
}
