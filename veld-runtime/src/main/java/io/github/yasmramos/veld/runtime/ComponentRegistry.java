package io.github.yasmramos.veld.runtime;

import io.github.yasmramos.veld.VeldException;
import java.util.List;

/**
 * Interface implemented by the generated registry class.
 * The registry holds all component factories discovered at compile time.
 * 
 * <p>This interface provides both traditional factory-based access (for compatibility)
 * and ultra-fast index-based access for maximum performance.
 * 
 * <p>Performance characteristics:
 * <ul>
 *   <li>Index lookup: O(1) using IdentityHashMap for Class keys
 *   <li>Instance access: Direct array access after index lookup
 *   <li>Creation: Switch-based dispatch, no virtual method overhead
 * </ul>
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
    
    /**
     * Returns the primary factory for the given type.
     * When multiple beans of the same type exist, returns the one marked with @Primary.
     * If no @Primary bean exists and multiple beans match, returns null (caller should handle ambiguity).
     *
     * @param type the component type
     * @param <T> the component type
     * @return the primary factory, null if not found, or null if multiple exist without a primary
     * @throws VeldException if multiple @Primary beans are found for the same type
     */
    default <T> ComponentFactory<T> getPrimaryFactory(Class<T> type) {
        List<ComponentFactory<? extends T>> factories = getFactoriesForType(type);
        
        if (factories.isEmpty()) {
            return null;
        }
        
        // Find primary factory
        ComponentFactory<T> primary = null;
        for (ComponentFactory<? extends T> factory : factories) {
            if (factory.isPrimary()) {
                if (primary != null) {
                    throw new VeldException("Multiple @Primary beans found for type: " + type.getName() + 
                        ". Only one bean can be marked as @Primary.");
                }
                @SuppressWarnings("unchecked")
                ComponentFactory<T> casted = (ComponentFactory<T>) factory;
                primary = casted;
            }
        }
        
        // If a primary was found, return it
        if (primary != null) {
            return primary;
        }
        
        // No primary found - if only one factory, return it
        if (factories.size() == 1) {
            @SuppressWarnings("unchecked")
            ComponentFactory<T> casted = (ComponentFactory<T>) factories.get(0);
            return casted;
        }
        
        // Multiple factories but none is primary - return null to let caller handle ambiguity
        return null;
    }
    
    // ==================== Ultra-Fast Index-Based API ====================
    
    /**
     * Gets the numeric index for a component type.
     * Uses IdentityHashMap internally for O(1) lookup.
     *
     * @param type the component type
     * @return the component index, or -1 if not found
     */
    default int getIndex(Class<?> type) {
        ComponentFactory<?> factory = getFactory(type);
        return factory != null ? factory.getIndex() : -1;
    }
    
    /**
     * Gets the numeric index for a component name.
     *
     * @param name the component name
     * @return the component index, or -1 if not found
     */
    default int getIndex(String name) {
        ComponentFactory<?> factory = getFactory(name);
        return factory != null ? factory.getIndex() : -1;
    }
    
    /**
     * Returns the total number of registered components.
     *
     * @return component count
     */
    default int getComponentCount() {
        return getAllFactories().size();
    }
    
    /**
     * Gets the scope for a component by index.
     * Direct array access, O(1).
     *
     * @param index the component index
     * @return the component scope
     */
    default Scope getScope(int index) {
        List<ComponentFactory<?>> factories = getAllFactories();
        if (index >= 0 && index < factories.size()) {
            return factories.get(index).getScope();
        }
        return Scope.SINGLETON;
    }
    
    /**
     * Checks if a component is lazy by index.
     * Direct array access, O(1).
     *
     * @param index the component index
     * @return true if lazy initialization is enabled
     */
    default boolean isLazy(int index) {
        List<ComponentFactory<?>> factories = getAllFactories();
        if (index >= 0 && index < factories.size()) {
            return factories.get(index).isLazy();
        }
        return false;
    }
    
    /**
     * Creates a component instance by index.
     * Uses switch-based dispatch for ultra-fast creation.
     *
     * @param index the component index
     * @param <T> the component type
     * @return the created instance
     */
    @SuppressWarnings("unchecked")
    default <T> T create(int index) {
        List<ComponentFactory<?>> factories = getAllFactories();
        if (index >= 0 && index < factories.size()) {
            return (T) factories.get(index).create();
        }
        throw new VeldException("Invalid component index: " + index);
    }
    
    /**
     * Gets all component indices for a given type.
     * Used by getAll() for collecting multiple implementations.
     *
     * @param type the type to look up
     * @return array of indices, empty array if none found
     */
    default int[] getIndicesForType(Class<?> type) {
        List<? extends ComponentFactory<?>> factories = getFactoriesForType(type);
        int[] indices = new int[factories.size()];
        for (int i = 0; i < factories.size(); i++) {
            indices[i] = factories.get(i).getIndex();
        }
        return indices;
    }
    
    /**
     * Invokes @PostConstruct method on a component by index.
     *
     * @param index the component index
     * @param instance the component instance
     */
    default void invokePostConstruct(int index, Object instance) {
        List<ComponentFactory<?>> factories = getAllFactories();
        if (index >= 0 && index < factories.size()) {
            @SuppressWarnings("unchecked")
            ComponentFactory<Object> factory = (ComponentFactory<Object>) factories.get(index);
            factory.invokePostConstruct(instance);
        }
    }
    
    /**
     * Invokes @PreDestroy method on a component by index.
     *
     * @param index the component index
     * @param instance the component instance
     */
    default void invokePreDestroy(int index, Object instance) {
        List<ComponentFactory<?>> factories = getAllFactories();
        if (index >= 0 && index < factories.size()) {
            @SuppressWarnings("unchecked")
            ComponentFactory<Object> factory = (ComponentFactory<Object>) factories.get(index);
            factory.invokePreDestroy(instance);
        }
    }
}
