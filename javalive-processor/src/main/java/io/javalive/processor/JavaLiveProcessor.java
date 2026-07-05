package io.javalive.processor;

import com.google.auto.service.AutoService;
import io.javalive.annotations.VueComponent;
import io.javalive.annotations.VuePage;
import io.javalive.processor.generator.*;
import io.javalive.processor.model.ComponentModel;
import io.javalive.processor.parser.ClassParser;
import io.javalive.processor.validator.AnnotationValidator;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.tools.Diagnostic;
import java.util.Set;

/**
 * The main JavaLive Annotation Processor.
 *
 * <p>This is the entry point for the entire compile-time pipeline. It is
 * automatically registered via Google AutoService, so no manual
 * {@code META-INF/services} configuration is required.
 *
 * <h3>Processing pipeline for each @VueComponent class:</h3>
 * <ol>
 *   <li>{@link AnnotationValidator} — validates correctness</li>
 *   <li>{@link ClassParser} — builds internal {@link ComponentModel}</li>
 *   <li>{@link SpringControllerGenerator} — generates Java WebSocket Controller</li>
 *   <li>{@link VueComponentGenerator} — generates Vue 3 JS component</li>
 *   <li>{@link StateSchemaGenerator} — generates JSON state schema</li>
 *   <li>{@link RouterGenerator} — accumulates pages, generates router.js at end</li>
 * </ol>
 *
 * <h3>Supported annotations:</h3>
 * <ul>
 *   <li>{@code @VueComponent} — any class</li>
 *   <li>{@code @VuePage} — implies @VueComponent, adds routing</li>
 * </ul>
 *
 * @author JavaLive Team
 * @since 1.0.0
 */
@SupportedAnnotationTypes({
    "io.javalive.annotations.VueComponent",
    "io.javalive.annotations.VuePage"
})
@SupportedSourceVersion(SourceVersion.RELEASE_21)
@AutoService(Processor.class)
public class JavaLiveProcessor extends AbstractProcessor {

    // Pipeline components
    private ClassParser classParser;
    private AnnotationValidator validator;
    private SpringControllerGenerator controllerGenerator;
    private VueComponentGenerator vueGenerator;
    private StateSchemaGenerator schemaGenerator;
    private RouterGenerator routerGenerator;

    // Counters for summary
    private int processedCount = 0;
    private int errorCount = 0;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        this.classParser          = new ClassParser(processingEnv);
        this.validator            = new AnnotationValidator(processingEnv.getMessager());
        this.controllerGenerator  = new SpringControllerGenerator(processingEnv);
        this.vueGenerator         = new VueComponentGenerator(processingEnv);
        this.schemaGenerator      = new StateSchemaGenerator(processingEnv);
        this.routerGenerator      = new RouterGenerator(processingEnv);

        log("JavaLive Annotation Processor initialized (v1.0.0)");
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {

        // On the last round, finalize router and print summary
        if (roundEnv.processingOver()) {
            finalizeProcessing();
            return false;
        }

        // Process all @VueComponent elements
        for (Element element : roundEnv.getElementsAnnotatedWith(VueComponent.class)) {
            if (element.getKind() != ElementKind.CLASS) {
                error("@VueComponent must be applied to a class, found: " + element.getKind(), element);
                errorCount++;
                continue;
            }

            processComponent((TypeElement) element);
        }

        // Process any bare @VuePage (without @VueComponent — defensive)
        for (Element element : roundEnv.getElementsAnnotatedWith(VuePage.class)) {
            if (element.getKind() != ElementKind.CLASS) continue;
            TypeElement classElement = (TypeElement) element;
            // Only process if not already processed via @VueComponent
            if (classElement.getAnnotation(VueComponent.class) == null) {
                warn("@VuePage found without @VueComponent on " + classElement.getSimpleName() +
                     ". Add @VueComponent to enable full JavaLive processing.", classElement);
            }
        }

        return true;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Private methods
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Processes a single @VueComponent-annotated class through the full pipeline.
     */
    private void processComponent(TypeElement classElement) {
        String className = classElement.getSimpleName().toString();

        try {
            // Step 1: Validate
            if (!validator.validate(classElement)) {
                errorCount++;
                log("⛔ Skipped (validation failed): " + className);
                return;
            }

            // Step 2: Parse into ComponentModel
            ComponentModel model = classParser.parse(classElement);
            log("📋 Parsed: " + className + " → " + model.getComponentName());

            // Step 3: Generate Spring Controller
            controllerGenerator.generate(model);
            log("☕ Generated Spring Controller: " + model.getGeneratedControllerSimpleName());

            // Step 4: Generate Vue Component
            vueGenerator.generate(model);
            log("💚 Generated Vue Component: " + model.getComponentName() + ".js");

            // Step 5: Generate State Schema
            schemaGenerator.generate(model);
            log("📐 Generated State Schema: " + model.getComponentName() + ".schema.json");

            // Step 6: Register with router if it's a page
            if (model.isPage()) {
                routerGenerator.registerPage(model);
                log("🛣️  Registered route: " + model.getVuePath() + " → " + model.getRouteName());
            }

            processedCount++;
            log("✅ Done: " + className);

        } catch (Exception e) {
            errorCount++;
            processingEnv.getMessager().printMessage(
                Diagnostic.Kind.ERROR,
                "🚫 JavaLive: Unexpected error processing " + className +
                ": " + e.getMessage() + "\n" +
                "Please report this at https://github.com/javalive/javalive/issues",
                classElement
            );
        }
    }

    /**
     * Called after all rounds are complete. Generates the router and prints summary.
     */
    private void finalizeProcessing() {
        if (processedCount == 0 && errorCount == 0) return;

        // Generate Vue Router
        routerGenerator.generateRouter();

        // Print processing summary
        processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE,
            "\n╔══════════════════════════════════════════════════╗\n" +
            "║           JavaLive Processing Complete          ║\n" +
            "╠══════════════════════════════════════════════════╣\n" +
            "║  ✅ Components processed : " + pad(processedCount) + "                 ║\n" +
            "║  ❌ Errors               : " + pad(errorCount) + "                 ║\n" +
            "╚══════════════════════════════════════════════════╝"
        );
    }

    private String pad(int n) {
        return String.format("%-3d", n);
    }

    private void log(String message) {
        processingEnv.getMessager().printMessage(
            Diagnostic.Kind.NOTE,
            "[JavaLive] " + message
        );
    }

    private void error(String message, Element element) {
        processingEnv.getMessager().printMessage(
            Diagnostic.Kind.ERROR,
            "🚫 JavaLive: " + message, element
        );
    }

    private void warn(String message, Element element) {
        processingEnv.getMessager().printMessage(
            Diagnostic.Kind.WARNING,
            "⚠️ JavaLive: " + message, element
        );
    }
}
