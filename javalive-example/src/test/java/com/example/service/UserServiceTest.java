package com.example.service;

import com.example.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link UserService}.
 * Tests all CRUD, search, and pagination operations.
 * UserService uses an in-memory list — no Spring context needed.
 */
@DisplayName("UserService")
class UserServiceTest {

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService();
        // The service starts with 5 pre-seeded users:
        // Alice Johnson, Bob Smith, Charlie Brown, Diana Prince, Edward Norton
    }

    // ─────────────────────────────────────────────────────────────────────────
    // findAll()
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("findAll()")
    class FindAll {

        @Test
        @DisplayName("returns all users on page 1 with large page size")
        void returnsAllUsersOnFirstPageLarge() {
            List<User> users = userService.findAll(1, 10);
            assertEquals(5, users.size());
        }

        @Test
        @DisplayName("returns correct page size")
        void returnsCorrectPageSize() {
            List<User> users = userService.findAll(1, 2);
            assertEquals(2, users.size());
        }

        @Test
        @DisplayName("page 2 returns next batch of users")
        void pageTwoReturnsNextBatch() {
            List<User> page1 = userService.findAll(1, 2);
            List<User> page2 = userService.findAll(2, 2);
            assertNotEquals(page1.get(0).getId(), page2.get(0).getId());
        }

        @Test
        @DisplayName("beyond last page returns empty list")
        void beyondLastPageIsEmpty() {
            List<User> users = userService.findAll(100, 10);
            assertTrue(users.isEmpty());
        }

        @Test
        @DisplayName("page 1 with page size 1 returns exactly 1 user")
        void pageOneSizeOne() {
            List<User> users = userService.findAll(1, 1);
            assertEquals(1, users.size());
        }

        @Test
        @DisplayName("last page returns remaining users")
        void lastPageReturnsRemaining() {
            // 5 users, page size 3: page 1 = [1,2,3], page 2 = [4,5]
            List<User> lastPage = userService.findAll(2, 3);
            assertEquals(2, lastPage.size());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // search()
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("search()")
    class Search {

        @Test
        @DisplayName("null query returns all users (delegates to findAll)")
        void nullQueryReturnsAll() {
            List<User> users = userService.search(null, 1, 10);
            assertEquals(5, users.size());
        }

        @Test
        @DisplayName("empty query returns all users")
        void emptyQueryReturnsAll() {
            List<User> users = userService.search("", 1, 10);
            assertEquals(5, users.size());
        }

        @Test
        @DisplayName("search by partial name is case-insensitive")
        void searchByNameCaseInsensitive() {
            List<User> users = userService.search("alice", 1, 10);
            assertEquals(1, users.size());
            assertEquals("Alice Johnson", users.get(0).getName());
        }

        @Test
        @DisplayName("search by email domain finds matching users")
        void searchByEmailDomain() {
            List<User> users = userService.search("@example.com", 1, 10);
            assertEquals(5, users.size()); // all have @example.com
        }

        @Test
        @DisplayName("search by specific email finds exactly one user")
        void searchBySpecificEmail() {
            List<User> users = userService.search("bob@example.com", 1, 10);
            assertEquals(1, users.size());
            assertEquals("Bob Smith", users.get(0).getName());
        }

        @Test
        @DisplayName("search for non-existent term returns empty list")
        void searchNonExistentReturnsEmpty() {
            List<User> users = userService.search("xyzzy-nonexistent", 1, 10);
            assertTrue(users.isEmpty());
        }

        @Test
        @DisplayName("search results are paginated")
        void searchResultsPaginated() {
            // All 5 have "example" in email, page 1 with size 2 → 2 results
            List<User> page1 = userService.search("example", 1, 2);
            assertEquals(2, page1.size());
        }

        @Test
        @DisplayName("uppercase search works (case-insensitive)")
        void uppercaseSearch() {
            List<User> users = userService.search("ALICE", 1, 10);
            assertEquals(1, users.size());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // save()
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("save()")
    class Save {

        @Test
        @DisplayName("saves new user and returns it with an assigned id")
        void savesNewUser() {
            User user = userService.save("Frank Miller", "frank@example.com");
            assertNotNull(user);
            assertNotNull(user.getId());
            assertEquals("Frank Miller", user.getName());
            assertEquals("frank@example.com", user.getEmail());
        }

        @Test
        @DisplayName("saved user appears in findAll")
        void savedUserAppearsInFindAll() {
            userService.save("Frank Miller", "frank@example.com");
            List<User> all = userService.findAll(1, 100);
            assertEquals(6, all.size());
        }

        @Test
        @DisplayName("consecutive saves get sequential ids")
        void consecutiveSavesGetSequentialIds() {
            User u1 = userService.save("User One", "u1@test.com");
            User u2 = userService.save("User Two", "u2@test.com");
            assertTrue(u2.getId() > u1.getId());
        }

        @Test
        @DisplayName("count increases after save")
        void countIncreasesAfterSave() {
            int before = userService.count();
            userService.save("New User", "new@test.com");
            assertEquals(before + 1, userService.count());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // delete()
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("delete()")
    class Delete {

        @Test
        @DisplayName("deletes existing user by id")
        void deletesExistingUser() {
            List<User> before = userService.findAll(1, 100);
            Long idToDelete = before.get(0).getId();
            userService.delete(idToDelete);
            List<User> after = userService.findAll(1, 100);
            assertEquals(4, after.size());
            assertTrue(after.stream().noneMatch(u -> u.getId().equals(idToDelete)));
        }

        @Test
        @DisplayName("count decreases after delete")
        void countDecreasesAfterDelete() {
            int before = userService.count();
            Long firstId = userService.findAll(1, 100).get(0).getId();
            userService.delete(firstId);
            assertEquals(before - 1, userService.count());
        }

        @Test
        @DisplayName("deleting non-existent id is safe (no-op)")
        void deleteNonExistentIsSafe() {
            assertDoesNotThrow(() -> userService.delete(9999L));
            assertEquals(5, userService.count()); // still 5
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // findById()
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("findById()")
    class FindById {

        @Test
        @DisplayName("returns user for known id")
        void returnsKnownUser() {
            Long firstId = userService.findAll(1, 100).get(0).getId();
            Optional<User> user = userService.findById(firstId);
            assertTrue(user.isPresent());
        }

        @Test
        @DisplayName("returns empty for unknown id")
        void returnsEmptyForUnknown() {
            Optional<User> user = userService.findById(99999L);
            assertFalse(user.isPresent());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // getTotalPages()
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getTotalPages()")
    class GetTotalPages {

        @Test
        @DisplayName("5 users / pageSize 5 = 1 page")
        void fiveUsersOnePage() {
            assertEquals(1, userService.getTotalPages(5));
        }

        @Test
        @DisplayName("5 users / pageSize 2 = 3 pages (ceiling)")
        void fiveUsersTwoPerPage() {
            assertEquals(3, userService.getTotalPages(2));
        }

        @Test
        @DisplayName("5 users / pageSize 10 = 1 page")
        void fiveUsersTenPerPage() {
            assertEquals(1, userService.getTotalPages(10));
        }

        @Test
        @DisplayName("5 users / pageSize 1 = 5 pages")
        void fiveUsersOnePerPage() {
            assertEquals(5, userService.getTotalPages(1));
        }

        @Test
        @DisplayName("6 users / pageSize 5 = 2 pages after save")
        void sixUsersTwoPages() {
            userService.save("Sixth User", "sixth@test.com");
            assertEquals(2, userService.getTotalPages(5));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // getSearchTotalPages()
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getSearchTotalPages()")
    class GetSearchTotalPages {

        @Test
        @DisplayName("null query delegates to total pages")
        void nullQueryDelegatesToTotal() {
            assertEquals(userService.getTotalPages(5),
                userService.getSearchTotalPages(null, 5));
        }

        @Test
        @DisplayName("empty query delegates to total pages")
        void emptyQueryDelegatesToTotal() {
            assertEquals(userService.getTotalPages(5),
                userService.getSearchTotalPages("", 5));
        }

        @Test
        @DisplayName("specific search has correct page count")
        void specificSearchPageCount() {
            // 1 user named Alice, 1 per page = 1 page
            assertEquals(1, userService.getSearchTotalPages("alice", 1));
        }

        @Test
        @DisplayName("no results = 0 pages")
        void noResultsZeroPages() {
            assertEquals(0, userService.getSearchTotalPages("xyzzy-nonexistent", 5));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // count()
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("count() returns initial user count of 5")
    void countReturnsInitialFive() {
        assertEquals(5, userService.count());
    }
}
