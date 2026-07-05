package io.javalive.processor.parser;

import io.javalive.annotations.*;
import io.javalive.processor.model.*;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Parses a {@code @VueComponent}-annotated Java class into a {@link ComponentModel}.
 *
 * <p>This is the first step in the JavaLive processing pipeline. The resulting
 * {@link ComponentModel} is a complete, serializable description of everything
 * the generators need to produce Spring Controllers and Vue components.
 *
 * <p><b>Processing order:</b>
 * <ol>
 *   <li>Class-level annotations ({@code @VueComponent}, {@code @VuePage}, {@code @VueLayout})</li>
 *   <li>Fields ({@code @Reactive}, {@code @VueProp}, {@code @VueTemplate})</li>
 *   <li>Methods ({@code @VueMethod}, {@code @VueComputed}, {@code @VueWatch}, {@code @VueLifecycle})</li>
 * </ol>
 *
 * @author JavaLive Team
 * @since 1.0.0
 */
public class ClassParser {

    private final ProcessingEnvironment processingEnv;
    private final FieldParser fieldParser;
    private final TemplateParser templateParser;

    public ClassParser(ProcessingEnvironment processingEnv) {
        this.processingEnv = processingEnv;
        this.fieldParser   = new FieldParser(processingEnv);
        this.templateParser = new TemplateParser(processingEnv);
    }

    /**
     * Parses a class element into a full ComponentModel.
     *
     * @param classElement the TypeElement of the @VueComponent class
     * @return a fully populated ComponentModel
     */
    public ComponentModel parse(TypeElement classElement) {
        ComponentModel model = new ComponentModel();

        // ── 1. Class identity ──────────────────────────────────────────────
        model.setJavaClassName(classElement.getSimpleName().toString());
        model.setPackageName(
            processingEnv.getElementUtils()
                .getPackageOf(classElement).getQualifiedName().toString()
        );

        // ── 2. @VueComponent ───────────────────────────────────────────────
        VueComponent vc = classElement.getAnnotation(VueComponent.class);
        // Resolve component name: name() takes precedence over value(), else derive from class name
        String resolvedName = (!vc.name().isEmpty()) ? vc.name()
            : (!vc.value().isEmpty()) ? vc.value()
            : toKebabCase(classElement.getSimpleName().toString());
        model.setComponentName(resolvedName);

        // ── 3. @VuePage (optional) ─────────────────────────────────────────
        VuePage vuePage = classElement.getAnnotation(VuePage.class);
        if (vuePage != null) {
            model.setPage(true);
            model.setVuePath(vuePage.path());
            String routeName = (vuePage.name() == null || vuePage.name().isEmpty())
                ? classElement.getSimpleName().toString()
                : vuePage.name();
            model.setRouteName(routeName);
            model.setRequiresAuth(vuePage.requiresAuth());
            model.setLayoutName(vuePage.layout());
            model.setPageTitle(vuePage.title().isEmpty() ? routeName : vuePage.title());
        }

        // ── 4. @VueLayout (optional) ───────────────────────────────────────
        VueLayout vueLayout = classElement.getAnnotation(VueLayout.class);
        if (vueLayout != null) {
            model.setLayout(true);
            model.setLayoutId(vueLayout.value());
        }

        // ── 5. @VueFile (optional) — external template ────────────────────
        VueFile vueFile = classElement.getAnnotation(VueFile.class);
        if (vueFile != null) {
            model.setTemplate(templateParser.parseExternalFile(vueFile.value()));
        }

        // ── 6. Enumerate enclosed elements (fields + methods) ─────────────
        List<FieldModel> reactiveFields  = new ArrayList<>();
        List<FieldModel> propFields      = new ArrayList<>();
        List<MethodModel> vueMethods     = new ArrayList<>();
        List<MethodModel> computedMethods = new ArrayList<>();
        List<MethodModel> watchMethods   = new ArrayList<>();
        List<MethodModel> lifecycleMethods = new ArrayList<>();
        List<String> autowiredTypes      = new ArrayList<>();

        for (Element enclosed : classElement.getEnclosedElements()) {

            // ─── Fields ────────────────────────────────────────────────────
            if (enclosed.getKind() == ElementKind.FIELD) {
                VariableElement field = (VariableElement) enclosed;

                Reactive   reactive  = field.getAnnotation(Reactive.class);
                VueProp    prop      = field.getAnnotation(VueProp.class);
                VueTemplate template = field.getAnnotation(VueTemplate.class);

                // Also detect @Autowired to inform generated controller
                if (field.getAnnotationMirrors().stream()
                        .anyMatch(m -> m.getAnnotationType().toString().contains("Autowired"))) {
                    autowiredTypes.add(field.asType().toString());
                }

                if (reactive != null) {
                    reactiveFields.add(fieldParser.parseReactiveField(field, reactive));
                }

                if (prop != null) {
                    propFields.add(fieldParser.parsePropField(field, prop));
                }

                // @VueTemplate: only if @VueFile not already found
                if (template != null && model.getTemplate() == null) {
                    TemplateModel tm = fieldParser.parseTemplateField(field);
                    if (tm != null) model.setTemplate(tm);
                }
            }

            // ─── Methods ───────────────────────────────────────────────────
            if (enclosed.getKind() == ElementKind.METHOD) {
                ExecutableElement method = (ExecutableElement) enclosed;

                VueMethod    vueMethod = method.getAnnotation(VueMethod.class);
                VueComputed  computed  = method.getAnnotation(VueComputed.class);
                VueWatch     watch     = method.getAnnotation(VueWatch.class);
                VueLifecycle lifecycle = method.getAnnotation(VueLifecycle.class);
                VueEmit      emit      = method.getAnnotation(VueEmit.class);

                if (vueMethod != null) {
                    MethodModel mm = parseMethodBase(method);
                    mm.setVueName(vueMethod.name().isEmpty()
                        ? method.getSimpleName().toString() : vueMethod.name());
                    mm.setShowLoading(vueMethod.loading());
                    mm.setDebounce(vueMethod.debounce());
                    mm.setConfirm(vueMethod.confirm());
                    mm.setConfirmMessage(vueMethod.confirmMessage());
                    if (emit != null) {
                        mm.setEmitsEvent(true);
                        mm.setEmitEventName(emit.value());
                    }
                    vueMethods.add(mm);
                }

                if (computed != null) {
                    MethodModel mm = parseMethodBase(method);
                    mm.setVueName(resolveComputedName(computed, method));
                    mm.setComputed(true);
                    computedMethods.add(mm);
                }

                if (watch != null) {
                    MethodModel mm = parseMethodBase(method);
                    mm.setWatchTarget(watch.value());
                    mm.setDeep(watch.deep());
                    mm.setImmediate(watch.immediate());
                    mm.setWatchDebounce(watch.debounce());
                    watchMethods.add(mm);
                }

                if (lifecycle != null) {
                    MethodModel mm = parseMethodBase(method);
                    mm.setLifecycleHook(lifecycle.hook());
                    lifecycleMethods.add(mm);
                }
            }
        }

        model.setReactiveFields(reactiveFields);
        model.setPropFields(propFields);
        model.setVueMethods(vueMethods);
        model.setComputedMethods(computedMethods);
        model.setWatchMethods(watchMethods);
        model.setLifecycleMethods(lifecycleMethods);
        model.setAutowiredTypes(autowiredTypes);

        return model;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Private helpers
    // ─────────────────────────────────────────────────────────────────────────

    private MethodModel parseMethodBase(ExecutableElement method) {
        MethodModel mm = new MethodModel();
        mm.setJavaName(method.getSimpleName().toString());
        mm.setReturnType(method.getReturnType().toString());

        List<String> paramNames = new ArrayList<>();
        List<String> paramTypes = new ArrayList<>();
        for (VariableElement param : method.getParameters()) {
            paramNames.add(param.getSimpleName().toString());
            paramTypes.add(param.asType().toString());
        }
        mm.setParamNames(paramNames);
        mm.setParamTypes(paramTypes);

        return mm;
    }

    /**
     * Resolves the Vue name for a computed property.
     * If annotation value is empty, strips "get" prefix and lowercases first char.
     * e.g., "getUsersCount" → "usersCount"
     */
    private String resolveComputedName(VueComputed computed, ExecutableElement method) {
        if (!computed.name().isEmpty()) return computed.name();
        String name = method.getSimpleName().toString();
        if (name.startsWith("get") && name.length() > 3) {
            return Character.toLowerCase(name.charAt(3)) + name.substring(4);
        }
        if (name.startsWith("is") && name.length() > 2) {
            return Character.toLowerCase(name.charAt(2)) + name.substring(3);
        }
        return name;
    }

    /**
     * Converts a PascalCase/camelCase Java class name to Vue kebab-case.
     * e.g., "UserManagement" → "user-management"
     *       "DashboardPage" → "dashboard-page"
     */
    private String toKebabCase(String name) {
        return name
            .replaceAll("([A-Z]+)([A-Z][a-z])", "$1-$2")
            .replaceAll("([a-z])([A-Z])", "$1-$2")
            .toLowerCase()
            .replaceAll("^-", "");
    }
}
