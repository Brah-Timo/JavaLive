package com.example.pages;

import io.javalive.annotations.*;
import org.springframework.stereotype.Controller;

/**
 * Dashboard page — demonstrates @VuePage + @VueLifecycle + @VueComputed.
 */
@Controller
@VueComponent("dashboard")
@VuePage(path = "/dashboard", name = "Dashboard", requiresAuth = false, layout = "default")
public class Dashboard {

    @Reactive
    public int visitCount = 0;

    @Reactive
    public String welcomeMessage = "Welcome to JavaLive!";

    @Reactive
    public boolean darkMode = false;

    @VueLifecycle(hook = "onMounted")
    public void onMount() {
        this.visitCount++;
    }

    @VueMethod
    public void toggleDarkMode() {
        this.darkMode = !this.darkMode;
    }

    @VueComputed
    public String themeLabel() {
        return darkMode ? "🌙 Dark Mode" : "☀️ Light Mode";
    }

    @VueTemplate
    static final String template = """
        <div class="dashboard" :class="{ 'dark': state.darkMode }">
            <div class="dashboard-header">
                <h1>{{ state.welcomeMessage }}</h1>
                <button @click="toggleDarkMode">{{ themeLabel }}</button>
            </div>
            <div class="stats">
                <div class="stat-card">
                    <span class="stat-label">Page Visits (this session)</span>
                    <span class="stat-value">{{ state.visitCount }}</span>
                </div>
            </div>
            <div class="widgets">
                <counter-widget />
            </div>
        </div>
    """;
}
