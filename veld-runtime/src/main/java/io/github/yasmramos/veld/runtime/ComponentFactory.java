package io.github.yasmramos.veld.runtime;

import io.github.yasmramos.veld.runtime.condition.ConditionContext;
import io.github.yasmramos.veld.runtime.condition.ConditionEvaluator;
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
     * Returns true if this component is marked as primary.
     * Primary beans are selected when multiple beans of the same type exist.
     *
     * @return true if this bean is primary
     */
    default boolean isPrimary() {
        return false;
    }
    
    /**
     * Returns the order value for this component.
     * Lower values have higher priority.
     * Used when injecting collections of beans to determine resolution order.
     *
     * @return the order value, defaults to 0
     */
    default int getOrder() {
        return 0;
    }

    /**
     * Returns the qualifier name for this component.
     * Used for qualified bean injection (@Named, @Qualifier).
     *
     * @return the qualifier name, or null if no qualifier
     */
    default String getQualifier() {
        return null;
    }

    /**
     * Returns the factory class that contains this bean method.
     * Used by @Factory and @Bean processing.
     *
     * @return the factory class
     */
    default Class<?> getFactoryClass() {
        return null;
    }

    /**
     * Returns the name of the @Bean method.
     * Used by @Factory and @Bean processing.
     *
     * @return the bean method name
     */
    default String getBeanMethodName() {
        return null;
    }

    /**
     * Returns the parameter types of the factory method.
     * Used for dependency resolution in @Bean methods.
     *
     * @return list of parameter types
     */
    default List<Class<?>> getFactoryMethodParameters() {
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
    
    /**
     * Returns the list of beans that this component must outlive during shutdown.
     * Used for explicit destruction order control via @DependsOn.
     *
     * @return list of bean names this component outlives
     */
    default List<String> getDestructionDependencies() {
        return Collections.emptyList();
    }
    
    /**
     * Returns the destruction order value for this component.
     * Lower values are destroyed first, higher values are destroyed last.
     * Used in combination with destruction dependencies for fine-grained shutdown control.
     *
     * @return the destruction order value (default 0)
     */
    default int getDestroyOrder() {
        return 0;
    }
    
    /**
     * Returns the list of dependency types for this component.
     * Used for building the dependency graph visualization.
     *
     * @return list of fully qualified dependency class names
     */
    default List<String> getDependencyTypes() {
        return Collections.emptyList();
    }
}
