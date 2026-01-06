package io.github.yasmramos.veld.runtime.scope;

import io.github.yasmramos.veld.runtime.ComponentFactory;

/**
 * Contract for a scope that controls the lifecycle of bean instances.
 * 
 * <p>Scopes define how and when bean instances are created, stored, and destroyed.
 * The framework provides built-in scopes ({@link SingletonScope}, {@link PrototypeScope})
 * and allows custom scopes to be registered.</p>
 * 
 * <h2>Usage Example:</h2>
 * <pre>{@code
 * // Built-in usage (implicit singleton)
 * public class MyService {
 *     private final Dependency dep;
 *     
 *     public MyService(Dependency dep) {
 *         this.dep = dep; // Same dep instance always used
 *     }
 * }
 * 
 * // Custom scope registration
 * VeldScope.registerScope("request", new RequestScope());
 * 
 * // Using custom scope annotation
 * @MyCustomScope
 * public class MyScopedBean { ... }
 * }</pre>
 * 
 * <h2>Implementing a Custom Scope:</h2>
 * <pre>{@code
 * public class ThreadScope implements Scope {
 *     private final ThreadLocal<Map<String, Object>> instances = ThreadLocal.withInitial(HashMap::new);
 *     
 *     @Override
 *     public <T> T get(String name, ComponentFactory<T> factory) {
 *         Map<String, Object> scope = instances.get();
 *         return (T) scope.computeIfAbsent(name, k -> factory.create());
 *     }
 *     
 *     @Override
 *     public Object remove(String name) {
 *         return instances.get().remove(name);
 *     }
 *     
 *     @Override
 *     public String getId() {
 *         return "thread";
 *     }
 * }
 * }</pre>
 * 
 * @see SingletonScope
 * @see PrototypeScope
 * @see io.github.yasmramos.veld.runtime.scope.RequestScope
 */
public interface Scope {
    
    /**
     * Returns the unique identifier for this scope.
     * Used for scope registration, lookup, and metadata.
     * 
     * @return the scope identifier (e.g., "singleton", "prototype", "request")
     */
    String getId();
    
    /**
     * Returns the display name for this scope.
     * Used in debugging and logging output.
     * 
     * @return a human-readable name for this scope
     */
    default String getDisplayName() {
        return getId();
    }
    
    /**
     * Returns a bean instance from this scope, creating it if necessary.
     * 
     * <p>This method is called by the container when resolving a bean
     * that belongs to this scope. The implementation should:</p>
     * <ul>
     *   <li>Check if an instance already exists in the scope</li>
     *   <li>If not, use the factory to create a new instance</li>
     *   <li>Store the instance if the scope maintains instance state</li>
     *   <li>Return the instance</li>
     * </ul>
     * 
     * @param <T> the bean type
     * @param name the bean name (unique within this scope context)
     * @param factory the factory to create the bean if needed
     * @return the bean instance
     */
    <T> T get(String name, ComponentFactory<T> factory);
    
    /**
     * Removes a bean instance from this scope, if it exists.
     * 
     * <p>This method is typically called during container shutdown
     * or when explicitly destroying a bean. Implementations should
     * perform cleanup and release any resources held.</p>
     * 
     * @param name the bean name to remove
     * @return the removed instance, or null if no instance existed
     */
    Object remove(String name);
    
    /**
     * Destroys all beans in this scope.
     * 
     * <p>This method is called during container shutdown to ensure
     * all beans in this scope have their lifecycle callbacks invoked
     * and resources are released.</p>
     * 
     * <p>Default implementation does nothing. Override if the scope
     * maintains instance state that needs cleanup.</p>
     */
    default void destroy() {
        // Default: no-op
    }
    
    /**
     * Returns whether this scope is currently active.
     * 
     * <p>Some scopes like "request" are only active during certain
     * contexts. This method allows the scope to indicate whether
     * bean resolution is valid at the current time.</p>
     * 
     * <p>Default implementation returns true.</p>
     * 
     * @return true if the scope is active and bean resolution is valid
     */
    default boolean isActive() {
        return true;
    }
    
    /**
     * Returns a description of this scope for debugging purposes.
     * 
     * @return a string description including the scope ID and any relevant state
     */
    default String describe() {
        return "Scope[" + getId() + "]";
    }
}
