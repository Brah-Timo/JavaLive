package io.javalive.core.state;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link StateSchema} and its inner {@link StateSchema.FieldSchema}.
 */
@DisplayName("StateSchema")
class StateSchemaTest {

    private StateSchema schema;

    @BeforeEach
    void setUp() {
        schema = new StateSchema();
        schema.setComponentName("dashboard");
        schema.setJavaClass("com.example.Dashboard");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Basic getters/setters
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Basic properties")
    class BasicProperties {

        @Test
        @DisplayName("componentName is stored and retrieved")
        void componentNameRoundTrip() {
            assertEquals("dashboard", schema.getComponentName());
        }

        @Test
        @DisplayName("javaClass is stored and retrieved")
        void javaClassRoundTrip() {
            assertEquals("com.example.Dashboard", schema.getJavaClass());
        }

        @Test
        @DisplayName("fields starts as empty list")
        void fieldsStartEmpty() {
            assertNotNull(schema.getFields());
            assertTrue(schema.getFields().isEmpty());
        }

        @Test
        @DisplayName("setFields replaces the list")
        void setFieldsReplacesFields() {
            StateSchema.FieldSchema f = makeField("count", "int", 0, "session");
            schema.setFields(List.of(f));
            assertEquals(1, schema.getFields().size());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // FieldSchema inner class
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("FieldSchema")
    class FieldSchemaTest {

        @Test
        @DisplayName("default scope is 'session'")
        void defaultScopeIsSession() {
            StateSchema.FieldSchema f = new StateSchema.FieldSchema();
            assertEquals("session", f.getScope());
        }

        @Test
        @DisplayName("default persist is false")
        void defaultPersistIsFalse() {
            StateSchema.FieldSchema f = new StateSchema.FieldSchema();
            assertFalse(f.isPersist());
        }

        @Test
        @DisplayName("isGlobal returns false for session scope")
        void isGlobalFalseForSession() {
            StateSchema.FieldSchema f = makeField("x", "int", 0, "session");
            assertFalse(f.isGlobal());
        }

        @Test
        @DisplayName("isGlobal returns true for global scope")
        void isGlobalTrueForGlobal() {
            StateSchema.FieldSchema f = makeField("x", "int", 0, "global");
            assertTrue(f.isGlobal());
        }

        @Test
        @DisplayName("all field setters/getters round-trip")
        void fieldGettersSettersRoundTrip() {
            StateSchema.FieldSchema f = new StateSchema.FieldSchema();
            f.setName("myField");
            f.setJavaType("java.lang.String");
            f.setVueType("String");
            f.setDefaultValue("hello");
            f.setScope("global");
            f.setPersist(true);
            f.setLabel("My Field");

            assertEquals("myField", f.getName());
            assertEquals("java.lang.String", f.getJavaType());
            assertEquals("String", f.getVueType());
            assertEquals("hello", f.getDefaultValue());
            assertEquals("global", f.getScope());
            assertTrue(f.isPersist());
            assertEquals("My Field", f.getLabel());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // createInitialState()
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("createInitialState()")
    class CreateInitialState {

        @Test
        @DisplayName("empty fields list produces empty state")
        void emptyFieldsEmptyState() {
            Map<String, Object> state = schema.createInitialState();
            assertTrue(state.isEmpty());
        }

        @Test
        @DisplayName("creates state with correct default values")
        void createsStateWithDefaults() {
            List<StateSchema.FieldSchema> fields = new ArrayList<>();
            fields.add(makeField("count", "int", 0, "session"));
            fields.add(makeField("name", "String", "Alice", "session"));
            fields.add(makeField("active", "boolean", false, "session"));
            schema.setFields(fields);

            Map<String, Object> state = schema.createInitialState();
            assertEquals(0, state.get("count"));
            assertEquals("Alice", state.get("name"));
            assertEquals(false, state.get("active"));
        }

        @Test
        @DisplayName("creates state including global-scoped fields")
        void includesGlobalFields() {
            List<StateSchema.FieldSchema> fields = new ArrayList<>();
            fields.add(makeField("sessionField", "String", "local", "session"));
            fields.add(makeField("globalField", "int", 42, "global"));
            schema.setFields(fields);

            Map<String, Object> state = schema.createInitialState();
            assertEquals("local", state.get("sessionField"));
            assertEquals(42, state.get("globalField"));
        }

        @Test
        @DisplayName("null default value is stored as null")
        void nullDefaultValueIsNull() {
            List<StateSchema.FieldSchema> fields = new ArrayList<>();
            fields.add(makeField("nullable", "String", null, "session"));
            schema.setFields(fields);

            Map<String, Object> state = schema.createInitialState();
            assertTrue(state.containsKey("nullable"));
            assertNull(state.get("nullable"));
        }

        @Test
        @DisplayName("returned map is mutable")
        void returnedMapIsMutable() {
            schema.setFields(List.of(makeField("x", "int", 1, "session")));
            Map<String, Object> state = schema.createInitialState();
            assertDoesNotThrow(() -> state.put("extra", "value"));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // getGlobalFields() / getSessionFields()
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getGlobalFields() and getSessionFields()")
    class FieldFiltering {

        @BeforeEach
        void addMixedFields() {
            List<StateSchema.FieldSchema> fields = new ArrayList<>();
            fields.add(makeField("sessionA", "String", "", "session"));
            fields.add(makeField("sessionB", "int", 0, "session"));
            fields.add(makeField("globalX", "int", 100, "global"));
            fields.add(makeField("globalY", "boolean", true, "global"));
            schema.setFields(fields);
        }

        @Test
        @DisplayName("getGlobalFields returns only global-scoped fields")
        void globalFieldsFiltered() {
            List<StateSchema.FieldSchema> globals = schema.getGlobalFields();
            assertEquals(2, globals.size());
            assertTrue(globals.stream().allMatch(StateSchema.FieldSchema::isGlobal));
        }

        @Test
        @DisplayName("getSessionFields returns only session-scoped fields")
        void sessionFieldsFiltered() {
            List<StateSchema.FieldSchema> sessions = schema.getSessionFields();
            assertEquals(2, sessions.size());
            assertTrue(sessions.stream().noneMatch(StateSchema.FieldSchema::isGlobal));
        }

        @Test
        @DisplayName("global and session fields together equal total field count")
        void globalPlusSessionEqualsTotal() {
            int total = schema.getFields().size();
            int globals = schema.getGlobalFields().size();
            int sessions = schema.getSessionFields().size();
            assertEquals(total, globals + sessions);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // hasGlobalFields()
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("hasGlobalFields()")
    class HasGlobalFields {

        @Test
        @DisplayName("returns false when no global fields")
        void falseWhenNoGlobal() {
            schema.setFields(List.of(makeField("x", "int", 0, "session")));
            assertFalse(schema.hasGlobalFields());
        }

        @Test
        @DisplayName("returns true when at least one global field exists")
        void trueWhenOneGlobal() {
            List<StateSchema.FieldSchema> fields = new ArrayList<>();
            fields.add(makeField("local", "int", 0, "session"));
            fields.add(makeField("global", "int", 0, "global"));
            schema.setFields(fields);
            assertTrue(schema.hasGlobalFields());
        }

        @Test
        @DisplayName("returns false for empty field list")
        void falseForEmpty() {
            assertFalse(schema.hasGlobalFields());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────

    private StateSchema.FieldSchema makeField(String name, String javaType,
                                               Object defaultValue, String scope) {
        StateSchema.FieldSchema f = new StateSchema.FieldSchema();
        f.setName(name);
        f.setJavaType(javaType);
        f.setDefaultValue(defaultValue);
        f.setScope(scope);
        return f;
    }
}
