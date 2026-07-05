package io.javalive.core.dispatch;

import io.javalive.annotations.VueMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Security guard preventing unauthorized method invocation via WebSocket.
 *
 * <p><strong>This is a critical security component.</strong>
 *
 * <p>Without this guard, any malicious client could craft a WebSocket message to invoke
 * any method in any class on the server — including destructive ones like
 * {@code deleteAllUsers()}, {@code dropDatabase()}, or internal helpers.
 *
 * <p>The guard maintains a cache of allowed methods (those annotated with {@code @VueMethod})
 * per class. The cache is populated on first access (lazy, one-time reflection per method)
 * and then accessed in O(1) thereafter.
 *
 * <h3>How it works:</h3>
 * <pre>
 * Browser sends: { destination: "/app/dashboard.deleteAllUsers", ... }
 *                                                 ↓
 *                         SecurityGuard.verify("Dashboard", "deleteAllUsers")
 *                                                 ↓
 *                   "deleteAllUsers" is NOT @VueMethod → SecurityException
 *                                                 ↓
 *                     Error returned to browser, method NEVER called
 * </pre>
 *
 * @author JavaLive Team
 * @since 1.0.0
 */
@Component
public class SecurityGuard {

    private static final Logger log = LoggerFactory.getLogger(SecurityGuard.class);

    /**
     * Cache: "com.example.Dashboard#increment" → true/false
     * ConcurrentHashMap for thread-safety under high concurrency.
     */
    private final ConcurrentHashMap<String, Boolean> allowedMethodCache
        = new ConcurrentHashMap<>();

    /**
     * Verifies that the specified method is permitted to be called from the browser.
     *
     * @param fullyQualifiedClassName the fully qualified Java class name
     * @param methodName              the method name to verify
     * @throws SecurityException if the method is not annotated with @VueMethod
     *                           or the class cannot be found
     */
    public static void verify(String fullyQualifiedClassName, String methodName) {
        String cacheKey = fullyQualifiedClassName + "#" + methodName;

        // Static cache for use in generated (static-context) controllers
        boolean allowed = STATIC_CACHE.computeIfAbsent(cacheKey, key -> {
            try {
                Class<?> clazz = Class.forName(fullyQualifiedClassName);
                for (Method method : clazz.getDeclaredMethods()) {
                    if (method.getName().equals(methodName)
                        && method.isAnnotationPresent(VueMethod.class)) {
                        return true;
                    }
                }
                return false;
            } catch (ClassNotFoundException e) {
                log.error("JavaLive SecurityGuard: Class not found: {}", fullyQualifiedClassName);
                return false;
            }
        });

        if (!allowed) {
            String msg = String.format(
                "JavaLive Security: BLOCKED invocation of '%s#%s'. " +
                "Only methods annotated with @VueMethod can be called from the browser.",
                fullyQualifiedClassName, methodName);
            log.warn(msg);
            throw new SecurityException(msg);
        }

        log.debug("JavaLive SecurityGuard: ✅ Allowed: {}#{}", fullyQualifiedClassName, methodName);
    }

    /**
     * Verifies that a lifecycle hook dispatch is allowed.
     * Lifecycle hooks don't need @VueMethod — they are verified differently.
     *
     * @param fullyQualifiedClassName the class name
     * @param lifecycleHook           the hook name (e.g., "onMounted")
     */
    public static void verifyLifecycle(String fullyQualifiedClassName, String lifecycleHook) {
        String cacheKey = fullyQualifiedClassName + "#lifecycle#" + lifecycleHook;

        boolean allowed = STATIC_CACHE.computeIfAbsent(cacheKey, key -> {
            try {
                Class<?> clazz = Class.forName(fullyQualifiedClassName);
                for (Method method : clazz.getDeclaredMethods()) {
                    io.javalive.annotations.VueLifecycle annotation =
                        method.getAnnotation(io.javalive.annotations.VueLifecycle.class);
                    if (annotation != null && lifecycleHook.equals(annotation.hook())) {
                        return true;
                    }
                }
                return false;
            } catch (ClassNotFoundException e) {
                return false;
            }
        });

        if (!allowed) {
            throw new SecurityException(
                "JavaLive Security: BLOCKED lifecycle invocation '" + lifecycleHook +
                "' on " + fullyQualifiedClassName + ". No @VueLifecycle annotation found.");
        }
    }

    /**
     * Clears the security cache.
     * Only needed in hot-reload development mode.
     */
    public static void clearCache() {
        STATIC_CACHE.clear();
        log.info("JavaLive SecurityGuard: Cache cleared (hot-reload)");
    }

    /** Shared static cache for use from generated static controller methods. */
    private static final ConcurrentHashMap<String, Boolean> STATIC_CACHE
        = new ConcurrentHashMap<>();

    /** Instance-method wrapper for bean injection scenarios. */
    public void verifyMethod(String className, String methodName) {
        verify(className, methodName);
    }
}
