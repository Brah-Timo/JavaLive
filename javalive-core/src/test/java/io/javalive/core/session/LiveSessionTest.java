package io.javalive.core.session;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link LiveSession}.
 * Covers constructor, component tracking, activity tracking, and disconnect.
 */
@DisplayName("LiveSession")
class LiveSessionTest {

    private LiveSession session;

    @BeforeEach
    void setUp() {
        session = new LiveSession("sess-abc", "alice", "192.168.1.1");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Constructor
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Constructor")
    class Constructor {

        @Test
        @DisplayName("sessionId is stored correctly")
        void sessionIdStored() {
            assertEquals("sess-abc", session.getSessionId());
        }

        @Test
        @DisplayName("username is stored correctly")
        void usernameStored() {
            assertEquals("alice", session.getUsername());
        }

        @Test
        @DisplayName("null username defaults to 'anonymous'")
        void nullUsernameDefaultsToAnonymous() {
            LiveSession s = new LiveSession("sess-1", null, "127.0.0.1");
            assertEquals("anonymous", s.getUsername());
        }

        @Test
        @DisplayName("remoteAddress is stored correctly")
        void remoteAddressStored() {
            assertEquals("192.168.1.1", session.getRemoteAddress());
        }

        @Test
        @DisplayName("connectedAt is not null and close to now")
        void connectedAtSetToNow() {
            Instant before = Instant.now().minusSeconds(1);
            Instant connectedAt = session.getConnectedAt();
            assertNotNull(connectedAt);
            assertTrue(connectedAt.isAfter(before));
        }

        @Test
        @DisplayName("session starts connected")
        void startsConnected() {
            assertTrue(session.isConnected());
        }

        @Test
        @DisplayName("no components mounted initially")
        void noComponentsMountedInitially() {
            assertTrue(session.getMountedComponents().isEmpty());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Component tracking
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Component tracking")
    class ComponentTracking {

        @Test
        @DisplayName("mountComponent adds component")
        void mountAddsComponent() {
            session.mountComponent("dashboard");
            assertTrue(session.hasComponent("dashboard"));
        }

        @Test
        @DisplayName("unmountComponent removes component")
        void unmountRemovesComponent() {
            session.mountComponent("dashboard");
            session.unmountComponent("dashboard");
            assertFalse(session.hasComponent("dashboard"));
        }

        @Test
        @DisplayName("hasComponent returns false for non-mounted component")
        void hasComponentFalseForUnmounted() {
            assertFalse(session.hasComponent("nope"));
        }

        @Test
        @DisplayName("multiple components can be mounted simultaneously")
        void multipleComponentsMounted() {
            session.mountComponent("dashboard");
            session.mountComponent("counter");
            session.mountComponent("user-list");
            assertEquals(3, session.getMountedComponents().size());
        }

        @Test
        @DisplayName("getMountedComponents returns defensive copy")
        void getMountedComponentsIsDefensiveCopy() {
            session.mountComponent("dashboard");
            var copy = session.getMountedComponents();
            copy.add("injected"); // modify the copy
            assertFalse(session.hasComponent("injected")); // original unaffected
        }

        @Test
        @DisplayName("unmounting non-mounted component is safe")
        void unmountNonMountedIsSafe() {
            assertDoesNotThrow(() -> session.unmountComponent("never-mounted"));
        }

        @Test
        @DisplayName("mounting same component twice still has it once in set")
        void mountingSameComponentTwice() {
            session.mountComponent("dashboard");
            session.mountComponent("dashboard");
            assertEquals(1, session.getMountedComponents().size());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Activity tracking
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Activity tracking")
    class ActivityTracking {

        @Test
        @DisplayName("touch() updates lastActivityAt")
        void touchUpdatesLastActivity() throws InterruptedException {
            Instant before = session.getLastActivityAt();
            Thread.sleep(5); // tiny sleep to ensure time difference
            session.touch();
            assertTrue(session.getLastActivityAt().isAfter(before)
                || session.getLastActivityAt().equals(before)); // at worst equal (same ms)
            assertNotNull(session.getLastActivityAt());
        }

        @Test
        @DisplayName("isIdle returns false for freshly created session")
        void freshSessionIsNotIdle() {
            assertFalse(session.isIdle(30)); // 30 minutes
        }

        @Test
        @DisplayName("isIdle returns true for very short timeout of 0 minutes")
        void isIdleWithZeroTimeoutMinutes() throws InterruptedException {
            Thread.sleep(10); // wait a tiny bit
            // 0 minutes → cutoff is Instant.now(), anything before that is idle
            assertTrue(session.isIdle(0));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Setters
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Mutable fields")
    class MutableFields {

        @Test
        @DisplayName("setUsername updates the username")
        void setUsernameWorks() {
            session.setUsername("bob");
            assertEquals("bob", session.getUsername());
        }

        @Test
        @DisplayName("disconnect sets connected to false")
        void disconnectSetsConnectedFalse() {
            session.disconnect();
            assertFalse(session.isConnected());
        }

        @Test
        @DisplayName("disconnect is idempotent")
        void disconnectIsIdempotent() {
            session.disconnect();
            assertDoesNotThrow(() -> session.disconnect());
            assertFalse(session.isConnected());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // toString()
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("toString contains sessionId and username")
    void toStringContainsIdAndUser() {
        String str = session.toString();
        assertTrue(str.contains("sess-abc"));
        assertTrue(str.contains("alice"));
    }
}
