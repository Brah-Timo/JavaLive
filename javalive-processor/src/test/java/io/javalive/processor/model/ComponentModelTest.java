package io.javalive.processor.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link ComponentModel} — including all derived helper methods.
 */
@DisplayName("ComponentModel")
class ComponentModelTest {

    private ComponentModel model;

    @BeforeEach
    void setUp() {
        model = new ComponentModel();
        model.setJavaClassName("CounterWidget");
        model.setPackageName("com.example.components");
        model.setComponentName("counter-widget");
    }

    // ── derived name helpers ──────────────────────────────────────────────────

    @Nested
    @DisplayName("generated name helpers")
    class GeneratedNames {

        @Test
        @DisplayName("getGeneratedControllerQualifiedName() produces correct FQN")
        void generatedControllerQualifiedName() {
            assertEquals(
                "com.example.components.generated.CounterWidgetLiveController",
                model.getGeneratedControllerQualifiedName()
            );
        }

        @Test
        @DisplayName("getGeneratedControllerSimpleName() returns ClassName + LiveController")
        void generatedControllerSimpleName() {
            assertEquals("CounterWidgetLiveController", model.getGeneratedControllerSimpleName());
        }

        @Test
        @DisplayName("getGeneratedPackageName() appends '.generated'")
        void generatedPackageName() {
            assertEquals("com.example.components.generated", model.getGeneratedPackageName());
        }
    }

    // ── topic helpers ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("WebSocket topic helpers")
    class Topics {

        @Test
        @DisplayName("getStateTopic() is /topic/{componentName}.state")
        void stateTopic() {
            assertEquals("/topic/counter-widget.state", model.getStateTopic());
        }

        @Test
        @DisplayName("getUserStateTopic() is /user/topic/{componentName}.state")
        void userStateTopic() {
            assertEquals("/user/topic/counter-widget.state", model.getUserStateTopic());
        }
    }

    // ── hasReactiveState ─────────────────────────────────────────────────────

    @Nested
    @DisplayName("hasReactiveState()")
    class HasReactiveState {

        @Test
        @DisplayName("false when no reactive fields")
        void noReactiveFields() {
            model.setReactiveFields(List.of());
            assertFalse(model.hasReactiveState());
        }

        @Test
        @DisplayName("true when reactive fields are present")
        void hasReactiveFields() {
            FieldModel f = new FieldModel();
            f.setJavaName("count");
            model.setReactiveFields(List.of(f));
            assertTrue(model.hasReactiveState());
        }
    }

    // ── hasGlobalState ────────────────────────────────────────────────────────

    @Nested
    @DisplayName("hasGlobalState()")
    class HasGlobalState {

        @Test
        @DisplayName("false when all fields are session-scoped")
        void allSessionScoped() {
            FieldModel f = new FieldModel();
            f.setScope("session");
            model.setReactiveFields(List.of(f));
            assertFalse(model.hasGlobalState());
        }

        @Test
        @DisplayName("true when any field is global-scoped")
        void hasGlobalField() {
            FieldModel f = new FieldModel();
            f.setScope("global");
            model.setReactiveFields(List.of(f));
            assertTrue(model.hasGlobalState());
        }
    }

    // ── hasProps / hasTemplate ────────────────────────────────────────────────

    @Nested
    @DisplayName("hasProps()")
    class HasProps {

        @Test
        @DisplayName("false when no props")
        void noProps() {
            model.setPropFields(List.of());
            assertFalse(model.hasProps());
        }

        @Test
        @DisplayName("true when props present")
        void hasProps() {
            FieldModel prop = new FieldModel();
            prop.setProp(true);
            model.setPropFields(List.of(prop));
            assertTrue(model.hasProps());
        }
    }

    // ── page properties ───────────────────────────────────────────────────────

    @Nested
    @DisplayName("page properties")
    class PageProperties {

        @Test
        @DisplayName("isPage defaults to false")
        void isPageDefault() {
            assertFalse(new ComponentModel().isPage());
        }

        @Test
        @DisplayName("requiresAuth defaults to false")
        void requiresAuthDefault() {
            assertFalse(new ComponentModel().isRequiresAuth());
        }

        @Test
        @DisplayName("can set page properties")
        void setPageProperties() {
            model.setPage(true);
            model.setVuePath("/dashboard");
            model.setRouteName("Dashboard");
            model.setRequiresAuth(false);
            model.setLayoutName("default");
            model.setPageTitle("Dashboard Page");

            assertTrue(model.isPage());
            assertEquals("/dashboard", model.getVuePath());
            assertEquals("Dashboard", model.getRouteName());
            assertFalse(model.isRequiresAuth());
            assertEquals("default", model.getLayoutName());
            assertEquals("Dashboard Page", model.getPageTitle());
        }
    }

    // ── toString ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("toString contains key information")
    void toStringContainsKeyInfo() {
        String s = model.toString();
        assertTrue(s.contains("CounterWidget"));
        assertTrue(s.contains("counter-widget"));
    }
}
