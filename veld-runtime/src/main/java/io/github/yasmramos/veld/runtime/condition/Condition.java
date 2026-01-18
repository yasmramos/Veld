package io.github.yasmramos.veld.runtime.condition;

import io.github.yasmramos.veld.runtime.ComponentRegistry;

/**
 * Interface for evaluating whether a component should be registered.
 * Implementations check specific conditions at runtime.
 * 
 * @since 1.0.0
 */
public interface Condition {
    
    /**
     * Evaluates whether this condition is satisfied.
     * 
     * @param context the condition evaluation context
     * @return true if the condition is satisfied and the component should be registered
     */
    boolean matches(ConditionContext context);
    
    /**
     * Returns a human-readable description of this condition.
     * Used for debugging and error messages.
     *
     * @return description of the condition
     */
    String getDescription();

    /**
     * Returns a detailed, context-aware explanation of why this condition failed.
     * This method should only be called when matches(context) returns false.
     *
     * @param context the condition evaluation context
     * @return a detailed failure explanation, or empty string if condition passed
     */
    default String getFailureReason(ConditionContext context) {
        return getDescription() + " (condition not satisfied)";
    }
}
