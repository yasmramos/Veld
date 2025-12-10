package io.github.yasmramos.runtime.condition;

import io.github.yasmramos.runtime.ComponentRegistry;

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
}
