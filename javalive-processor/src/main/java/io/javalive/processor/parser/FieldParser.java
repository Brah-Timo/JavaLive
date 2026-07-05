package io.javalive.processor.parser;

import io.javalive.annotations.*;
import io.javalive.processor.model.FieldModel;
import io.javalive.processor.model.TemplateModel;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.VariableElement;
import javax.tools.Diagnostic;

/**
 * Parses individual field elements from a @VueComponent class into FieldModel instances.
 *
 * <p>Handles three distinct field types:
 * <ul>
 *   <li>{@link Reactive} → server-managed live state</li>
 *   <li>{@link VueProp} → read-only input from parent component</li>
 *   <li>{@link VueTemplate} → inline Vue template string</li>
 * </ul>
 *
 * @author JavaLive Team
 * @since 1.0.0
 */
public class FieldParser {

    private final ProcessingEnvironment processingEnv;

    public FieldParser(ProcessingEnvironment processingEnv) {
        this.processingEnv = processingEnv;
    }

    /**
     * Parses a {@code @Reactive} field into a FieldModel.
     *
     * @param field    the field element from the APT
     * @param reactive the @Reactive annotation instance
     * @return a populated FieldModel
     */
    public FieldModel parseReactiveField(VariableElement field, Reactive reactive) {
        FieldModel model = createBase(field);
        model.setScope(reactive.scope());
        model.setPersist(reactive.persist());
        model.setLabel(reactive.label().isEmpty() ? field.getSimpleName().toString() : reactive.label());
        model.setProp(false);
        return model;
    }

    /**
     * Parses a {@code @VueProp} field into a FieldModel.
     *
     * @param field the field element from the APT
     * @param prop  the @VueProp annotation instance
     * @return a populated FieldModel
     */
    public FieldModel parsePropField(VariableElement field, VueProp prop) {
        FieldModel model = createBase(field);
        model.setProp(true);
        model.setRequired(prop.required());
        model.setValidator(prop.validator());

        // Use annotation's defaultValue if provided; otherwise infer from type
        if (!prop.defaultValue().isEmpty()) {
            model.setDefaultValue(prop.defaultValue());
        }
        return model;
    }

    /**
     * Parses a {@code @VueTemplate} field into a TemplateModel.
     *
     * <p>The field must be a compile-time constant String. If it is not,
     * a compilation error is emitted.
     *
     * @param field the field element
     * @return a TemplateModel, or null if the field is not a constant
     */
    public TemplateModel parseTemplateField(VariableElement field) {
        Object constantValue = field.getConstantValue();
        if (constantValue == null) {
            processingEnv.getMessager().printMessage(
                Diagnostic.Kind.ERROR,
                "@VueTemplate field must be a compile-time constant String. " +
                "Declare it as: static final String template = \"...\";",
                field
            );
            return null;
        }

        String rawHtml = constantValue.toString();

        TemplateModel tm = new TemplateModel();
        tm.setInline(true);
        tm.setHtml(rawHtml);

        // Check for embedded <style> block
        if (rawHtml.contains("<style")) {
            tm.setHasStyles(true);
            String styles = extractStyleBlock(rawHtml);
            tm.setStyles(styles);
        }

        return tm;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Private helpers
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Creates the common base FieldModel from a field element (name, type, defaults).
     */
    private FieldModel createBase(VariableElement field) {
        FieldModel model = new FieldModel();

        String javaName  = field.getSimpleName().toString();
        String javaType  = field.asType().toString();
        String simpleType = extractSimpleType(javaType);

        model.setJavaName(javaName);
        model.setJavaType(javaType);
        model.setJavaSimpleType(simpleType);
        model.setVueType(TypeMapper.toVueType(javaType));
        model.setDefaultValue(TypeMapper.toDefaultValue(javaType));

        return model;
    }

    /**
     * Extracts the simple type name from a fully qualified type.
     * e.g., "java.util.List<com.example.User>" → "List<User>"
     */
    private String extractSimpleType(String fullType) {
        if (fullType == null) return "Object";
        // Very basic simplification: remove package prefixes
        return fullType.replaceAll("([a-z][a-z0-9_]*\\.)+([A-Z])", "$2");
    }

    /**
     * Extracts the content of the first {@code <style>} block from a template.
     */
    private String extractStyleBlock(String html) {
        int start = html.indexOf("<style");
        int end   = html.indexOf("</style>");
        if (start < 0 || end < 0) return "";
        int contentStart = html.indexOf('>', start) + 1;
        return html.substring(contentStart, end).trim();
    }
}
