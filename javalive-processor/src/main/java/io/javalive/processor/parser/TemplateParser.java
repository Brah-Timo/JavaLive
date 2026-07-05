package io.javalive.processor.parser;

import io.javalive.processor.model.TemplateModel;

import javax.annotation.processing.ProcessingEnvironment;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Handles loading and parsing of external Vue template files referenced by @VueFile.
 *
 * <p>When a component uses {@code @VueFile("MyComponent.vue")}, this parser
 * locates the file in the project resources, reads its content, and extracts
 * the {@code <template>} and optional {@code <style>} sections.
 *
 * @author JavaLive Team
 * @since 1.0.0
 */
public class TemplateParser {

    private final ProcessingEnvironment processingEnv;

    public TemplateParser(ProcessingEnvironment processingEnv) {
        this.processingEnv = processingEnv;
    }

    /**
     * Loads and parses an external .vue file.
     *
     * @param filePath the path declared in @VueFile (relative to resources/)
     * @return populated TemplateModel, or null if the file cannot be found
     */
    public TemplateModel parseExternalFile(String filePath) {
        TemplateModel model = new TemplateModel();
        model.setInline(false);
        model.setFilePath(filePath);

        String content = readResourceFile(filePath);
        if (content == null) {
            // Try alternative locations
            content = readResourceFile("templates/components/" + filePath);
        }
        if (content == null) {
            content = readResourceFile("templates/" + filePath);
        }

        if (content == null) {
            // Return a placeholder so generation continues without crashing
            model.setHtml("<!-- @VueFile: " + filePath + " not found at compile time. " +
                          "File will be loaded at runtime. -->");
            return model;
        }

        // Extract <template> block
        String templateHtml = extractBlock(content, "template");
        model.setHtml(templateHtml != null ? templateHtml : content);

        // Extract <style> block if present
        String styleContent = extractBlock(content, "style");
        if (styleContent != null && !styleContent.trim().isEmpty()) {
            model.setHasStyles(true);
            model.setStyles(styleContent);
        }

        return model;
    }

    /**
     * Attempts to read a file from the annotation processor's file system
     * (src/main/resources or the class output directory).
     *
     * @param path relative file path
     * @return file content as String, or null if not found
     */
    private String readResourceFile(String path) {
        try {
            FileObject resource = processingEnv.getFiler()
                .getResource(StandardLocation.SOURCE_PATH, "", path);
            try (InputStream is = resource.openInputStream()) {
                return new String(is.readAllBytes(), StandardCharsets.UTF_8);
            }
        } catch (IOException e1) {
            // Try CLASS_OUTPUT
            try {
                FileObject resource = processingEnv.getFiler()
                    .getResource(StandardLocation.CLASS_OUTPUT, "", path);
                try (InputStream is = resource.openInputStream()) {
                    return new String(is.readAllBytes(), StandardCharsets.UTF_8);
                }
            } catch (IOException e2) {
                return null;
            }
        }
    }

    /**
     * Extracts the content of a named block from a .vue file.
     * e.g., extractBlock(content, "template") returns the HTML inside <template>...</template>
     *
     * @param content   the full file content
     * @param blockName "template", "style", or "script"
     * @return the inner content of the block, or null if not found
     */
    private String extractBlock(String content, String blockName) {
        String openTag  = "<" + blockName;
        String closeTag = "</" + blockName + ">";

        int start = content.indexOf(openTag);
        int end   = content.indexOf(closeTag);

        if (start < 0 || end < 0) return null;

        int contentStart = content.indexOf('>', start) + 1;
        return content.substring(contentStart, end).trim();
    }
}
