package io.javalive.core.dispatch;

import io.javalive.annotations.VueLifecycle;
import io.javalive.annotations.VueMethod;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link SecurityGuard}.
 *
 * <p>Uses nested static helper classes annotated with @VueMethod / @VueLifecycle
 * to simulate production code. These helper classes are in the same test class
 * so they are always accessible on the classpath.
 */
@DisplayName("SecurityGuard")
class SecurityGuardTest {

    // ─────────────────────────────────────────────────────────────────────────
    // Test fixtures — helper classes simulating annotated production code
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * A fake component class that has an allowed method and a blocked method.
     * The FQN is used in SecurityGuard.verify() calls.
     */
    public static class FakeComponent {

        /** This method IS allowed — annotated with @VueMethod. */
        @VueMethod
        public void increment() {}

        /** This method IS allowed with extra settings. */
        @VueMethod(confirm = true)
        public void deleteItem() {}

        /** This method is NOT allowed — no @VueMethod annotation. */
        public void internalHelper() {}

        /** Private method — also NOT allowed. */
        @SuppressWarnings("unused")
        private void secretMethod() {}
    }

    /**
     * A fake component with lifecycle hooks.
     */
    public static class FakeLifecycleComponent {

        @VueLifecycle(hook = "onMounted")
        public void onMounted() {}

        @VueLifecycle(hook = "onUnmounted")
        public void onUnmounted() {}

        /** No @VueLifecycle — should be blocked. */
        public void notAHook() {}
    }

    private static final String FAKE_CLASS =
        "io.javalive.core.dispatch.SecurityGuardTest$FakeComponent";

    private static final String FAKE_LIFECYCLE_CLASS =
        "io.javalive.core.dispatch.SecurityGuardTest$FakeLifecycleComponent";

    @BeforeEach
    void clearCacheBefore() {
        SecurityGuard.clearCache();
    }

    @AfterEach
    void clearCacheAfter() {
        SecurityGuard.clearCache();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // verify() — allowed methods
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("verify() — allowed methods")
    class AllowedMethods {

        @Test
        @DisplayName("allows @VueMethod-annotated method without throwing")
        void allowsAnnotatedMethod() {
            assertDoesNotThrow(() -> SecurityGuard.verify(FAKE_CLASS, "increment"));
        }

        @Test
        @DisplayName("allows @VueMethod with confirm=true attribute")
        void allowsAnnotatedMethodWithAttributes() {
            assertDoesNotThrow(() -> SecurityGuard.verify(FAKE_CLASS, "deleteItem"));
        }

        @Test
        @DisplayName("second call uses cache — still passes without throwing")
        void secondCallUsesCache() {
            SecurityGuard.verify(FAKE_CLASS, "increment"); // populates cache
            assertDoesNotThrow(() -> SecurityGuard.verify(FAKE_CLASS, "increment"));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // verify() — blocked methods
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("verify() — blocked methods")
    class BlockedMethods {

        @Test
        @DisplayName("blocks method without @VueMethod annotation")
        void blocksUnannotatedMethod() {
            assertThrows(SecurityException.class,
                () -> SecurityGuard.verify(FAKE_CLASS, "internalHelper"));
        }

        @Test
        @DisplayName("blocks unknown method name")
        void blocksUnknownMethod() {
            assertThrows(SecurityException.class,
                () -> SecurityGuard.verify(FAKE_CLASS, "nonExistentMethod"));
        }

        @Test
        @DisplayName("security exception message mentions class and method")
        void exceptionMessageContainsDetails() {
            SecurityException ex = assertThrows(SecurityException.class,
                () -> SecurityGuard.verify(FAKE_CLASS, "internalHelper"));
            assertTrue(ex.getMessage().contains("internalHelper")
                || ex.getMessage().contains("BLOCKED"));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // verify() — class not found
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("verify() — class not found")
    class ClassNotFound {

        @Test
        @DisplayName("blocks when class does not exist on classpath")
        void blocksForUnknownClass() {
            assertThrows(SecurityException.class,
                () -> SecurityGuard.verify("com.example.NonExistentClass", "someMethod"));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // verifyLifecycle()
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("verifyLifecycle()")
    class VerifyLifecycle {

        @Test
        @DisplayName("allows @VueLifecycle hook 'onMounted'")
        void allowsOnMounted() {
            assertDoesNotThrow(() ->
                SecurityGuard.verifyLifecycle(FAKE_LIFECYCLE_CLASS, "onMounted"));
        }

        @Test
        @DisplayName("allows @VueLifecycle hook 'onUnmounted'")
        void allowsOnUnmounted() {
            assertDoesNotThrow(() ->
                SecurityGuard.verifyLifecycle(FAKE_LIFECYCLE_CLASS, "onUnmounted"));
        }

        @Test
        @DisplayName("blocks method without @VueLifecycle annotation")
        void blocksMethodWithoutLifecycleAnnotation() {
            assertThrows(SecurityException.class,
                () -> SecurityGuard.verifyLifecycle(FAKE_LIFECYCLE_CLASS, "notAHook"));
        }

        @Test
        @DisplayName("blocks unknown lifecycle hook name")
        void blocksUnknownHookName() {
            assertThrows(SecurityException.class,
                () -> SecurityGuard.verifyLifecycle(FAKE_LIFECYCLE_CLASS, "onCreated"));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // clearCache()
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("clearCache()")
    class ClearCache {

        @Test
        @DisplayName("clearCache does not throw")
        void clearCacheDoesNotThrow() {
            assertDoesNotThrow(SecurityGuard::clearCache);
        }

        @Test
        @DisplayName("after clearCache, verify still works correctly")
        void afterClearVerifyStillWorks() {
            SecurityGuard.verify(FAKE_CLASS, "increment"); // populate cache
            SecurityGuard.clearCache();
            // Should still work (re-computes from reflection)
            assertDoesNotThrow(() -> SecurityGuard.verify(FAKE_CLASS, "increment"));
        }

        @Test
        @DisplayName("after clearCache, blocked method is still blocked")
        void afterClearBlockedStillBlocked() {
            SecurityGuard.clearCache();
            assertThrows(SecurityException.class,
                () -> SecurityGuard.verify(FAKE_CLASS, "internalHelper"));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Instance method verifyMethod() (bean wrapper)
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("verifyMethod() — instance wrapper")
    class VerifyMethodInstance {

        @Test
        @DisplayName("verifyMethod() allows annotated method")
        void instanceVerifyAllowsAnnotated() {
            SecurityGuard guard = new SecurityGuard();
            assertDoesNotThrow(() -> guard.verifyMethod(FAKE_CLASS, "increment"));
        }

        @Test
        @DisplayName("verifyMethod() blocks unannotated method")
        void instanceVerifyBlocksUnannotated() {
            SecurityGuard guard = new SecurityGuard();
            assertThrows(SecurityException.class,
                () -> guard.verifyMethod(FAKE_CLASS, "internalHelper"));
        }
    }
}
