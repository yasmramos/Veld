package io.github.yasmramos.veld.runtime.scope;

import io.github.yasmramos.veld.runtime.ComponentFactory;
import io.github.yasmramos.veld.runtime.lifecycle.DisposableBean;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Scope implementation for singleton beans.
 * 
 * <p>A singleton scope maintains a single shared instance of each bean
 * for the lifetime of the container. This is the default scope for
 * most beans in a Veld application.</p>
 * 
 * <p>Key characteristics:</p>
 * <ul>
 *   <li>Only one instance exists per bean name</li>
 *   <li>Instance is created on first access (lazy by default)</li>
 *   <li>Thread-safe concurrent access</li>
 *   <li>Lifecycle callbacks (@PreDestroy) are invoked on container shutdown</li>
 * </ul>
 * 
 * <h2>Usage:</h2>
 * <pre>{@code
 * // Using @Singleton annotation (implicit, this is the default)
 * @Singleton
 * public class MyService {
 *     public MyService() {}
 * }
 * 
 * // Or explicitly via @Component without scope annotation
 * // (defaults to singleton)
 * public class MyService {
 *     public MyService() {}
 * }
 * }</pre>
 * 
 * @see Scope
 * @see PrototypeScope
 * @see io.github.yasmramos.veld.annotation.Singleton
 */
public final class SingletonScope implements Scope {
    
    public static final String SCOPE_ID = "singleton";
    
    /**
     * Map of bean name to singleton instance.
     * Uses ConcurrentHashMap for thread-safe access.
     */
    private final Map<String, Object> instances = new ConcurrentHashMap<>();
    
    /**
     * Map of bean name to its creation factory.
     * Stored to support re-creation if needed and for debugging.
     */
    private final Map<String, Object> factories = new ConcurrentHashMap<>();
    
    /**
     * Creates a new SingletonScope instance.
     */
    public SingletonScope() {
        // Default constructor for SPI and manual instantiation
    }
    
    @Override
    public String getId() {
        return SCOPE_ID;
    }
    
    @Override
    public String getDisplayName() {
        return "Singleton";
    }
    
    @Override
    public <T> T get(String name, ComponentFactory<T> factory) {
        // Check if instance already exists
        @SuppressWarnings("unchecked")
        T instance = (T) instances.get(name);
        
        if (instance != null) {
            return instance;
        }
        
        // Create new instance with double-checked locking pattern
        synchronized (instances) {
            // Double-check after acquiring lock
            instance = (T) instances.get(name);
            if (instance != null) {
                return instance;
            }
            
            // Create the instance
            instance = factory.create();
            
            // Store instance and factory
            instances.put(name, instance);
            factories.put(name, factory);
            
            return instance;
        }
    }
    
    @Override
    public Object remove(String name) {
        // Remove instance
        Object instance = instances.remove(name);
        
        // Also remove factory reference
        factories.remove(name);
        
        return instance;
    }
    
    @Override
    public void destroy() {
        // Destroy all singleton instances in reverse creation order
        // This is a simplified version; a full implementation would track creation order
        
        for (Map.Entry<String, Object> entry : instances.entrySet()) {
            String name = entry.getKey();
            Object instance = entry.getValue();
            
            // Invoke PreDestroy callback if the bean implements DisposableBean
            if (instance instanceof DisposableBean) {
                try {
                    ((DisposableBean) instance).destroy();
                } catch (Exception e) {
                    // Log but don't propagate destruction errors
                    System.err.println("Error destroying singleton bean '" + name + "': " + e.getMessage());
                }
            }
        }
        
        // Clear all maps
        instances.clear();
        factories.clear();
    }
    
    @Override
    public String describe() {
        return "SingletonScope[instances=" + instances.size() + "]";
    }
    
    /**
     * Returns the number of singleton instances currently held.
     * Useful for debugging and monitoring.
     * 
     * @return the instance count
     */
    public int getInstanceCount() {
        return instances.size();
    }
    
    /**
     * Checks if a singleton instance exists for the given name.
     * 
     * @param name the bean name
     * @return true if an instance exists
     */
    public boolean contains(String name) {
        return instances.containsKey(name);
    }
    
    /**
     * Returns all singleton instance names.
     * Useful for debugging and testing.
     * 
     * @return unmodifiable set of bean names
     */
    public java.util.Set<String> getInstanceNames() {
        return java.util.Collections.unmodifiableSet(instances.keySet());
    }
}
