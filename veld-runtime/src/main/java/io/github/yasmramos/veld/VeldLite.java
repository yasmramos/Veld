package io.github.yasmramos.veld;

/**
 * Lightweight Service Locator for Veld framework.
 * 
 * <p>Provides a simple, runtime-only alternative to the full DI container.
 * No annotation processing required - all registration is manual.</p>
 * 
 * <h2>Usage Example:</h2>
 * <pre>{@code
 * // Register components (eager - singleton)
 * VeldLite.register(Database.class, new PostgresDatabase("jdbc:postgresql://localhost/db"));
 * 
 * // Register with manual dependency injection (lazy)
 * VeldLite.register(UserRepository.class, () -> new UserRepositoryImpl(
 *     VeldLite.get(Database.class)
 * ));
 * 
 * VeldLite.register(UserService.class, () -> new UserService(
 *     VeldLite.get(UserRepository.class)
 * ));
 * 
 * // Retrieve
 * UserService service = VeldLite.get(UserService.class);
 * }</pre>
 * 
 * <h2>Features:</h2>
 * <ul>
 *   <li>Zero annotation processing</li>
 *   <li>Manual dependency wiring via lambdas</li>
 *   <li>Lazy initialization (components created on first access)</li>
 *   <li>Singleton scope by default (lazy singleton pattern)</li>
 *   <li>Thread-safe</li>
 * </ul>
 */
public final class VeldLite {
    
    private static final LiteComponentRegistry REGISTRY = new LiteComponentRegistry();
    
    private VeldLite() {
        // Utility class - no instantiation
    }
    
    /**
     * Registers a pre-created singleton instance.
     * The instance will be returned directly on lookup.
     * 
     * @param <T> the component type
     * @param type the component class
     * @param instance the singleton instance to register
     */
    public static <T> void register(Class<T> type, T instance) {
        REGISTRY.register(type, instance);
    }
    
    /**
     * Registers a lazy factory for component creation.
     * The factory will be invoked on first {@code get()} call.
     * 
     * <p>Use this for components with dependencies that need manual wiring.</p>
     * 
     * @param <T> the component type
     * @param type the component class
     * @param factory the supplier that creates the instance
     */
    public static <T> void register(Class<T> type, VeldSupplier<T> factory) {
        REGISTRY.register(type, factory);
    }
    
    /**
     * Retrieves a component by type.
     * 
     * @param <T> the component type
     * @param type the component class
     * @return the component instance
     * @throws VeldException if component is not registered
     */
    @SuppressWarnings("unchecked")
    public static <T> T get(Class<T> type) {
        return (T) REGISTRY.get(type);
    }
    
    /**
     * Retrieves a component by type, or null if not registered.
     * 
     * @param <T> the component type
     * @param type the component class
     * @return the component instance, or null if not found
     */
    @SuppressWarnings("unchecked")
    public static <T> T getOrNull(Class<T> type) {
        return (T) REGISTRY.getOrNull(type);
    }
    
    /**
     * Checks if a component is registered.
     * 
     * @param type the component class
     * @return true if registered
     */
    public static boolean contains(Class<?> type) {
        return REGISTRY.contains(type);
    }
    
    /**
     * Clears all registrations.
     * Useful for testing or resetting the locator.
     */
    public static void clear() {
        REGISTRY.clear();
    }
    
    /**
     * Gets the number of registered components.
     * 
     * @return component count
     */
    public static int componentCount() {
        return REGISTRY.componentCount();
    }
}
