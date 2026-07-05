package io.javalive.core.state;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Handles serialization and deserialization of state objects for WebSocket transmission.
 *
 * <p>Uses Jackson under the hood with:
 * <ul>
 *   <li>{@code JavaTimeModule} for proper Java 8 date/time serialization</li>
 *   <li>ISO-8601 date strings (not timestamps)</li>
 *   <li>Null values included in output (client needs to know about nulls)</li>
 *   <li>Unknown properties ignored on deserialization</li>
 * </ul>
 *
 * @author JavaLive Team
 * @since 1.0.0
 */
@Component
public class StateSerializer {

    private final ObjectMapper mapper;

    public StateSerializer() {
        this.mapper = new ObjectMapper();
        this.mapper.registerModule(new JavaTimeModule());
        this.mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        this.mapper.configure(
            com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,
            false
        );
    }

    /**
     * Serializes a state map to a JSON string.
     *
     * @param state the state map to serialize
     * @return JSON string representation
     * @throws RuntimeException if serialization fails
     */
    public String serialize(Map<String, Object> state) {
        try {
            return mapper.writeValueAsString(state);
        } catch (Exception e) {
            throw new RuntimeException("JavaLive: Failed to serialize state: " + e.getMessage(), e);
        }
    }

    /**
     * Deserializes a JSON string back to a state map.
     *
     * @param json the JSON string to parse
     * @return the deserialized state map
     * @throws RuntimeException if deserialization fails
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> deserialize(String json) {
        try {
            return mapper.readValue(json, Map.class);
        } catch (Exception e) {
            throw new RuntimeException("JavaLive: Failed to deserialize state: " + e.getMessage(), e);
        }
    }

    /**
     * Converts any object to a state-compatible representation via Jackson.
     * Useful for converting POJOs returned from @VueMethod to plain Maps.
     *
     * @param value the object to convert
     * @return a Map/List/primitive representation
     */
    public Object toStateCompatible(Object value) {
        if (value == null) return null;
        if (value instanceof String || value instanceof Number || value instanceof Boolean) {
            return value;
        }
        try {
            // Round-trip through JSON to normalize complex objects
            String json = mapper.writeValueAsString(value);
            return mapper.readValue(json, Object.class);
        } catch (Exception e) {
            return value.toString();
        }
    }

    /**
     * Returns the underlying ObjectMapper for advanced use cases.
     */
    public ObjectMapper getMapper() {
        return mapper;
    }
}
