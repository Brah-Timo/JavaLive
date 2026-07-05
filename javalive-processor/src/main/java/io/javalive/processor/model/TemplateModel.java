package io.javalive.processor.model;

/**
 * Internal model representing the Vue template for a @VueComponent.
 *
 * <p>A template can come from two sources:
 * <ol>
 *   <li><b>Inline</b>: a {@code @VueTemplate} String field in the Java class</li>
 *   <li><b>External</b>: a {@code @VueFile("path.vue")} annotation on the class</li>
 * </ol>
 *
 * @author JavaLive Team
 * @since 1.0.0
 */
public class TemplateModel {

    /** The raw HTML/Vue template content. */
    private String html;

    /** True if this template was defined inline as a Java String field. */
    private boolean inline;

    /**
     * Path to the external .vue file (when inline = false).
     * Relative to the Java source file location or src/main/resources/.
     */
    private String filePath;

    /**
     * True if the template contains a {@code <style>} block.
     * Used by the generator to decide whether to extract and include styles.
     */
    private boolean hasStyles;

    /**
     * Raw CSS extracted from the template's {@code <style>} block, if any.
     */
    private String styles;

    // ─────────────────────────────────────────────────────────────────────────
    // Getters and Setters
    // ─────────────────────────────────────────────────────────────────────────

    public String getHtml() { return html; }
    public void setHtml(String html) { this.html = html; }

    public boolean isInline() { return inline; }
    public void setInline(boolean inline) { this.inline = inline; }

    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }

    public boolean isHasStyles() { return hasStyles; }
    public void setHasStyles(boolean hasStyles) { this.hasStyles = hasStyles; }

    public String getStyles() { return styles; }
    public void setStyles(String styles) { this.styles = styles; }

    // ─────────────────────────────────────────────────────────────────────────
    // Derived helpers
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Returns true if this template has actual content to render.
     */
    public boolean hasContent() {
        return html != null && !html.trim().isEmpty();
    }

    /**
     * Returns the template HTML escaped for embedding as a JS template literal.
     * Escapes backticks and ${} sequences that would break the JS template string.
     */
    public String getHtmlEscapedForJs() {
        if (html == null) return "";
        return html
            .replace("\\", "\\\\")
            .replace("`", "\\`")
            .replace("${", "\\${");
    }

    @Override
    public String toString() {
        return "TemplateModel{" +
               "inline=" + inline +
               ", filePath='" + filePath + '\'' +
               ", hasContent=" + hasContent() +
               '}';
    }
}
