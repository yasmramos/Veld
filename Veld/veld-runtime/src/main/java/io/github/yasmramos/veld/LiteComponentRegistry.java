package io.github.yasmramos.veld;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Internal registry implementation for VeldLite.
 * Thread-safe component storage with lazy initialization support.
 */
final class LiteComponentRegistry {
    
    // Stores either singleton instances or VeldSupplier for lazy creation
    private final ConcurrentHashMap<Class<?>, Object> registry = new ConcurrentHashMap<>();
    
    /**
     * Registers a singleton instance.
     */
    <T> void register(Class<T> type, T instance) {
        if (instance == null) {
            throw new VeldException("Cannot register null instance for: " + type.getName());
        }
        registry.put(type, instance);
    }
    
    /**
     * Registers a lazy factory.
     */
    <T> void register(Class<T> type, VeldSupplier<T> factory) {
        if (factory == null) {
            throw new VeldException("Cannot register null factory for: " + type.getName());
        }
        registry.put(type, factory);
    }
    
    /**
     * Retrieves a component, creating it if necessary.
     */
    @SuppressWarnings("unchecked")
    <T> T get(Class<T> type) {
        Object result = registry.get(type);
        
        if (result == null) {
            throw new VeldException.ComponentNotFoundException(
                "No component registered for: " + type.getName() + 
                ". Use VeldLite.register() to register components.");
        }
        
        // If it's a supplier, create the instance (lazy singleton pattern)
        if (result instanceof VeldSupplier) {
            VeldSupplier<T> supplier = (VeldSupplier<T>) result;
            T instance = supplier.get();
            
            // Replace supplier with instance (atomic operation)
            // This ensures the supplier is only called once
            Object existing = registry.replace(type, supplier, instance);
            
            // If another thread already replaced it, use that value
            if (existing instanceof VeldSupplier) {
                return (T) registry.get(type);
            }
            
            return instance;
        }
        
        return (T) result;
    }
    
    /**
     * Retrieves a component if registered, returns null otherwise.
     */
    @SuppressWarnings("unchecked")
    <T> T getOrNull(Class<T> type) {
        Object result = registry.get(type);
        
        if (result == null) {
            return null;
        }
        
        if (result instanceof VeldSupplier) {
            VeldSupplier<T> supplier = (VeldSupplier<T>) result;
            T instance = supplier.get();
            registry.replace(type, supplier, instance);
            return instance;
        }
        
        return (T) result;
    }
    
    /**
     * Checks if a component is registered.
     */
    boolean contains(Class<?> type) {
        return registry.containsKey(type);
    }
    
    /**
     * Clears all registrations.
     */
    void clear() {
        registry.clear();
    }
    
    /**
     * Returns the number of registered components.
     */
    int componentCount() {
        return registry.size();
    }
}
