package io.javalive.processor.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Internal model representing a single method from a @VueComponent class.
 *
 * <p>Used for all method types: @VueMethod, @VueComputed, @VueWatch, @VueLifecycle.
 * Built by {@link io.javalive.processor.parser.ClassParser} and consumed by generators.
 *
 * @author JavaLive Team
 * @since 1.0.0
 */
public class MethodModel {

    // ─── Identity ─────────────────────────────────────────────────────────────
    /** The Java method name as declared. */
    private String javaName;

    /**
     * The name exposed in the Vue component.
     * For @VueMethod: the method name in JavaScript setup().
     * For @VueComputed: the computed property name.
     */
    private String vueName;

    /** Fully qualified return type (e.g., "java.util.List<com.example.User>"). */
    private String returnType;

    // ─── Parameters ───────────────────────────────────────────────────────────
    /** Parameter names in declaration order. */
    private List<String> paramNames = new ArrayList<>();

    /** Fully qualified parameter types in declaration order. */
    private List<String> paramTypes = new ArrayList<>();

    // ─── @VueMethod specifics ─────────────────────────────────────────────────
    /** Whether to show a loading indicator while this method runs. */
    private boolean showLoading = false;

    /** Debounce delay in milliseconds (0 = no debounce). */
    private int debounce = 0;

    /** Whether to prompt user for confirmation before dispatching. */
    private boolean confirm = false;

    /** Custom confirmation message. */
    private String confirmMessage = "Are you sure?";

    /** Whether this method emits a Vue event (@VueEmit). */
    private boolean emitsEvent = false;

    /** The event name to emit. */
    private String emitEventName = "";

    // ─── @VueWatch specifics ──────────────────────────────────────────────────
    /** The reactive field name this method watches. */
    private String watchTarget = "";

    /** Whether to use deep watching. */
    private boolean deep = false;

    /** Whether to execute immediately on mount. */
    private boolean immediate = false;

    /** Watcher debounce. */
    private int watchDebounce = 0;

    // ─── @VueLifecycle specifics ──────────────────────────────────────────────
    /** The Vue lifecycle hook: "onMounted", "onUnmounted", etc. */
    private String lifecycleHook = "";

    // ─── @VueComputed specifics ───────────────────────────────────────────────
    /** True if this is a computed property. */
    private boolean computed = false;

    // ─────────────────────────────────────────────────────────────────────────
    // Getters and Setters
    // ─────────────────────────────────────────────────────────────────────────

    public String getJavaName() { return javaName; }
    public void setJavaName(String javaName) { this.javaName = javaName; }

    public String getVueName() { return vueName != null && !vueName.isEmpty() ? vueName : javaName; }
    public void setVueName(String vueName) { this.vueName = vueName; }

    public String getReturnType() { return returnType; }
    public void setReturnType(String returnType) { this.returnType = returnType; }

    public List<String> getParamNames() { return paramNames; }
    public void setParamNames(List<String> paramNames) { this.paramNames = paramNames; }

    public List<String> getParamTypes() { return paramTypes; }
    public void setParamTypes(List<String> paramTypes) { this.paramTypes = paramTypes; }

    public boolean isShowLoading() { return showLoading; }
    public void setShowLoading(boolean showLoading) { this.showLoading = showLoading; }

    public int getDebounce() { return debounce; }
    public void setDebounce(int debounce) { this.debounce = debounce; }

    public boolean isConfirm() { return confirm; }
    public void setConfirm(boolean confirm) { this.confirm = confirm; }

    public String getConfirmMessage() { return confirmMessage; }
    public void setConfirmMessage(String confirmMessage) { this.confirmMessage = confirmMessage; }

    public boolean isEmitsEvent() { return emitsEvent; }
    public void setEmitsEvent(boolean emitsEvent) { this.emitsEvent = emitsEvent; }

    public String getEmitEventName() { return emitEventName; }
    public void setEmitEventName(String emitEventName) { this.emitEventName = emitEventName; }

    public String getWatchTarget() { return watchTarget; }
    public void setWatchTarget(String watchTarget) { this.watchTarget = watchTarget; }

    public boolean isDeep() { return deep; }
    public void setDeep(boolean deep) { this.deep = deep; }

    public boolean isImmediate() { return immediate; }
    public void setImmediate(boolean immediate) { this.immediate = immediate; }

    public int getWatchDebounce() { return watchDebounce; }
    public void setWatchDebounce(int watchDebounce) { this.watchDebounce = watchDebounce; }

    public String getLifecycleHook() { return lifecycleHook; }
    public void setLifecycleHook(String lifecycleHook) { this.lifecycleHook = lifecycleHook; }

    public boolean isComputed() { return computed; }
    public void setComputed(boolean computed) { this.computed = computed; }

    // ─────────────────────────────────────────────────────────────────────────
    // Derived helpers
    // ─────────────────────────────────────────────────────────────────────────

    /** Returns true if the method has no parameters. */
    public boolean hasNoParams() {
        return paramNames == null || paramNames.isEmpty();
    }

    /** Returns a comma-joined parameter name list for JavaScript: "a, b, c". */
    public String getParamNamesJoined() {
        if (paramNames == null || paramNames.isEmpty()) return "";
        return String.join(", ", paramNames);
    }

    /** Returns the JavaScript args array literal: "[]" or "[a, b]". */
    public String getArgsArrayLiteral() {
        if (paramNames == null || paramNames.isEmpty()) return "[]";
        return "[" + String.join(", ", paramNames) + "]";
    }

    /** True if return type is void. */
    public boolean isVoidReturn() {
        return returnType == null || returnType.equals("void");
    }

    @Override
    public String toString() {
        return "MethodModel{" +
               "javaName='" + javaName + '\'' +
               ", vueName='" + vueName + '\'' +
               ", returnType='" + returnType + '\'' +
               ", params=" + paramNames +
               '}';
    }
}
