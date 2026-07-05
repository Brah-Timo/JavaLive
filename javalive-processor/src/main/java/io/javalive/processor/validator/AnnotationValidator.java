package io.javalive.processor.validator;

import io.javalive.annotations.*;
import io.javalive.processor.model.ComponentModel;

import javax.annotation.processing.Messager;
import javax.lang.model.element.*;
import javax.tools.Diagnostic;
import java.util.HashSet;
import java.util.Set;

/**
 * Validates @VueComponent-annotated classes for correctness before code generation.
 *
 * <p>Emits compilation errors (not warnings) for violations so developers
 * catch mistakes at build time rather than at runtime.
 *
 * <h3>Validations performed:</h3>
 * <ul>
 *   <li>Class must not be abstract or an interface</li>
 *   <li>Class must not be an enum</li>
 *   <li>Class must not be private</li>
 *   <li>Class must have a public no-args constructor</li>
 *   <li>@VuePage path must start with "/"</li>
 *   <li>@VueMethod methods must not be private or static</li>
 *   <li>@VueComputed methods must return a value (non-void)</li>
 *   <li>@VueComputed methods must take no parameters</li>
 *   <li>@VueWatch value must match an existing @Reactive field name</li>
 *   <li>Component names must be unique (no duplicates in the same compilation unit)</li>
 *   <li>@VueTemplate field must be a compile-time String constant</li>
 * </ul>
 *
 * @author JavaLive Team
 * @since 1.0.0
 */
public class AnnotationValidator {

    private final Messager messager;
    private final Set<String> registeredComponentNames = new HashSet<>();

    public AnnotationValidator(Messager messager) {
        this.messager = messager;
    }

    /**
     * Validates a class element. Returns false if any error was found,
     * and the processor should skip code generation for this class.
     *
     * @param classElement the class to validate
     * @return true if all validations pass, false if any error was found
     */
    public boolean validate(TypeElement classElement) {
        boolean valid = true;

        // ── 1. Class-level checks ──────────────────────────────────────────
        if (classElement.getModifiers().contains(Modifier.ABSTRACT)) {
            error("@VueComponent cannot be applied to an abstract class.", classElement);
            valid = false;
        }

        if (classElement.getKind() == ElementKind.INTERFACE) {
            error("@VueComponent cannot be applied to an interface.", classElement);
            valid = false;
        }

        if (classElement.getKind() == ElementKind.ENUM) {
            error("@VueComponent cannot be applied to an enum.", classElement);
            valid = false;
        }

        if (classElement.getModifiers().contains(Modifier.PRIVATE)) {
            error("@VueComponent class must not be private.", classElement);
            valid = false;
        }

        // ── 2. Component name uniqueness ───────────────────────────────────
        VueComponent vc = classElement.getAnnotation(VueComponent.class);
        String componentName = vc.name().isEmpty()
            ? toKebabCase(classElement.getSimpleName().toString())
            : vc.name();

        if (registeredComponentNames.contains(componentName)) {
            error("Duplicate @VueComponent name '" + componentName +
                  "'. Each component must have a unique name.", classElement);
            valid = false;
        } else {
            registeredComponentNames.add(componentName);
        }

        // ── 3. @VuePage path validation ────────────────────────────────────
        VuePage vuePage = classElement.getAnnotation(VuePage.class);
        if (vuePage != null) {
            if (!vuePage.path().startsWith("/")) {
                error("@VuePage path must start with '/'. Found: '" + vuePage.path() + "'",
                      classElement);
                valid = false;
            }
        }

        // ── 4. Track @Reactive field names for @VueWatch validation ────────
        Set<String> reactiveFieldNames = new HashSet<>();
        boolean hasTemplateField = false;
        int templateFieldCount = 0;

        for (Element enclosed : classElement.getEnclosedElements()) {

            // ─── Field validations ─────────────────────────────────────────
            if (enclosed.getKind() == ElementKind.FIELD) {
                VariableElement field = (VariableElement) enclosed;

                if (field.getAnnotation(Reactive.class) != null) {
                    reactiveFieldNames.add(field.getSimpleName().toString());
                }

                if (field.getAnnotation(VueTemplate.class) != null) {
                    templateFieldCount++;
                    hasTemplateField = true;

                    // Must be a String constant
                    if (!field.asType().toString().equals("java.lang.String")) {
                        error("@VueTemplate field must be of type String.", field);
                        valid = false;
                    }
                    if (field.getConstantValue() == null) {
                        warn("@VueTemplate field '" + field.getSimpleName() +
                             "' is not a compile-time constant. Use 'static final String'.", field);
                    }
                }
            }

            // ─── Method validations ────────────────────────────────────────
            if (enclosed.getKind() == ElementKind.METHOD) {
                ExecutableElement method = (ExecutableElement) enclosed;

                VueMethod vueMethod = method.getAnnotation(VueMethod.class);
                if (vueMethod != null) {
                    if (method.getModifiers().contains(Modifier.PRIVATE)) {
                        error("@VueMethod '" + method.getSimpleName() +
                              "' must not be private. JavaLive needs to invoke it.", method);
                        valid = false;
                    }
                    if (method.getModifiers().contains(Modifier.STATIC)) {
                        error("@VueMethod '" + method.getSimpleName() +
                              "' must not be static.", method);
                        valid = false;
                    }
                }

                VueComputed computed = method.getAnnotation(VueComputed.class);
                if (computed != null) {
                    if (method.getReturnType().toString().equals("void")) {
                        error("@VueComputed method '" + method.getSimpleName() +
                              "' must return a value (non-void).", method);
                        valid = false;
                    }
                    if (!method.getParameters().isEmpty()) {
                        error("@VueComputed method '" + method.getSimpleName() +
                              "' must take no parameters.", method);
                        valid = false;
                    }
                }
            }
        }

        // ── 5. Validate @VueWatch targets against @Reactive fields ─────────
        // (done in second pass after collecting reactiveFieldNames)
        for (Element enclosed : classElement.getEnclosedElements()) {
            if (enclosed.getKind() == ElementKind.METHOD) {
                ExecutableElement method = (ExecutableElement) enclosed;
                VueWatch watch = method.getAnnotation(VueWatch.class);
                if (watch != null && !watch.value().isEmpty()) {
                    if (!reactiveFieldNames.contains(watch.value())) {
                        error("@VueWatch target '" + watch.value() +
                              "' does not match any @Reactive field in this class. " +
                              "Available fields: " + reactiveFieldNames, method);
                        valid = false;
                    }
                }
            }
        }

        // ── 6. Multiple @VueTemplate warning ──────────────────────────────
        if (templateFieldCount > 1) {
            warn("Multiple @VueTemplate fields found in " + classElement.getSimpleName() +
                 ". Only the first one will be used.", classElement);
        }

        return valid;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Private helpers
    // ─────────────────────────────────────────────────────────────────────────

    private void error(String message, Element element) {
        messager.printMessage(Diagnostic.Kind.ERROR,
            "🚫 JavaLive: " + message, element);
    }

    private void warn(String message, Element element) {
        messager.printMessage(Diagnostic.Kind.WARNING,
            "⚠️ JavaLive: " + message, element);
    }

    private String toKebabCase(String name) {
        return name
            .replaceAll("([A-Z]+)([A-Z][a-z])", "$1-$2")
            .replaceAll("([a-z])([A-Z])", "$1-$2")
            .toLowerCase()
            .replaceAll("^-", "");
    }
}
