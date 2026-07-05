package io.javalive.core.dispatch;

import io.javalive.annotations.VueLifecycle;
import io.javalive.annotations.VueMethod;
import io.javalive.core.state.StateSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;

/**
 * Dispatches method calls from the browser to actual Java methods via reflection.
 *
 * <p>The dispatch lifecycle for each call:
 * <ol>
 *   <li>Load the Java class by name</li>
 *   <li>Create a new instance (via no-args constructor)</li>
 *   <li>Inject Spring beans via @Autowired fields</li>
 *   <li>Apply current session state to @Reactive fields (no reflection loops needed)</li>
 *   <li>Find and invoke the target method with converted parameters</li>
 *   <li>Extract updated @Reactive field values as the new state</li>
 *   <li>Return the new state + method return value as a DispatchResult</li>
 * </ol>
 *
 * <p><strong>Design note:</strong> In a future version, the Annotation Processor
 * will generate a compiled dispatch switch table, replacing this reflection-based
 * approach for maximum performance. The API contract won't change.
 *
 * @author JavaLive Team
 * @since 1.0.0
 */
@Component
public class MethodDispatcher {

    private static final Logger log = LoggerFactory.getLogger(MethodDispatcher.class);

    @Autowired
    private StateSerializer stateSerializer;

    @Autowired(required = false)
    private org.springframework.context.ApplicationContext applicationContext;

    // ─────────────────────────────────────────────────────────────────────────
    // Main dispatch
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Dispatches a @VueMethod call.
     *
     * @param fullyQualifiedClassName the Java class to instantiate
     * @param methodName              the method to call
     * @param currentState            current session state (applied to @Reactive fields)
     * @param args                    method arguments from the browser
     * @return the dispatch result containing new state and optional return value
     */
    public DispatchResult dispatch(String fullyQualifiedClassName,
                                   String methodName,
                                   Map<String, Object> currentState,
                                   List<Object> args) {
        long startMs = System.currentTimeMillis();
        try {
            // 1. Load and instantiate
            Class<?> clazz = Class.forName(fullyQualifiedClassName);
            Object instance = clazz.getDeclaredConstructor().newInstance();

            // 2. Inject Spring beans (@Autowired fields)
            injectBeans(instance, clazz);

            // 3. Apply current state to @Reactive fields
            applyStateToInstance(instance, clazz, currentState);

            // 4. Find the method
            Method method = findVueMethod(clazz, methodName, args);
            if (method == null) {
                return DispatchResult.error(currentState,
                    "Method '" + methodName + "' not found or not annotated with @VueMethod",
                    "METHOD_NOT_FOUND");
            }
            method.setAccessible(true);

            // 5. Invoke
            Object[] convertedArgs = convertArguments(method, args);
            Object returnValue = method.invoke(instance, convertedArgs);

            // 6. Extract new state
            Map<String, Object> newState = extractStateFromInstance(instance, clazz);

            long elapsed = System.currentTimeMillis() - startMs;
            log.debug("JavaLive: Dispatched {}#{} in {}ms", fullyQualifiedClassName, methodName, elapsed);

            return DispatchResult.success(newState,
                returnValue != null ? stateSerializer.toStateCompatible(returnValue) : null);

        } catch (SecurityException e) {
            throw e; // Re-throw security exceptions
        } catch (java.lang.reflect.InvocationTargetException e) {
            // The actual business exception from the method
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            log.warn("JavaLive: Method {}#{} threw: {}", fullyQualifiedClassName,
                     methodName, cause.getMessage());
            return DispatchResult.error(currentState, cause.getMessage(), "METHOD_EXCEPTION");
        } catch (Exception e) {
            log.error("JavaLive: Dispatch failed for {}#{}: {}",
                      fullyQualifiedClassName, methodName, e.getMessage(), e);
            return DispatchResult.error(currentState, "Internal error: " + e.getMessage());
        }
    }

    /**
     * Dispatches a @VueLifecycle hook call.
     *
     * @param fullyQualifiedClassName the Java class
     * @param lifecycleHook           the hook name (e.g., "onMounted")
     * @param currentState            current session state
     * @return the dispatch result
     */
    public DispatchResult dispatchLifecycle(String fullyQualifiedClassName,
                                            String lifecycleHook,
                                            Map<String, Object> currentState) {
        try {
            Class<?> clazz = Class.forName(fullyQualifiedClassName);
            Object instance = clazz.getDeclaredConstructor().newInstance();

            injectBeans(instance, clazz);
            applyStateToInstance(instance, clazz, currentState);

            // Find method annotated with @VueLifecycle(hook = lifecycleHook)
            Method method = findLifecycleMethod(clazz, lifecycleHook);
            if (method == null) {
                // No lifecycle hook found — that's OK, return current state unchanged
                return DispatchResult.success(new HashMap<>(currentState), null);
            }

            method.setAccessible(true);
            method.invoke(instance);

            Map<String, Object> newState = extractStateFromInstance(instance, clazz);
            return DispatchResult.success(newState, null);

        } catch (Exception e) {
            log.warn("JavaLive: Lifecycle hook '{}' failed for {}: {}",
                     lifecycleHook, fullyQualifiedClassName, e.getMessage());
            return DispatchResult.success(new HashMap<>(currentState), null);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Private helpers
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Injects Spring beans into @Autowired fields using the ApplicationContext.
     * This allows @VueComponent classes to use @Autowired services normally.
     */
    private void injectBeans(Object instance, Class<?> clazz) {
        if (applicationContext == null) return;

        for (Field field : getAllFields(clazz)) {
            if (isAutowired(field)) {
                field.setAccessible(true);
                try {
                    Object bean = applicationContext.getBean(field.getType());
                    field.set(instance, bean);
                } catch (Exception e) {
                    log.debug("JavaLive: Could not inject bean for field '{}': {}",
                              field.getName(), e.getMessage());
                }
            }
        }
    }

    /**
     * Applies the current session state to the @Reactive fields of the instance.
     */
    private void applyStateToInstance(Object instance, Class<?> clazz,
                                       Map<String, Object> state) {
        if (state == null || state.isEmpty()) return;

        for (Field field : getAllFields(clazz)) {
            if (field.isAnnotationPresent(io.javalive.annotations.Reactive.class)) {
                Object value = state.get(field.getName());
                if (value != null) {
                    field.setAccessible(true);
                    try {
                        field.set(instance, convertValue(value, field.getType()));
                    } catch (Exception e) {
                        log.debug("JavaLive: Could not set field '{}': {}", field.getName(), e.getMessage());
                    }
                }
            }
        }
    }

    /**
     * Extracts all @Reactive field values from the instance into a state map.
     */
    private Map<String, Object> extractStateFromInstance(Object instance, Class<?> clazz) {
        Map<String, Object> state = new LinkedHashMap<>();

        for (Field field : getAllFields(clazz)) {
            if (field.isAnnotationPresent(io.javalive.annotations.Reactive.class)) {
                field.setAccessible(true);
                try {
                    Object value = field.get(instance);
                    state.put(field.getName(), stateSerializer.toStateCompatible(value));
                } catch (Exception e) {
                    log.debug("JavaLive: Could not read field '{}': {}", field.getName(), e.getMessage());
                }
            }
        }

        return state;
    }

    /**
     * Finds a @VueMethod by name and argument count.
     * Returns null if not found.
     */
    private Method findVueMethod(Class<?> clazz, String methodName, List<Object> args) {
        int argCount = args != null ? args.size() : 0;

        for (Method method : clazz.getDeclaredMethods()) {
            if (method.getName().equals(methodName)
                && method.isAnnotationPresent(VueMethod.class)) {
                // Match by name first; if multiple overloads, match by arg count
                if (method.getParameterCount() == argCount) {
                    return method;
                }
            }
        }

        // Fallback: find by name only (if no exact arg-count match)
        for (Method method : clazz.getDeclaredMethods()) {
            if (method.getName().equals(methodName)
                && method.isAnnotationPresent(VueMethod.class)) {
                return method;
            }
        }

        return null;
    }

    /**
     * Finds a @VueLifecycle method matching the given hook name.
     */
    private Method findLifecycleMethod(Class<?> clazz, String hook) {
        for (Method method : clazz.getDeclaredMethods()) {
            VueLifecycle lc = method.getAnnotation(VueLifecycle.class);
            if (lc != null && hook.equals(lc.hook())) {
                return method;
            }
        }
        return null;
    }

    /**
     * Converts method arguments from JSON-deserialized types to the target Java types.
     */
    private Object[] convertArguments(Method method, List<Object> args) {
        Class<?>[] paramTypes = method.getParameterTypes();
        Object[] converted = new Object[paramTypes.length];

        for (int i = 0; i < paramTypes.length; i++) {
            Object arg = (args != null && i < args.size()) ? args.get(i) : null;
            converted[i] = convertValue(arg, paramTypes[i]);
        }

        return converted;
    }

    /**
     * Converts a value (from JSON deserialization) to the target Java type.
     */
    @SuppressWarnings("unchecked")
    private Object convertValue(Object value, Class<?> targetType) {
        if (value == null) return null;
        if (targetType.isInstance(value)) return value;

        try {
            if (targetType == int.class || targetType == Integer.class)
                return ((Number) value).intValue();
            if (targetType == long.class || targetType == Long.class)
                return ((Number) value).longValue();
            if (targetType == double.class || targetType == Double.class)
                return ((Number) value).doubleValue();
            if (targetType == float.class || targetType == Float.class)
                return ((Number) value).floatValue();
            if (targetType == boolean.class || targetType == Boolean.class)
                return Boolean.parseBoolean(value.toString());
            if (targetType == String.class)
                return value.toString();
            // For complex types, use Jackson round-trip
            String json = stateSerializer.serialize(
                value instanceof Map ? (Map<String, Object>) value
                                     : Map.of("v", value));
            return stateSerializer.getMapper().readValue(json, targetType);
        } catch (Exception e) {
            return value;
        }
    }

    private boolean isAutowired(Field field) {
        return field.isAnnotationPresent(org.springframework.beans.factory.annotation.Autowired.class);
    }

    /** Returns all declared fields, including those from superclasses. */
    private List<Field> getAllFields(Class<?> clazz) {
        List<Field> fields = new ArrayList<>();
        Class<?> current = clazz;
        while (current != null && current != Object.class) {
            fields.addAll(Arrays.asList(current.getDeclaredFields()));
            current = current.getSuperclass();
        }
        return fields;
    }
}
