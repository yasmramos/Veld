package com.veld.runtime;

import java.util.List;

/**
 * Interface implemented by the generated registry class.
 * The registry holds all component factories discovered at compile time.
 */
public interface ComponentRegistry {

    /**
     * Returns all registered component factories.
     *
     * @return list of all factories
     */
    List<ComponentFactory<?>> getAllFactories();

    /**
     * Returns a factory for the given component type.
     *
     * @param type the component type
     * @param <T> the component type
     * @return the factory, or null if not found
     */
    <T> ComponentFactory<T> getFactory(Class<T> type);

    /**
     * Returns a factory for the given component name.
     *
     * @param name the component name
     * @return the factory, or null if not found
     */
    ComponentFactory<?> getFactory(String name);

    /**
     * Returns all factories that produce the given type or its subtypes.
     *
     * @param type the component type
     * @param <T> the component type
     * @return list of matching factories
     */
    <T> List<ComponentFactory<? extends T>> getFactoriesForType(Class<T> type);
}
