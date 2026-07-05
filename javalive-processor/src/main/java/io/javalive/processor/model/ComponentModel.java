package io.javalive.processor.model;

import java.util.ArrayList;
import java.util.List;

/**
 * The central internal model for a single @VueComponent-annotated class.
 *
 * <p>This is the "bridge" object that flows through the entire processor pipeline:
 * <pre>
 *   Java Class → ClassParser → ComponentModel → [All Generators]
 *                                                  ├── SpringControllerGenerator
 *                                                  ├── VueComponentGenerator
 *                                                  ├── StateSchemaGenerator
 *                                                  └── RouterGenerator
 * </pre>
 *
 * <p>All information the generators need is contained here.
 *
 * @author JavaLive Team
 * @since 1.0.0
 */
public class ComponentModel {

    // ─── Class Identity ───────────────────────────────────────────────────────
    /** Simple class name (e.g., "Dashboard"). */
    private String javaClassName;

    /** Fully qualified package (e.g., "com.example.pages"). */
    private String packageName;

    /** Vue component name in kebab-case (e.g., "dashboard", "user-management"). */
    private String componentName;

    // ─── Routing (@VuePage) ───────────────────────────────────────────────────
    /** True if this component is also a routable page (@VuePage). */
    private boolean isPage;

    /** Vue Router path (e.g., "/dashboard", "/users/:id"). */
    private String vuePath;

    /** Named route in Vue Router (e.g., "Dashboard"). */
    private String routeName;

    /** Whether the route requires authentication. */
    private boolean requiresAuth;

    /** Layout name this page uses (e.g., "default", "admin"). */
    private String layoutName;

    /** Page title for browser tab. */
    private String pageTitle;

    /** True if this component is a layout (@VueLayout). */
    private boolean isLayout;

    /** Layout identifier (e.g., "default", "admin"). */
    private String layoutId;

    // ─── Fields ───────────────────────────────────────────────────────────────
    /** All @Reactive fields — the server-managed live state. */
    private List<FieldModel> reactiveFields = new ArrayList<>();

    /** All @VueProp fields — read-only inputs from parent components. */
    private List<FieldModel> propFields = new ArrayList<>();

    // ─── Methods ──────────────────────────────────────────────────────────────
    /** All @VueMethod methods — callable from the Vue template. */
    private List<MethodModel> vueMethods = new ArrayList<>();

    /** All @VueComputed methods — derived reactive values. */
    private List<MethodModel> computedMethods = new ArrayList<>();

    /** All @VueWatch methods — triggered on field changes. */
    private List<MethodModel> watchMethods = new ArrayList<>();

    /** All @VueLifecycle methods — bound to Vue lifecycle hooks. */
    private List<MethodModel> lifecycleMethods = new ArrayList<>();

    // ─── Template ─────────────────────────────────────────────────────────────
    /** The component's HTML template (inline or external file). */
    private TemplateModel template;

    // ─── Injected Spring Beans ────────────────────────────────────────────────
    /**
     * List of @Autowired field types found in the class.
     * Needed so the generated Controller can inject the same beans.
     */
    private List<String> autowiredTypes = new ArrayList<>();

    // ─────────────────────────────────────────────────────────────────────────
    // Getters and Setters
    // ─────────────────────────────────────────────────────────────────────────

    public String getJavaClassName() { return javaClassName; }
    public void setJavaClassName(String javaClassName) { this.javaClassName = javaClassName; }

    public String getPackageName() { return packageName; }
    public void setPackageName(String packageName) { this.packageName = packageName; }

    public String getComponentName() { return componentName; }
    public void setComponentName(String componentName) { this.componentName = componentName; }

    public boolean isPage() { return isPage; }
    public void setPage(boolean page) { isPage = page; }

    public String getVuePath() { return vuePath; }
    public void setVuePath(String vuePath) { this.vuePath = vuePath; }

    public String getRouteName() { return routeName; }
    public void setRouteName(String routeName) { this.routeName = routeName; }

    public boolean isRequiresAuth() { return requiresAuth; }
    public void setRequiresAuth(boolean requiresAuth) { this.requiresAuth = requiresAuth; }

    public String getLayoutName() { return layoutName; }
    public void setLayoutName(String layoutName) { this.layoutName = layoutName; }

    public String getPageTitle() { return pageTitle; }
    public void setPageTitle(String pageTitle) { this.pageTitle = pageTitle; }

    public boolean isLayout() { return isLayout; }
    public void setLayout(boolean layout) { isLayout = layout; }

    public String getLayoutId() { return layoutId; }
    public void setLayoutId(String layoutId) { this.layoutId = layoutId; }

    public List<FieldModel> getReactiveFields() { return reactiveFields; }
    public void setReactiveFields(List<FieldModel> reactiveFields) { this.reactiveFields = reactiveFields; }

    public List<FieldModel> getPropFields() { return propFields; }
    public void setPropFields(List<FieldModel> propFields) { this.propFields = propFields; }

    public List<MethodModel> getVueMethods() { return vueMethods; }
    public void setVueMethods(List<MethodModel> vueMethods) { this.vueMethods = vueMethods; }

    public List<MethodModel> getComputedMethods() { return computedMethods; }
    public void setComputedMethods(List<MethodModel> computedMethods) { this.computedMethods = computedMethods; }

    public List<MethodModel> getWatchMethods() { return watchMethods; }
    public void setWatchMethods(List<MethodModel> watchMethods) { this.watchMethods = watchMethods; }

    public List<MethodModel> getLifecycleMethods() { return lifecycleMethods; }
    public void setLifecycleMethods(List<MethodModel> lifecycleMethods) { this.lifecycleMethods = lifecycleMethods; }

    public TemplateModel getTemplate() { return template; }
    public void setTemplate(TemplateModel template) { this.template = template; }

    public List<String> getAutowiredTypes() { return autowiredTypes; }
    public void setAutowiredTypes(List<String> autowiredTypes) { this.autowiredTypes = autowiredTypes; }

    // ─────────────────────────────────────────────────────────────────────────
    // Derived helpers
    // ─────────────────────────────────────────────────────────────────────────

    /** Fully qualified name of the generated Controller class. */
    public String getGeneratedControllerQualifiedName() {
        return packageName + ".generated." + javaClassName + "LiveController";
    }

    /** Simple name of the generated Controller class. */
    public String getGeneratedControllerSimpleName() {
        return javaClassName + "LiveController";
    }

    /** Package name for generated classes. */
    public String getGeneratedPackageName() {
        return packageName + ".generated";
    }

    /** WebSocket topic path for state updates: "/topic/dashboard.state". */
    public String getStateTopic() {
        return "/topic/" + componentName + ".state";
    }

    /** WebSocket user-specific topic path. */
    public String getUserStateTopic() {
        return "/user" + getStateTopic();
    }

    /** Returns true if this component has any reactive fields (session or global). */
    public boolean hasReactiveState() {
        return reactiveFields != null && !reactiveFields.isEmpty();
    }

    /** Returns true if this component has any props. */
    public boolean hasProps() {
        return propFields != null && !propFields.isEmpty();
    }

    /** Returns true if this component has any template. */
    public boolean hasTemplate() {
        return template != null && template.hasContent();
    }

    /** Returns true if any reactive field is global-scoped. */
    public boolean hasGlobalState() {
        return reactiveFields.stream().anyMatch(FieldModel::isGlobal);
    }

    @Override
    public String toString() {
        return "ComponentModel{" +
               "className='" + javaClassName + '\'' +
               ", componentName='" + componentName + '\'' +
               ", isPage=" + isPage +
               ", vuePath='" + vuePath + '\'' +
               ", reactiveFields=" + reactiveFields.size() +
               ", vueMethods=" + vueMethods.size() +
               '}';
    }
}
