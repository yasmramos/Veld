package com.veld.runtime;

import com.veld.runtime.condition.ConditionContext;
import com.veld.runtime.condition.ConditionEvaluator;
import java.util.Collections;
import java.util.List;

/**
 * Interface implemented by generated factory classes.
 * Each component annotated with @Component will have a corresponding
 * factory class generated at compile time.
 *
 * @param <T> the type of component this factory creates
 */
public interface ComponentFactory<T> {

    /**
     * Creates a new instance of the component.
     * Dependencies are resolved through the generated Veld class.
     *
     * @return a new instance of the component
     */
    T create();

    /**
     * Returns the type of component this factory creates.
     *
     * @return the component class
     */
    Class<T> getComponentType();

    /**
     * Returns the name of the component.
     *
     * @return the component name
     */
    String getComponentName();

    /**
     * Returns the scope of the component.
     *
     * @return the component scope
     */
    Scope getScope();
    
    /**
     * Returns true if this component should be lazily initialized.
     * Lazy components are not instantiated until first accessed.
     *
     * @return true if lazy initialization is enabled
     */
    default boolean isLazy() {
        return false;
    }

    /**
     * Invokes the post-construct lifecycle method if present.
     *
     * @param instance the component instance
     */
    void invokePostConstruct(T instance);

    /**
     * Invokes the pre-destroy lifecycle method if present.
     *
     * @param instance the component instance
     */
    void invokePreDestroy(T instance);
    
    /**
     * Returns true if this component has conditional registration.
     *
     * @return true if conditions must be evaluated
     */
    default boolean hasConditions() {
        return false;
    }
    
    /**
     * Evaluates whether this component should be registered based on its conditions.
     * Only called if {@link #hasConditions()} returns true.
     *
     * @param context the condition evaluation context
     * @return true if all conditions are satisfied
     */
    default boolean evaluateConditions(ConditionContext context) {
        return true;
    }
    
    /**
     * Creates and configures a ConditionEvaluator for this component.
     * Override this method in generated factories to add conditions.
     *
     * @return the condition evaluator, or null if no conditions
     */
    default ConditionEvaluator createConditionEvaluator() {
        return null;
    }
    
    /**
     * Returns the list of interfaces implemented by this component.
     * Used for @ConditionalOnMissingBean type checking.
     *
     * @return list of fully qualified interface names
     */
    default List<String> getImplementedInterfaces() {
        return Collections.emptyList();
    }
    
    /**
     * Returns the numeric index of this component in the registry.
     * Used for ultra-fast array-based lookups.
     *
     * @return the component index (0-based), or -1 if not indexed
     */
    default int getIndex() {
        return -1;
    }
}
