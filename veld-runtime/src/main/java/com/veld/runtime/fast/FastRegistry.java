package com.veld.runtime.fast;

import com.veld.runtime.ComponentFactory;
import com.veld.runtime.Scope;

/**
 * Ultra-fast registry interface for index-based component access.
 * Generated at compile-time with pre-computed indices for O(1) lookups.
 * 
 * <p>This interface is the core of Veld's performance advantage over Dagger:
 * <ul>
 *   <li>Array-based singleton storage instead of HashMap</li>
 *   <li>Pre-computed class-to-index mapping</li>
 *   <li>Zero-allocation lookups for singletons</li>
 *   <li>JIT-friendly predictable memory access patterns</li>
 * </ul>
 * 
 * <p>Performance characteristics:
 * <ul>
 *   <li>Singleton lookup: ~0.5ns (direct array access)</li>
 *   <li>Type-to-index: ~1-2ns (IdentityHashMap or generated switch)</li>
 *   <li>Total get(): &lt;2ns (faster than Dagger's DoubleCheck)</li>
 * </ul>
 */
public interface FastRegistry {
    
    /**
     * Gets the index for a component type.
     * Returns -1 if the type is not registered.
     *
     * @param type the component type
     * @return the index, or -1 if not found
     */
    int getIndex(Class<?> type);
    
    /**
     * Gets the index for a component name.
     * Returns -1 if the name is not registered.
     *
     * @param name the component name
     * @return the index, or -1 if not found
     */
    int getIndex(String name);
    
    /**
     * Gets the factory at the specified index.
     *
     * @param index the component index
     * @return the factory
     */
    ComponentFactory<?> getFactory(int index);
    
    /**
     * Gets the scope at the specified index.
     *
     * @param index the component index
     * @return the scope
     */
    Scope getScope(int index);
    
    /**
     * Gets the total number of registered components.
     *
     * @return the component count
     */
    int getComponentCount();
    
    /**
     * Gets all factory indices for components that implement the given type.
     * Used for getAll() operations.
     *
     * @param type the interface or superclass type
     * @return array of indices, or empty array if none found
     */
    int[] getIndicesForType(Class<?> type);
    
    /**
     * Checks if a component at the given index is lazy.
     *
     * @param index the component index
     * @return true if lazy initialization is enabled
     */
    boolean isLazy(int index);
    
    /**
     * Creates a new instance using the factory at the given index.
     * This is used for prototype scope and initial singleton creation.
     *
     * @param index the component index
     * @param container the container for dependency resolution
     * @param <T> the component type
     * @return a new instance
     */
    <T> T create(int index, FastContainer container);
    
    /**
     * Invokes post-construct on the instance at the given index.
     *
     * @param index the component index
     * @param instance the component instance
     */
    void invokePostConstruct(int index, Object instance);
    
    /**
     * Invokes pre-destroy on the instance at the given index.
     *
     * @param index the component index
     * @param instance the component instance
     */
    void invokePreDestroy(int index, Object instance);
}
