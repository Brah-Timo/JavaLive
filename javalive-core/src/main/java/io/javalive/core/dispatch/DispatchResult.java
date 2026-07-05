package io.javalive.core.dispatch;

import java.util.Map;

/**
 * Encapsulates the result of a {@link MethodDispatcher#dispatch} call.
 *
 * <p>Contains:
 * <ul>
 *   <li>The new state after the method execution</li>
 *   <li>The return value of the Java method (for @VueEmit support)</li>
 *   <li>An error message if the method threw an exception</li>
 * </ul>
 *
 * @author JavaLive Team
 * @since 1.0.0
 */
public class DispatchResult {

    /** The component state after method execution. */
    private final Map<String, Object> newState;

    /** The return value of the Java method (null if void). */
    private final Object returnValue;

    /** Error message if the method threw an exception, null otherwise. */
    private final String error;

    /** Optional error code for structured error handling. */
    private final String errorCode;

    public DispatchResult(Map<String, Object> newState, Object returnValue, String error) {
        this(newState, returnValue, error, null);
    }

    public DispatchResult(Map<String, Object> newState, Object returnValue,
                          String error, String errorCode) {
        this.newState    = newState;
        this.returnValue = returnValue;
        this.error       = error;
        this.errorCode   = errorCode;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Factory methods
    // ─────────────────────────────────────────────────────────────────────────

    /** Creates a successful result. */
    public static DispatchResult success(Map<String, Object> newState, Object returnValue) {
        return new DispatchResult(newState, returnValue, null);
    }

    /** Creates an error result (state unchanged). */
    public static DispatchResult error(Map<String, Object> currentState,
                                       String errorMessage, String errorCode) {
        return new DispatchResult(currentState, null, errorMessage, errorCode);
    }

    /** Creates a simple error result. */
    public static DispatchResult error(Map<String, Object> currentState, String errorMessage) {
        return new DispatchResult(currentState, null, errorMessage, "DISPATCH_ERROR");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Predicates
    // ─────────────────────────────────────────────────────────────────────────

    public boolean hasError()       { return error != null && !error.isEmpty(); }
    public boolean hasReturnValue() { return returnValue != null; }

    // ─────────────────────────────────────────────────────────────────────────
    // Getters
    // ─────────────────────────────────────────────────────────────────────────

    public Map<String, Object> getNewState() { return newState; }
    public Object getReturnValue()           { return returnValue; }
    public String getError()                 { return error; }
    public String getErrorCode()             { return errorCode; }

    @Override
    public String toString() {
        return "DispatchResult{hasError=" + hasError() +
               ", stateKeys=" + (newState != null ? newState.keySet() : "null") + "}";
    }
}
