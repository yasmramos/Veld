package io.github.yasmramos.veld.runtime.scope;

import io.github.yasmramos.veld.runtime.ComponentFactory;

/**
 * Scope implementation for prototype (non-singleton) beans.
 * 
 * <p>A prototype scope creates a new instance of the bean each time
 * it is requested from the container. The container does not manage
 * the lifecycle of prototype beans - it simply creates the instance
 * and returns it.</p>
 * 
 * <p>Key characteristics:</p>
 * <ul>
 *   <li>New instance created for each request</li>
 *   <li>Container does not track or manage prototype instances</li>
 *   <li>No lifecycle callbacks are invoked (no @PostConstruct/@PreDestroy)</li>
 *   <li>Thread-safe - each request gets its own instance</li>
 * </ul>
 * 
 * <h2>Usage:</h2>
 * <pre>{@code
 * @Prototype
 * public class StatelessProcessor {
 *     private final Configuration config;
 *     
 *     public StatelessProcessor(Configuration config) {
 *         this.config = config; // New config instance per use
 *     }
 *     
 *     public Result process(Data data) {
 *         // Process with fresh instance
 *         return new Result(config, data);
 *     }
 * }
 * }</pre>
 * 
 * <h2>When to Use Prototype Scope:</h2>
 * <ul>
 *   <li>Stateful beans where each client needs its own instance</li>
 *   <li>Beans with mutable state that should not be shared</li>
 *   <li>Integration with external systems where isolation is required</li>
 * </ul>
 * 
 * @see Scope
 * @see SingletonScope
 * @see io.github.yasmramos.veld.annotation.Prototype
 */
public final class PrototypeScope implements Scope {
    
    public static final String SCOPE_ID = "prototype";
    
    /**
     * Creates a new PrototypeScope instance.
     */
    public PrototypeScope() {
        // Default constructor for SPI and manual instantiation
    }
    
    @Override
    public String getId() {
        return SCOPE_ID;
    }
    
    @Override
    public String getDisplayName() {
        return "Prototype";
    }
    
    @Override
    public <T> T get(String name, ComponentFactory<T> factory) {
        // Always create a new instance
        return factory.create();
    }
    
    @Override
    public Object remove(String name) {
        // Prototype scope doesn't store instances, so nothing to remove
        return null;
    }
    
    @Override
    public void destroy() {
        // Prototype beans are not tracked, so nothing to destroy
        // Each instance is responsible for its own lifecycle
    }
    
    @Override
    public String describe() {
        return "PrototypeScope[creates-new-instance-per-request]";
    }
    
    /**
     * PrototypeScope is stateless and always returns a new instance,
     * so this always returns true.
     * 
     * @return true (always active)
     */
    @Override
    public boolean isActive() {
        return true;
    }
}
