package io.javalive.processor.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link MethodModel} — especially the derived helper methods
 * called by VueComponentGenerator and SpringControllerGenerator.
 */
@DisplayName("MethodModel")
class MethodModelTest {

    private MethodModel model;

    @BeforeEach
    void setUp() {
        model = new MethodModel();
        model.setJavaName("increment");
        model.setVueName("increment");
        model.setReturnType("void");
    }

    // ── getParamNamesJoined ───────────────────────────────────────────────────

    @Nested
    @DisplayName("getParamNamesJoined()")
    class GetParamNamesJoined {

        @Test
        @DisplayName("returns empty string when no params")
        void noParams() {
            model.setParamNames(Collections.emptyList());
            assertEquals("", model.getParamNamesJoined());
        }

        @Test
        @DisplayName("returns null-list as empty string")
        void nullParams() {
            model.setParamNames(null);
            assertEquals("", model.getParamNamesJoined());
        }

        @Test
        @DisplayName("returns single param name")
        void singleParam() {
            model.setParamNames(List.of("userId"));
            assertEquals("userId", model.getParamNamesJoined());
        }

        @Test
        @DisplayName("returns multiple params comma-joined")
        void multipleParams() {
            model.setParamNames(Arrays.asList("name", "email", "age"));
            assertEquals("name, email, age", model.getParamNamesJoined());
        }
    }

    // ── getArgsArrayLiteral ───────────────────────────────────────────────────

    @Nested
    @DisplayName("getArgsArrayLiteral()")
    class GetArgsArrayLiteral {

        @Test
        @DisplayName("returns '[]' when no params")
        void noParams() {
            model.setParamNames(Collections.emptyList());
            assertEquals("[]", model.getArgsArrayLiteral());
        }

        @Test
        @DisplayName("returns '[]' for null params")
        void nullParams() {
            model.setParamNames(null);
            assertEquals("[]", model.getArgsArrayLiteral());
        }

        @Test
        @DisplayName("returns '[param]' for single param")
        void singleParam() {
            model.setParamNames(List.of("userId"));
            assertEquals("[userId]", model.getArgsArrayLiteral());
        }

        @Test
        @DisplayName("returns '[a, b, c]' for multiple params")
        void multipleParams() {
            model.setParamNames(Arrays.asList("a", "b", "c"));
            assertEquals("[a, b, c]", model.getArgsArrayLiteral());
        }
    }

    // ── isVoidReturn ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("isVoidReturn()")
    class IsVoidReturn {

        @Test
        @DisplayName("true when returnType is 'void'")
        void returnTypeVoid() {
            model.setReturnType("void");
            assertTrue(model.isVoidReturn());
        }

        @Test
        @DisplayName("true when returnType is null")
        void returnTypeNull() {
            model.setReturnType(null);
            assertTrue(model.isVoidReturn());
        }

        @Test
        @DisplayName("false when returnType is 'String'")
        void returnTypeString() {
            model.setReturnType("String");
            assertFalse(model.isVoidReturn());
        }

        @Test
        @DisplayName("false when returnType is 'boolean'")
        void returnTypeBoolean() {
            model.setReturnType("boolean");
            assertFalse(model.isVoidReturn());
        }

        @Test
        @DisplayName("false when returnType is 'java.util.List'")
        void returnTypeList() {
            model.setReturnType("java.util.List<String>");
            assertFalse(model.isVoidReturn());
        }
    }

    // ── getVueName fallback ───────────────────────────────────────────────────

    @Nested
    @DisplayName("getVueName() fallback to javaName")
    class GetVueName {

        @Test
        @DisplayName("returns vueName when set")
        void returnsVueName() {
            model.setJavaName("increment");
            model.setVueName("inc");
            assertEquals("inc", model.getVueName());
        }

        @Test
        @DisplayName("falls back to javaName when vueName is empty")
        void fallsBackToJavaName() {
            model.setJavaName("increment");
            model.setVueName("");
            assertEquals("increment", model.getVueName());
        }

        @Test
        @DisplayName("falls back to javaName when vueName is null")
        void fallsBackWhenNull() {
            model.setJavaName("doSomething");
            model.setVueName(null);
            assertEquals("doSomething", model.getVueName());
        }
    }

    // ── boolean flags ────────────────────────────────────────────────────────

    @Nested
    @DisplayName("boolean flag defaults")
    class BooleanFlags {

        @Test
        @DisplayName("showLoading defaults to false")
        void showLoadingDefault() {
            assertFalse(new MethodModel().isShowLoading());
        }

        @Test
        @DisplayName("confirm defaults to false")
        void confirmDefault() {
            assertFalse(new MethodModel().isConfirm());
        }

        @Test
        @DisplayName("emitsEvent defaults to false")
        void emitsEventDefault() {
            assertFalse(new MethodModel().isEmitsEvent());
        }

        @Test
        @DisplayName("computed defaults to false")
        void computedDefault() {
            assertFalse(new MethodModel().isComputed());
        }

        @Test
        @DisplayName("deep defaults to false")
        void deepDefault() {
            assertFalse(new MethodModel().isDeep());
        }

        @Test
        @DisplayName("immediate defaults to false")
        void immediateDefault() {
            assertFalse(new MethodModel().isImmediate());
        }
    }

    // ── numeric defaults ─────────────────────────────────────────────────────

    @Nested
    @DisplayName("numeric defaults")
    class NumericDefaults {

        @Test
        @DisplayName("debounce defaults to 0")
        void debounceDefault() {
            assertEquals(0, new MethodModel().getDebounce());
        }

        @Test
        @DisplayName("watchDebounce defaults to 0")
        void watchDebounceDefault() {
            assertEquals(0, new MethodModel().getWatchDebounce());
        }
    }

    // ── hasNoParams ───────────────────────────────────────────────────────────

    @Nested
    @DisplayName("hasNoParams()")
    class HasNoParams {

        @Test
        @DisplayName("true when empty list")
        void emptyList() {
            model.setParamNames(Collections.emptyList());
            assertTrue(model.hasNoParams());
        }

        @Test
        @DisplayName("true when null")
        void nullList() {
            model.setParamNames(null);
            assertTrue(model.hasNoParams());
        }

        @Test
        @DisplayName("false when has params")
        void hasParams() {
            model.setParamNames(List.of("id"));
            assertFalse(model.hasNoParams());
        }
    }

    // ── toString ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("toString contains key fields")
    void toStringContainsKeyFields() {
        model.setJavaName("myMethod");
        model.setVueName("myMethod");
        model.setReturnType("String");
        String s = model.toString();
        assertTrue(s.contains("myMethod"));
        assertTrue(s.contains("String"));
    }
}
