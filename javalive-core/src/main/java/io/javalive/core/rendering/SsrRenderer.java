package io.javalive.core.rendering;

import io.javalive.core.state.StateManager;
import io.javalive.core.state.StateSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

/**
 * Performs Server-Side Rendering (SSR) for JavaLive pages.
 *
 * <p>When a browser first requests a @VuePage URL, JavaLive serves a complete
 * HTML response with:
 * <ol>
 *   <li>The page structure and layout (from the Java template)</li>
 *   <li>The initial component state embedded as JSON (hydration data)</li>
 *   <li>The Vue application bootstrap script</li>
 *   <li>The generated Vue component scripts</li>
 * </ol>
 *
 * <p>This eliminates the blank-page-flash that pure SPAs suffer from on first load.
 * After hydration, the WebSocket takes over and all further updates are real-time.
 *
 * <h3>SSR Flow:</h3>
 * <pre>
 * Browser → GET /dashboard
 *                ↓
 *         SsrRenderer.render("dashboard", sessionId)
 *                ↓
 *         StateManager.getOrCreate(sessionId, "dashboard")
 *                ↓
 *         Inject hydration data into HTML
 *                ↓
 * Browser ← Full HTML with embedded state
 *                ↓
 *         Client runtime reads #javalive-state script
 *                ↓
 *         Vue mounts with pre-populated state
 *                ↓
 *         WebSocket connects (no flicker)
 * </pre>
 *
 * @author JavaLive Team
 * @since 1.0.0
 */
@Component
public class SsrRenderer {

    private static final Logger log = LoggerFactory.getLogger(SsrRenderer.class);

    private final StateManager stateManager;
    private final StateSerializer stateSerializer;

    public SsrRenderer(StateManager stateManager, StateSerializer stateSerializer) {
        this.stateManager    = stateManager;
        this.stateSerializer = stateSerializer;
    }

    /**
     * Renders the initial HTML for a component page.
     *
     * @param componentName the component's kebab-case name
     * @param sessionId     the session ID (creates one if null)
     * @return the hydration data to embed in the page
     */
    public HydrationData prepareHydration(String componentName, String sessionId) {
        if (sessionId == null) {
            sessionId = "ssr-" + UUID.randomUUID().toString().substring(0, 8);
        }

        // Initialize or retrieve state for this session
        Map<String, Object> state = stateManager.getOrCreate(sessionId, componentName);

        HydrationData data = new HydrationData(componentName, state);
        data.setSessionId(sessionId);

        log.debug("JavaLive SSR: Prepared hydration for [{}/{}]", sessionId, componentName);

        return data;
    }

    /**
     * Injects the hydration script tag into an HTML page.
     *
     * <p>Call this when building the initial page response. The resulting HTML
     * should have the {@code <script id="javalive-state">} tag inserted just
     * before the closing {@code </body>} tag.
     *
     * @param html          the raw HTML to enhance
     * @param hydrationData the hydration data to embed
     * @return HTML with embedded hydration data
     */
    public String injectHydration(String html, HydrationData hydrationData) {
        String scriptTag = hydrationData.toScriptTag();

        // Insert before </body>
        if (html.contains("</body>")) {
            return html.replace("</body>", "\n" + scriptTag + "\n</body>");
        }

        // Fallback: append at the end
        return html + "\n" + scriptTag;
    }

    /**
     * Generates the minimal HTML shell for a JavaLive page.
     * Redirects to the SPA shell ({@code /app.html}) so Vue Router
     * handles all navigation client-side.
     *
     * <p>The shell includes:
     * <ul>
     *   <li>SockJS + @stomp/stompjs from CDN (required for STOMP over WebSocket)</li>
     *   <li>Vue 3 + vue-router via importmap (ES modules)</li>
     *   <li>The generated {@code /javalive/app.js} bootstrap</li>
     * </ul>
     *
     * @param title         page title
     * @param componentName the Vue component name (used as page title fallback)
     * @return complete HTML shell for SSR delivery
     */
    public String generateShell(String title, String componentName) {
        return """
            <!DOCTYPE html>
            <html lang="en">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>%s — JavaLive</title>
                <!-- SockJS (Spring STOMP requires SockJS transport) -->
                <script src="https://cdn.jsdelivr.net/npm/sockjs-client@1/dist/sockjs.min.js"></script>
                <!-- @stomp/stompjs — exposes window.StompJs -->
                <script src="https://cdn.jsdelivr.net/npm/@stomp/stompjs@7/bundles/stomp.umd.min.js"></script>
                <!-- importmap: Vue 3 + Vue Router as ES modules -->
                <script type="importmap">
                {
                    "imports": {
                        "vue":        "https://cdn.jsdelivr.net/npm/vue@3/dist/vue.esm-browser.prod.js",
                        "vue-router": "https://cdn.jsdelivr.net/npm/vue-router@4/dist/vue-router.esm-browser.js"
                    }
                }
                </script>
                <link rel="stylesheet" href="/javalive/javalive.css">
            </head>
            <body>
                <div id="app"></div>
                <script type="module" src="/javalive/app.js"></script>
            </body>
            </html>
            """.formatted(title);
    }
}
