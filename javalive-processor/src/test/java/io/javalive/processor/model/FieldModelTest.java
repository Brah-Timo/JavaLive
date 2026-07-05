package io.javalive.processor.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link FieldModel}.
 */
@DisplayName("FieldModel")
class FieldModelTest {

    @Nested
    @DisplayName("defaults")
    class Defaults {

        @Test
        @DisplayName("scope defaults to 'session'")
        void scopeDefaultIsSession() {
            assertEquals("session", new FieldModel().getScope());
        }

        @Test
        @DisplayName("persist defaults to false")
        void persistDefaultIsFalse() {
            assertFalse(new FieldModel().isPersist());
        }

        @Test
        @DisplayName("label defaults to empty string")
        void labelDefaultIsEmpty() {
            assertEquals("", new FieldModel().getLabel());
        }

        @Test
        @DisplayName("isProp defaults to false")
        void isPropDefaultIsFalse() {
            assertFalse(new FieldModel().isProp());
        }

        @Test
        @DisplayName("required defaults to true")
        void requiredDefaultIsTrue() {
            assertTrue(new FieldModel().isRequired());
        }

        @Test
        @DisplayName("validator defaults to empty string")
        void validatorDefaultIsEmpty() {
            assertEquals("", new FieldModel().getValidator());
        }

        @Test
        @DisplayName("isTemplate defaults to false")
        void isTemplateDefaultIsFalse() {
            assertFalse(new FieldModel().isTemplate());
        }
    }

    @Nested
    @DisplayName("isGlobal()")
    class IsGlobal {

        @Test
        @DisplayName("false when scope is 'session'")
        void sessionScopeIsNotGlobal() {
            FieldModel f = new FieldModel();
            f.setScope("session");
            assertFalse(f.isGlobal());
        }

        @Test
        @DisplayName("true when scope is 'global'")
        void globalScopeIsGlobal() {
            FieldModel f = new FieldModel();
            f.setScope("global");
            assertTrue(f.isGlobal());
        }

        @Test
        @DisplayName("false when scope is null")
        void nullScopeIsNotGlobal() {
            FieldModel f = new FieldModel();
            f.setScope(null);
            assertFalse(f.isGlobal());
        }
    }

    @Nested
    @DisplayName("getCamelName()")
    class GetCamelName {

        @Test
        @DisplayName("returns the javaName unchanged")
        void returnsSameAsJavaName() {
            FieldModel f = new FieldModel();
            f.setJavaName("searchQuery");
            assertEquals("searchQuery", f.getCamelName());
        }
    }

    @Nested
    @DisplayName("setters and getters")
    class SettersAndGetters {

        @Test
        @DisplayName("javaType round-trip")
        void javaTypeRoundTrip() {
            FieldModel f = new FieldModel();
            f.setJavaType("java.util.List<com.example.User>");
            assertEquals("java.util.List<com.example.User>", f.getJavaType());
        }

        @Test
        @DisplayName("vueType round-trip")
        void vueTypeRoundTrip() {
            FieldModel f = new FieldModel();
            f.setVueType("Array");
            assertEquals("Array", f.getVueType());
        }

        @Test
        @DisplayName("defaultValue round-trip")
        void defaultValueRoundTrip() {
            FieldModel f = new FieldModel();
            f.setDefaultValue("[]");
            assertEquals("[]", f.getDefaultValue());
        }
    }

    @Test
    @DisplayName("toString contains key fields")
    void toStringContainsKeyFields() {
        FieldModel f = new FieldModel();
        f.setJavaName("count");
        f.setJavaType("int");
        f.setVueType("Number");
        String s = f.toString();
        assertTrue(s.contains("count"));
        assertTrue(s.contains("int"));
        assertTrue(s.contains("Number"));
    }
}
