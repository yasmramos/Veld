package com.veld.runtime;

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
     * Dependencies are resolved through the provided container.
     *
     * @param container the container to resolve dependencies from
     * @return a new instance of the component
     */
    T create(VeldContainer container);

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
}
