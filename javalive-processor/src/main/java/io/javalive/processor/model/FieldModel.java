package io.javalive.processor.model;

/**
 * Internal model representing a single field from a @VueComponent class.
 *
 * <p>Built by {@link io.javalive.processor.parser.ClassParser} from either
 * a {@code @Reactive} field or a {@code @VueProp} field, and consumed by
 * all generators to produce the corresponding Vue and Java artifacts.
 *
 * @author JavaLive Team
 * @since 1.0.0
 */
public class FieldModel {

    // ─── Java Side ──────────────────────────────────────────────────────────
    /** The Java field name exactly as declared (e.g., "searchQuery"). */
    private String javaName;

    /** Fully qualified Java type (e.g., "java.util.List<com.example.User>"). */
    private String javaType;

    /** Simple Java type for display purposes (e.g., "List<User>"). */
    private String javaSimpleType;

    // ─── Vue / JavaScript Side ───────────────────────────────────────────────
    /** Vue constructor type: "Number", "String", "Boolean", "Array", "Object". */
    private String vueType;

    /** JavaScript default value literal (e.g., "0", "''", "false", "[]", "{}"). */
    private String defaultValue;

    // ─── Reactive Configuration ──────────────────────────────────────────────
    /** "session" | "global" — scope of this reactive field. */
    private String scope = "session";

    /** Whether to persist this field to the database automatically. */
    private boolean persist = false;

    /** Optional human-readable label for DevTools. */
    private String label = "";

    // ─── Prop Configuration ──────────────────────────────────────────────────
    /** True if this is a @VueProp field (read-only from parent). */
    private boolean isProp = false;

    /** True if this prop is required. */
    private boolean required = true;

    /** JavaScript validator function body (for props). */
    private String validator = "";

    // ─── Template Configuration ──────────────────────────────────────────────
    /** True if this field holds the @VueTemplate string. */
    private boolean isTemplate = false;

    // ─────────────────────────────────────────────────────────────────────────
    // Getters and Setters
    // ─────────────────────────────────────────────────────────────────────────

    public String getJavaName() { return javaName; }
    public void setJavaName(String javaName) { this.javaName = javaName; }

    public String getJavaType() { return javaType; }
    public void setJavaType(String javaType) { this.javaType = javaType; }

    public String getJavaSimpleType() { return javaSimpleType; }
    public void setJavaSimpleType(String javaSimpleType) { this.javaSimpleType = javaSimpleType; }

    public String getVueType() { return vueType; }
    public void setVueType(String vueType) { this.vueType = vueType; }

    public String getDefaultValue() { return defaultValue; }
    public void setDefaultValue(String defaultValue) { this.defaultValue = defaultValue; }

    public String getScope() { return scope; }
    public void setScope(String scope) { this.scope = scope; }

    public boolean isPersist() { return persist; }
    public void setPersist(boolean persist) { this.persist = persist; }

    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }

    public boolean isProp() { return isProp; }
    public void setProp(boolean prop) { isProp = prop; }

    public boolean isRequired() { return required; }
    public void setRequired(boolean required) { this.required = required; }

    public String getValidator() { return validator; }
    public void setValidator(String validator) { this.validator = validator; }

    public boolean isTemplate() { return isTemplate; }
    public void setTemplate(boolean template) { isTemplate = template; }

    // ─────────────────────────────────────────────────────────────────────────
    // Derived helpers
    // ─────────────────────────────────────────────────────────────────────────

    /** Returns true if this field is global-scoped reactive state. */
    public boolean isGlobal() {
        return "global".equals(scope);
    }

    /** Returns the field name in camelCase (same as javaName for fields). */
    public String getCamelName() {
        return javaName;
    }

    @Override
    public String toString() {
        return "FieldModel{" +
               "javaName='" + javaName + '\'' +
               ", javaType='" + javaType + '\'' +
               ", vueType='" + vueType + '\'' +
               ", isProp=" + isProp +
               ", scope='" + scope + '\'' +
               '}';
    }
}
