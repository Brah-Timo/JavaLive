package io.javalive.core.state;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

/**
 * Loads all generated state schema JSON files at application startup.
 *
 * <p>Scans the classpath for all {@code *.schema.json} files in
 * {@code /static/javalive/schemas/} and registers them with the
 * {@link StateManager}.
 *
 * <p>This eliminates the need for any reflection-based schema discovery
 * at runtime — all type information comes from the compile-time generated files.
 *
 * @author JavaLive Team
 * @since 1.0.0
 */
@Component
public class SchemaLoader {

    private static final Logger log = LoggerFactory.getLogger(SchemaLoader.class);
    private static final String SCHEMA_PATTERN = "classpath*:/static/javalive/schemas/*.schema.json";

    @Autowired
    private StateManager stateManager;

    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * Called after the entire Spring context is ready.
     * Loads all generated schemas and registers them.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void loadAllSchemas() {
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();

        try {
            Resource[] resources = resolver.getResources(SCHEMA_PATTERN);
            int loaded = 0;

            for (Resource resource : resources) {
                try {
                    StateSchema schema = parseSchema(resource);
                    if (schema != null && schema.getComponentName() != null) {
                        stateManager.registerSchema(schema.getComponentName(), schema);
                        loaded++;
                        log.debug("JavaLive: Loaded schema for '{}'", schema.getComponentName());
                    }
                } catch (Exception e) {
                    log.warn("JavaLive: Failed to load schema from {}: {}",
                             resource.getFilename(), e.getMessage());
                }
            }

            log.info("JavaLive: Loaded {} component state schema(s)", loaded);

        } catch (IOException e) {
            log.warn("JavaLive: Could not scan for state schemas: {}", e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Private helpers
    // ─────────────────────────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private StateSchema parseSchema(Resource resource) throws IOException {
        try (InputStream is = resource.getInputStream()) {
            Map<String, Object> json = mapper.readValue(is, Map.class);

            StateSchema schema = new StateSchema();
            schema.setComponentName((String) json.get("name"));
            schema.setJavaClass((String) json.get("javaClass"));

            // Parse fields
            List<Map<String, Object>> fieldList = (List<Map<String, Object>>) json.get("fields");
            if (fieldList != null) {
                for (Map<String, Object> fieldJson : fieldList) {
                    StateSchema.FieldSchema field = new StateSchema.FieldSchema();
                    field.setName((String) fieldJson.get("name"));
                    field.setJavaType((String) fieldJson.get("javaType"));
                    field.setVueType((String) fieldJson.get("vueType"));
                    field.setDefaultValue(fieldJson.get("defaultValue"));
                    field.setScope((String) fieldJson.getOrDefault("scope", "session"));
                    field.setPersist(Boolean.TRUE.equals(fieldJson.get("persist")));
                    field.setLabel((String) fieldJson.getOrDefault("label", field.getName()));
                    schema.getFields().add(field);
                }
            }

            return schema;
        }
    }
}
