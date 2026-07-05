package io.javalive.core.session;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link LiveSessionManager}.
 * Covers createSession, removeSession, findById, sessionExists,
 * markComponentMounted/Unmounted, getStats, and getActiveSessionCount.
 */
@DisplayName("LiveSessionManager")
class LiveSessionManagerTest {

    private LiveSessionManager manager;

    @BeforeEach
    void setUp() {
        manager = new LiveSessionManager();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // createSession()
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("createSession()")
    class CreateSession {

        @Test
        @DisplayName("creates and returns a LiveSession")
        void createsSession() {
            LiveSession session = manager.createSession("sess-1", "alice", "10.0.0.1");
            assertNotNull(session);
            assertEquals("sess-1", session.getSessionId());
        }

        @Test
        @DisplayName("session is immediately findable after creation")
        void sessionFindableAfterCreate() {
            manager.createSession("sess-1", "alice", "10.0.0.1");
            assertTrue(manager.findById("sess-1").isPresent());
        }

        @Test
        @DisplayName("null username defaults to 'anonymous' in LiveSession")
        void nullUsernameDefaultsToAnonymous() {
            LiveSession session = manager.createSession("sess-anon", null, "127.0.0.1");
            assertEquals("anonymous", session.getUsername());
        }

        @Test
        @DisplayName("multiple sessions can be created independently")
        void multipleSessionsCreated() {
            manager.createSession("s1", "alice", "1.1.1.1");
            manager.createSession("s2", "bob",   "2.2.2.2");
            manager.createSession("s3", "carol", "3.3.3.3");
            assertEquals(3, manager.getActiveSessionCount());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // removeSession()
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("removeSession()")
    class RemoveSession {

        @Test
        @DisplayName("removes session and returns it")
        void removesAndReturnsSession() {
            manager.createSession("sess-1", "alice", "10.0.0.1");
            Optional<LiveSession> removed = manager.removeSession("sess-1");
            assertTrue(removed.isPresent());
            assertEquals("sess-1", removed.get().getSessionId());
        }

        @Test
        @DisplayName("session is no longer findable after removal")
        void sessionNotFindableAfterRemove() {
            manager.createSession("sess-1", "alice", "10.0.0.1");
            manager.removeSession("sess-1");
            assertFalse(manager.findById("sess-1").isPresent());
        }

        @Test
        @DisplayName("removed session is marked as disconnected")
        void removedSessionIsDisconnected() {
            manager.createSession("sess-1", "alice", "10.0.0.1");
            LiveSession session = manager.removeSession("sess-1").orElseThrow();
            assertFalse(session.isConnected());
        }

        @Test
        @DisplayName("removing non-existent session returns empty Optional")
        void removeNonExistentReturnsEmpty() {
            Optional<LiveSession> result = manager.removeSession("does-not-exist");
            assertFalse(result.isPresent());
        }

        @Test
        @DisplayName("active session count decreases after removal")
        void countDecreasesAfterRemoval() {
            manager.createSession("s1", "alice", "1.1.1.1");
            manager.createSession("s2", "bob",   "2.2.2.2");
            manager.removeSession("s1");
            assertEquals(1, manager.getActiveSessionCount());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // findById()
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("findById()")
    class FindById {

        @Test
        @DisplayName("returns session for known id")
        void returnsKnownSession() {
            manager.createSession("sess-1", "alice", "10.0.0.1");
            Optional<LiveSession> found = manager.findById("sess-1");
            assertTrue(found.isPresent());
            assertEquals("alice", found.get().getUsername());
        }

        @Test
        @DisplayName("returns empty for unknown id")
        void returnsEmptyForUnknown() {
            Optional<LiveSession> found = manager.findById("ghost");
            assertFalse(found.isPresent());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // sessionExists()
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("sessionExists()")
    class SessionExists {

        @Test
        @DisplayName("returns true for existing session")
        void trueForExistingSession() {
            manager.createSession("sess-1", "alice", "10.0.0.1");
            assertTrue(manager.sessionExists("sess-1"));
        }

        @Test
        @DisplayName("returns false for non-existing session")
        void falseForNonExistingSession() {
            assertFalse(manager.sessionExists("nope"));
        }

        @Test
        @DisplayName("returns false after session is removed")
        void falseAfterRemoval() {
            manager.createSession("sess-1", "alice", "10.0.0.1");
            manager.removeSession("sess-1");
            assertFalse(manager.sessionExists("sess-1"));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // markComponentMounted / Unmounted
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("markComponentMounted() and markComponentUnmounted()")
    class ComponentMounting {

        @Test
        @DisplayName("marking component mounted reflects in session")
        void markMountedReflectsInSession() {
            manager.createSession("sess-1", "alice", "10.0.0.1");
            manager.markComponentMounted("sess-1", "dashboard");

            LiveSession session = manager.findById("sess-1").orElseThrow();
            assertTrue(session.hasComponent("dashboard"));
        }

        @Test
        @DisplayName("marking component unmounted reflects in session")
        void markUnmountedReflectsInSession() {
            manager.createSession("sess-1", "alice", "10.0.0.1");
            manager.markComponentMounted("sess-1", "dashboard");
            manager.markComponentUnmounted("sess-1", "dashboard");

            LiveSession session = manager.findById("sess-1").orElseThrow();
            assertFalse(session.hasComponent("dashboard"));
        }

        @Test
        @DisplayName("markComponentMounted for unknown session is silent no-op")
        void mountForUnknownSessionIsSafe() {
            assertDoesNotThrow(() ->
                manager.markComponentMounted("unknown-session", "dashboard"));
        }

        @Test
        @DisplayName("markComponentUnmounted for unknown session is silent no-op")
        void unmountForUnknownSessionIsSafe() {
            assertDoesNotThrow(() ->
                manager.markComponentUnmounted("unknown-session", "dashboard"));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // getAllSessions() / findByUsername()
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getAllSessions() and findByUsername()")
    class CollectionMethods {

        @Test
        @DisplayName("getAllSessions returns all active sessions")
        void getAllSessionsReturnsAll() {
            manager.createSession("s1", "alice", "1.1.1.1");
            manager.createSession("s2", "bob",   "2.2.2.2");
            Collection<LiveSession> all = manager.getAllSessions();
            assertEquals(2, all.size());
        }

        @Test
        @DisplayName("getAllSessions returns empty when no sessions")
        void getAllSessionsEmptyWhenNone() {
            assertTrue(manager.getAllSessions().isEmpty());
        }

        @Test
        @DisplayName("findByUsername returns sessions for matching user")
        void findByUsernameMatchesUser() {
            manager.createSession("s1", "alice", "1.1.1.1");
            manager.createSession("s2", "alice", "2.2.2.2");
            manager.createSession("s3", "bob",   "3.3.3.3");

            Collection<LiveSession> aliceSessions = manager.findByUsername("alice");
            assertEquals(2, aliceSessions.size());
        }

        @Test
        @DisplayName("findByUsername returns empty for unknown user")
        void findByUsernameEmptyForUnknown() {
            manager.createSession("s1", "alice", "1.1.1.1");
            assertTrue(manager.findByUsername("ghost").isEmpty());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // getStats()
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getStats()")
    class GetStats {

        @Test
        @DisplayName("stats map contains 'activeSessions' key")
        void statsContainsActiveSessionsKey() {
            manager.createSession("s1", "alice", "1.1.1.1");
            Map<String, Object> stats = manager.getStats();
            assertTrue(stats.containsKey("activeSessions"));
        }

        @Test
        @DisplayName("activeSessions count is correct")
        void activeSessionsCountIsCorrect() {
            manager.createSession("s1", "alice", "1.1.1.1");
            manager.createSession("s2", "bob",   "2.2.2.2");
            Map<String, Object> stats = manager.getStats();
            assertEquals(2, stats.get("activeSessions"));
        }

        @Test
        @DisplayName("stats map contains 'sessions' list")
        void statsContainsSessionsList() {
            manager.createSession("s1", "alice", "1.1.1.1");
            Map<String, Object> stats = manager.getStats();
            assertTrue(stats.containsKey("sessions"));
        }
    }
}
