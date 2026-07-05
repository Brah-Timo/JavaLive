package com.example.pages;

import com.example.model.User;
import com.example.service.UserService;
import io.javalive.annotations.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import java.util.ArrayList;
import java.util.List;

/**
 * User Management page — full CRUD demo for JavaLive v0.1.0-alpha.
 *
 * Demonstrates:
 * - @VuePage          (registered as a route at /users)
 * - @VueLifecycle     (onMounted — loads initial data)
 * - @VueMethod        (search, goToPage, createUser, deleteUser)
 * - @VueComputed      (derived values from reactive state)
 * - @VueWatch(debounce=300) (auto-search as user types)
 * - @Reactive         (server-side reactive fields)
 * - @VueTemplate      (inline HTML template)
 */
@Controller
@VueComponent("user-management")
@VuePage(path = "/users", name = "UserManagement", requiresAuth = false)
public class UserManagement {

    @Autowired
    private UserService userService;

    // ── Reactive State ─────────────────────────────────────────────────────────

    @Reactive(scope = "session")
    public List<User> users = new ArrayList<>();

    @Reactive
    public String searchQuery = "";

    @Reactive
    public boolean isLoading = false;

    @Reactive
    public String errorMessage = "";

    @Reactive
    public String successMessage = "";

    @Reactive
    public int currentPage = 1;

    @Reactive
    public int totalPages = 1;

    // ── Form state for Create User modal ──────────────────────────────────────

    @Reactive
    public boolean showCreateModal = false;

    @Reactive
    public String newUserName = "";

    @Reactive
    public String newUserEmail = "";

    private static final int PAGE_SIZE = 20;

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    @VueLifecycle(hook = "onMounted")
    public void loadInitialData() {
        this.users      = userService.findAll(1, PAGE_SIZE);
        this.totalPages = userService.getTotalPages(PAGE_SIZE);
    }

    // ── Methods ───────────────────────────────────────────────────────────────

    @VueMethod(loading = true)
    public void search() {
        this.isLoading   = true;
        this.currentPage = 1;
        try {
            this.users        = userService.search(this.searchQuery, 1, PAGE_SIZE);
            this.totalPages   = userService.getSearchTotalPages(this.searchQuery, PAGE_SIZE);
            this.errorMessage = "";
        } catch (Exception e) {
            this.errorMessage = "Search failed: " + e.getMessage();
        } finally {
            this.isLoading = false;
        }
    }

    @VueMethod
    public void goToPage(int page) {
        if (page < 1 || page > this.totalPages) return;
        this.currentPage = page;
        this.users = userService.search(this.searchQuery, page, PAGE_SIZE);
    }

    /** Opens the Create User modal. */
    @VueMethod
    public void openCreateModal() {
        this.newUserName  = "";
        this.newUserEmail = "";
        this.showCreateModal = true;
        this.errorMessage    = "";
    }

    /** Closes the Create User modal without saving. */
    @VueMethod
    public void closeCreateModal() {
        this.showCreateModal = false;
        this.newUserName     = "";
        this.newUserEmail    = "";
    }

    /**
     * Creates a new user from the modal form fields.
     * Validates inputs, saves to the service, and refreshes the list.
     */
    @VueMethod(loading = true)
    public void createUser() {
        // Basic validation
        if (this.newUserName == null || this.newUserName.trim().isEmpty()) {
            this.errorMessage = "Name is required.";
            return;
        }
        if (this.newUserEmail == null || this.newUserEmail.trim().isEmpty()) {
            this.errorMessage = "Email is required.";
            return;
        }
        if (!this.newUserEmail.contains("@")) {
            this.errorMessage = "Please enter a valid email address.";
            return;
        }

        try {
            userService.save(this.newUserName.trim(), this.newUserEmail.trim().toLowerCase());
            this.errorMessage    = "";
            this.successMessage  = "✅ User \"" + this.newUserName.trim() + "\" created successfully!";
            this.showCreateModal = false;
            this.newUserName     = "";
            this.newUserEmail    = "";

            // Refresh the list
            this.users      = userService.search(this.searchQuery, this.currentPage, PAGE_SIZE);
            this.totalPages = userService.getSearchTotalPages(this.searchQuery, PAGE_SIZE);
        } catch (Exception e) {
            this.errorMessage = "Failed to create user: " + e.getMessage();
        }
    }

    @VueMethod(confirm = true, confirmMessage = "Are you sure you want to delete this user?")
    public void deleteUser(Long userId) {
        try {
            userService.delete(userId);
            this.users.removeIf(u -> u.getId().equals(userId));
            this.totalPages   = userService.getSearchTotalPages(this.searchQuery, PAGE_SIZE);
            this.errorMessage = "";
            this.successMessage = "🗑️ User deleted.";
        } catch (Exception e) {
            this.errorMessage = "Delete failed: " + e.getMessage();
        }
    }

    /** Clears the search field and reloads all users. */
    @VueMethod
    public void clearSearch() {
        this.searchQuery = "";
        this.currentPage = 1;
        this.users       = userService.findAll(1, PAGE_SIZE);
        this.totalPages  = userService.getTotalPages(PAGE_SIZE);
    }

    // ── Computed ──────────────────────────────────────────────────────────────

    @VueComputed
    public int usersCount() { return this.users.size(); }

    @VueComputed
    public boolean hasUsers() { return !this.users.isEmpty(); }

    // ── Watchers ──────────────────────────────────────────────────────────────

    @VueWatch(value = "searchQuery", debounce = 300)
    public void onSearchQueryChange(String newQuery, String oldQuery) {
        if (newQuery.length() >= 2 || newQuery.isEmpty()) {
            this.search();
        }
    }

    // ── Template ──────────────────────────────────────────────────────────────

    @VueTemplate
    static final String template = """
        <div class="user-management p-6">

            <!-- Connection warning -->
            <div v-if="!isConnected" class="alert alert-warning mb-4">
                ⚠️ Reconnecting to server...
            </div>

            <!-- Success message -->
            <div v-if="state.successMessage" class="alert alert-success mb-4" style="cursor:pointer" @click="state.successMessage=''">
                {{ state.successMessage }}
                <span style="margin-left:auto;opacity:0.6">✕</span>
            </div>

            <!-- Page header -->
            <div class="page-header flex justify-between items-center mb-6">
                <div>
                    <h1 class="text-2xl font-bold">👥 User Management</h1>
                    <p style="color:#64748b;font-size:0.875rem;margin-top:0.25rem">
                        {{ usersCount }} user(s) displayed
                    </p>
                </div>
                <button @click="openCreateModal" class="btn btn-primary">
                    ➕ New User
                </button>
            </div>

            <!-- Search bar -->
            <div class="search-bar flex gap-2 mb-6">
                <input
                    v-model="state.searchQuery"
                    placeholder="Search by name or email…"
                    class="input input-bordered flex-1"
                    @keyup.enter="search"
                />
                <button @click="search" :disabled="state.isLoading" class="btn btn-primary">
                    <span v-if="state.isLoading">⏳</span>
                    <span v-else>🔍</span>
                    Search
                </button>
                <button v-if="state.searchQuery" @click="clearSearch" class="btn btn-ghost">
                    ✕ Clear
                </button>
            </div>

            <!-- Error message -->
            <div v-if="state.errorMessage" class="alert alert-error mb-4">
                {{ state.errorMessage }}
            </div>

            <!-- Users table -->
            <table v-if="hasUsers" class="table w-full">
                <thead>
                    <tr>
                        <th style="width:3rem">#</th>
                        <th>Name</th>
                        <th>Email</th>
                        <th>Joined</th>
                        <th style="width:8rem">Actions</th>
                    </tr>
                </thead>
                <tbody>
                    <tr v-for="user in state.users" :key="user.id">
                        <td style="color:#64748b">{{ user.id }}</td>
                        <td style="font-weight:600">{{ user.name }}</td>
                        <td style="color:#94a3b8">{{ user.email }}</td>
                        <td style="color:#64748b;font-size:0.8125rem">{{ user.createdAt }}</td>
                        <td>
                            <button @click="deleteUser(user.id)" class="btn btn-error btn-xs">
                                🗑️ Delete
                            </button>
                        </td>
                    </tr>
                </tbody>
            </table>

            <!-- Empty state -->
            <div v-else class="text-center py-12 text-gray-400">
                <div style="font-size:3rem;margin-bottom:0.5rem">🔍</div>
                <p>No users found{{ state.searchQuery ? ' for "' + state.searchQuery + '"' : '' }}.</p>
                <button v-if="state.searchQuery" @click="clearSearch" class="btn btn-ghost" style="margin-top:1rem">
                    Show all users
                </button>
            </div>

            <!-- Pagination -->
            <div v-if="state.totalPages > 1" class="pagination mt-6">
                <button
                    @click="goToPage(state.currentPage - 1)"
                    :disabled="state.currentPage <= 1"
                    class="btn btn-outline btn-sm"
                >← Prev</button>
                <button
                    v-for="page in state.totalPages"
                    :key="page"
                    @click="goToPage(page)"
                    :class="['btn btn-sm', page === state.currentPage ? 'btn-primary' : 'btn-outline']"
                >{{ page }}</button>
                <button
                    @click="goToPage(state.currentPage + 1)"
                    :disabled="state.currentPage >= state.totalPages"
                    class="btn btn-outline btn-sm"
                >Next →</button>
            </div>

            <!-- Create User Modal -->
            <div v-if="state.showCreateModal" class="modal-overlay" @click.self="closeCreateModal">
                <div class="modal-box">
                    <h2 class="modal-title">➕ Create New User</h2>

                    <div v-if="state.errorMessage" class="alert alert-error mb-4" style="font-size:0.8rem">
                        {{ state.errorMessage }}
                    </div>

                    <div class="form-group">
                        <label class="form-label">Full Name *</label>
                        <input
                            v-model="state.newUserName"
                            class="input w-full"
                            placeholder="e.g. Jane Doe"
                            @keyup.enter="createUser"
                            autofocus
                        />
                    </div>

                    <div class="form-group">
                        <label class="form-label">Email Address *</label>
                        <input
                            v-model="state.newUserEmail"
                            type="email"
                            class="input w-full"
                            placeholder="e.g. jane@example.com"
                            @keyup.enter="createUser"
                        />
                    </div>

                    <div class="modal-actions">
                        <button @click="closeCreateModal" class="btn btn-ghost">Cancel</button>
                        <button
                            @click="createUser"
                            :disabled="isLoading || !state.newUserName || !state.newUserEmail"
                            class="btn btn-primary"
                        >
                            <span v-if="isLoading">⏳ Creating…</span>
                            <span v-else>✅ Create User</span>
                        </button>
                    </div>
                </div>
            </div>

        </div>
    """;
}
