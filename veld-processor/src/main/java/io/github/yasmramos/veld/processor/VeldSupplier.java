package io.github.yasmramos.veld;

/**
 * Supplier interface for lazy component creation in VeldLite.
 * 
 * <p>Allows manual dependency injection by calling {@code VeldLite.get()} 
 * inside the supplier lambda.</p>
 * 
 * @param <T> the component type
 */
@FunctionalInterface
public interface VeldSupplier<T> {
    
    /**
     * Creates the component instance.
     * 
     * @return the created component
     */
    T get();
}
