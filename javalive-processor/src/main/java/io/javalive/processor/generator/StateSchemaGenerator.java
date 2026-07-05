package io.javalive.processor.generator;

import io.javalive.processor.model.*;

import javax.annotation.processing.ProcessingEnvironment;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * Generates a JSON state schema file for each @VueComponent.
 *
 * <p>The schema is loaded by the JavaLive client runtime at startup to:
 * <ul>
 *   <li>Initialize the correct default state for each component</li>
 *   <li>Validate incoming state patches from the server</li>
 *   <li>Power the DevTools inspector (field types, scopes, default values)</li>
 *   <li>Enable the StateManager to create initial state without reflection</li>
 * </ul>
 *
 * <h3>Generated file location:</h3>
 * {@code static/javalive/schemas/{component-name}.schema.json}
 *
 * <h3>Example output for a "counter" component:</h3>
 * <pre>
 * {
 *   "name": "counter",
 *   "javaClass": "com.example.CounterWidget",
 *   "fields": [
 *     {
 *       "name": "count",
 *       "javaType": "int",
 *       "vueType": "Number",
 *       "defaultValue": "0",
 *       "scope": "session",
 *       "persist": false
 *     }
 *   ],
 *   "methods": ["increment", "decrement", "reset"],
 *   "computed": ["isZero", "isNegative"],
 *   "route": "/counter"
 * }
 * </pre>
 *
 * @author JavaLive Team
 * @since 1.0.0
 */
public class StateSchemaGenerator {

    private final ProcessingEnvironment processingEnv;

    public StateSchemaGenerator(ProcessingEnvironment processingEnv) {
        this.processingEnv = processingEnv;
    }

    /**
     * Generates the JSON schema file for the given component.
     *
     * @param model the parsed component model
     */
    public void generate(ComponentModel model) {
        String outputPath = "static/javalive/schemas/" + model.getComponentName() + ".schema.json";

        try {
            FileObject file = processingEnv.getFiler()
                .createResource(StandardLocation.CLASS_OUTPUT, "", outputPath);

            try (PrintWriter out = new PrintWriter(file.openWriter())) {
                writeSchema(out, model);
            }
        } catch (IOException e) {
            throw new RuntimeException(
                "JavaLive: Failed to generate state schema for " + model.getJavaClassName(), e);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Private generation methods
    // ─────────────────────────────────────────────────────────────────────────

    private void writeSchema(PrintWriter out, ComponentModel model) {
        out.println("{");
        out.println("  \"_generated\": true,");
        out.println("  \"_generator\": \"JavaLive Annotation Processor\",");
        out.println("  \"name\": \"" + model.getComponentName() + "\",");
        out.println("  \"javaClass\": \"" + model.getPackageName() + "." + model.getJavaClassName() + "\",");

        // Route info
        if (model.isPage()) {
            out.println("  \"route\": {");
            out.println("    \"path\": \"" + model.getVuePath() + "\",");
            out.println("    \"name\": \"" + model.getRouteName() + "\",");
            out.println("    \"requiresAuth\": " + model.isRequiresAuth() + ",");
            out.println("    \"layout\": \"" + (model.getLayoutName() != null ? model.getLayoutName() : "default") + "\"");
            out.println("  },");
        } else {
            out.println("  \"route\": null,");
        }

        // Reactive fields
        out.println("  \"fields\": [");
        for (int i = 0; i < model.getReactiveFields().size(); i++) {
            FieldModel field = model.getReactiveFields().get(i);
            boolean last = (i == model.getReactiveFields().size() - 1);
            out.println("    {");
            out.println("      \"name\": \"" + field.getJavaName() + "\",");
            out.println("      \"javaType\": \"" + escapeJson(field.getJavaType()) + "\",");
            out.println("      \"vueType\": \"" + field.getVueType() + "\",");
            out.println("      \"defaultValue\": " + fieldDefaultAsJson(field) + ",");
            out.println("      \"scope\": \"" + field.getScope() + "\",");
            out.println("      \"persist\": " + field.isPersist() + ",");
            out.println("      \"label\": \"" + escapeJson(field.getLabel()) + "\"");
            out.println("    }" + (last ? "" : ","));
        }
        out.println("  ],");

        // Props
        out.println("  \"props\": [");
        for (int i = 0; i < model.getPropFields().size(); i++) {
            FieldModel prop = model.getPropFields().get(i);
            boolean last = (i == model.getPropFields().size() - 1);
            out.println("    {");
            out.println("      \"name\": \"" + prop.getJavaName() + "\",");
            out.println("      \"vueType\": \"" + prop.getVueType() + "\",");
            out.println("      \"required\": " + prop.isRequired());
            out.println("    }" + (last ? "" : ","));
        }
        out.println("  ],");

        // Methods
        out.println("  \"methods\": [");
        for (int i = 0; i < model.getVueMethods().size(); i++) {
            MethodModel method = model.getVueMethods().get(i);
            boolean last = (i == model.getVueMethods().size() - 1);
            out.println("    {");
            out.println("      \"name\": \"" + method.getVueName() + "\",");
            out.println("      \"javaName\": \"" + method.getJavaName() + "\",");
            out.println("      \"loading\": " + method.isShowLoading() + ",");
            out.println("      \"debounce\": " + method.getDebounce() + ",");
            out.println("      \"params\": " + jsonStringArray(method.getParamNames()));
            out.println("    }" + (last ? "" : ","));
        }
        out.println("  ],");

        // Computed
        out.println("  \"computed\": " + jsonStringArray(
            model.getComputedMethods().stream()
                .map(MethodModel::getVueName)
                .collect(java.util.stream.Collectors.toList())
        ) + ",");

        // Lifecycle hooks
        out.println("  \"lifecycleHooks\": " + jsonStringArray(
            model.getLifecycleMethods().stream()
                .map(MethodModel::getLifecycleHook)
                .collect(java.util.stream.Collectors.toList())
        ));

        out.println("}");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // JSON helper utilities
    // ─────────────────────────────────────────────────────────────────────────

    private String escapeJson(String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private String jsonStringArray(java.util.List<String> list) {
        if (list == null || list.isEmpty()) return "[]";
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < list.size(); i++) {
            sb.append("\"").append(escapeJson(list.get(i))).append("\"");
            if (i < list.size() - 1) sb.append(", ");
        }
        sb.append("]");
        return sb.toString();
    }

    private String fieldDefaultAsJson(FieldModel field) {
        String dv = field.getDefaultValue();
        if (dv == null || dv.equals("null")) return "null";
        if (dv.equals("0") || dv.equals("false") || dv.equals("true")) return dv;
        if (dv.equals("[]") || dv.equals("{}")) return dv;
        // String types need JSON quotes
        return "\"" + escapeJson(dv.replaceAll("^'|'$", "")) + "\"";
    }
}
