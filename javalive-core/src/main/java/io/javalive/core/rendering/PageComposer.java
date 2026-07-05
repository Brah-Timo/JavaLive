package io.javalive.core.rendering;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.ModelAndView;



/**
 * Composes complete HTML page responses for JavaLive @VuePage components.
 *
 * <p>Integrates with Spring MVC to handle initial page loads. When a browser
 * navigates to a @VuePage route, this composer:
 * <ol>
 *   <li>Gets or creates the HTTP session</li>
 *   <li>Prepares hydration data from {@link SsrRenderer}</li>
 *   <li>Returns a Spring {@link ModelAndView} that renders the page template</li>
 * </ol>
 *
 * @author JavaLive Team
 * @since 1.0.0
 */
@Component
public class PageComposer {

    private final SsrRenderer ssrRenderer;

    public PageComposer(SsrRenderer ssrRenderer) {
        this.ssrRenderer = ssrRenderer;
    }

    /**
     * Composes a full HTML response for a @VuePage component.
     *
     * @param componentName the Vue component name (e.g., "dashboard")
     * @param pageTitle     the page title
     * @param sessionId     the WebSocket session ID (from HTTP session or cookie)
     * @return a ModelAndView suitable for Spring MVC rendering
     */
    public ModelAndView composePage(String componentName, String pageTitle, String sessionId) {
        HydrationData hydration = ssrRenderer.prepareHydration(componentName, sessionId);

        ModelAndView mav = new ModelAndView("javalive/page");
        mav.addObject("componentName", componentName);
        mav.addObject("pageTitle", pageTitle);
        mav.addObject("hydrationJson", hydration.toJson());
        mav.addObject("hydrationScript", hydration.toScriptTag());
        mav.addObject("sessionId", hydration.getSessionId());

        return mav;
    }

    /**
     * Generates a self-contained HTML page string (no Thymeleaf required).
     * Used when the application doesn't have a template engine configured.
     *
     * @param componentName the Vue component name
     * @param pageTitle     the page title
     * @param sessionId     the session ID
     * @return complete HTML string
     */
    public String composeHtmlString(String componentName, String pageTitle, String sessionId) {
        HydrationData hydration = ssrRenderer.prepareHydration(componentName, sessionId);
        String shell = ssrRenderer.generateShell(pageTitle, componentName);
        return ssrRenderer.injectHydration(shell, hydration);
    }
}
