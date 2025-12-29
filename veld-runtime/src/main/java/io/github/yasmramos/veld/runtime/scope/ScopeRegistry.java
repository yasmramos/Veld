package io.github.yasmramos.veld.runtime.scope;

import io.github.yasmramos.veld.VeldException;

import java.util.Map;
import java.util.ServiceLoader;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Registry for managing scope instances.
 * 
 * <p>The ScopeRegistry maintains all available scopes in the application,
 * allowing scopes to be looked up by their ID and enabling automatic
 * initialization of built-in and custom scopes.</p>
 * 
 * <p>Features:</p>
 * <ul>
 *   <li>Thread-safe scope registration and lookup</li>
 *   <li>Built-in scopes (singleton, prototype) registered by default</li>
 *   <li>Custom scopes via registration or SPI</li>
 *   <li>Lazy initialization of scopes</li>
 *   <li>Default scope configuration</li>
 * </ul>
 * 
 * <h2>Usage:</h2>
 * <pre>{@code
 * // Register a custom scope
 * ScopeRegistry.register("mythread", () -> new ThreadScope());
 * 
 * // Get a scope by ID
 * Scope scope = ScopeRegistry.get("singleton");
 * 
 * // Check if a scope exists
 * if (ScopeRegistry.contains("request")) {
 *     // Use request scope
 * }
 * }</pre>
 * 
 * <h2>SPI Usage:</h2>
 * <p>Custom scopes can be discovered via Java SPI by implementing
 * {@link ScopeProvider} and registering in META-INF/services:</p>
 * <pre>{@code
 * # META-INF/services/io.github.yasmramos.veld.runtime.scope.ScopeProvider
 * com.example.MyScopeProvider
 * }</pre>
 * 
 * @see Scope
 * @see SingletonScope
 * @see PrototypeScope
 */
public final class ScopeRegistry {
    
    private static final Map<String, Scope> SCOPES = new ConcurrentHashMap<>();
    private static final Map<String, Supplier<Scope>> SCOPE_FACTORIES = new ConcurrentHashMap<>();
    private static final Map<String, ScopeMetadata> SCOPE_METADATA = new ConcurrentHashMap<>();
    
    private static volatile String defaultScopeId = SingletonScope.SCOPE_ID;
    private static volatile boolean initialized = false;
    
    // Private constructor to prevent instantiation
    private ScopeRegistry() {
        throw new UnsupportedOperationException("ScopeRegistry is a static utility class");
    }
    
    /**
     * Initializes the registry with built-in scopes.
     * Called automatically on first access.
     */
    private static synchronized void initialize() {
        if (initialized) {
            return;
        }
        
        // Register built-in scopes
        registerBuiltInScopes();
        
        // Load scopes via SPI
        loadScopeProviders();
        
        initialized = true;
    }
    
    /**
     * Registers the built-in scopes.
     */
    private static void registerBuiltInScopes() {
        // Singleton scope
        registerInternal(SingletonScope.SCOPE_ID, SingletonScope::new, 
            new ScopeMetadata(SingletonScope.SCOPE_ID, "Singleton", 
                "Default scope - one instance per container"));
        
        // Prototype scope
        registerInternal(PrototypeScope.SCOPE_ID, PrototypeScope::new,
            new ScopeMetadata(PrototypeScope.SCOPE_ID, "Prototype",
                "Creates new instance for each request"));
    }
    
    /**
     * Loads scope providers via Java SPI.
     */
    private static void loadScopeProviders() {
        try {
            ServiceLoader<ScopeProvider> loader = ServiceLoader.load(ScopeProvider.class);
            for (ScopeProvider provider : loader) {
                String scopeId = provider.getScopeId();
                Supplier<Scope> factory = provider::getScope;
                String displayName = provider.getDisplayName();
                String description = provider.getDescription();
                
                registerInternal(scopeId, factory, new ScopeMetadata(scopeId, displayName, description));
            }
        } catch (Exception e) {
            // SPI loading is optional - log but don't fail
            System.err.println("Warning: Failed to load scope providers via SPI: " + e.getMessage());
        }
    }
    
    /**
     * Registers a scope internally (during initialization).
     * 
     * @param id the scope ID
     * @param factory the scope factory
     * @param metadata the scope metadata
     */
    private static void registerInternal(String id, Supplier<Scope> factory, ScopeMetadata metadata) {
        SCOPE_FACTORIES.put(id, factory);
        SCOPE_METADATA.put(id, metadata);
    }
    
    /**
     * Registers a custom scope.
     * 
     * <p>Scopes can be registered by ID and used by name in annotations.
     * The scope instance is created lazily on first use.</p>
     * 
     * <h2>Example:</h2>
     * <pre>{@code
     * ScopeRegistry.register("session", () -> new SessionScope(sessionManager));
     * 
     * // Now @MyComponent with scope="session" will use SessionScope
     * }</pre>
     * 
     * @param id the unique scope identifier
     * @param factory supplier that creates the scope instance
     * @param displayName human-readable name for debugging
     * @param description description of the scope behavior
     * @throws IllegalArgumentException if a scope with the same ID is already registered
     */
    public static void register(String id, Supplier<Scope> factory, String displayName, String description) {
        if (id == null || id.isEmpty()) {
            throw new IllegalArgumentException("Scope ID cannot be null or empty");
        }
        if (factory == null) {
            throw new IllegalArgumentException("Scope factory cannot be null");
        }
        if (SCOPE_FACTORIES.containsKey(id)) {
            throw new IllegalArgumentException("Scope with ID '" + id + "' is already registered");
        }
        
        SCOPE_FACTORIES.put(id, factory);
        SCOPE_METADATA.put(id, new ScopeMetadata(id, displayName, description));
    }
    
    /**
     * Registers a custom scope with default display name and description.
     * 
     * @param id the unique scope identifier
     * @param factory supplier that creates the scope instance
     */
    public static void register(String id, Supplier<Scope> factory) {
        register(id, factory, id, "Custom scope");
    }
    
    /**
     * Registers a scope instance directly.
     * 
     * @param scope the scope instance to register
     */
    public static void register(Scope scope) {
        if (scope == null) {
            throw new IllegalArgumentException("Scope cannot be null");
        }
        String id = scope.getId();
        if (SCOPES.containsKey(id)) {
            throw new IllegalArgumentException("Scope with ID '" + id + "' is already registered");
        }
        SCOPES.put(id, scope);
        SCOPE_METADATA.putIfAbsent(id, new ScopeMetadata(id, scope.getDisplayName(), "Custom scope"));
    }
    
    /**
     * Gets or creates a scope by its ID.
     * 
     * @param id the scope identifier
     * @return the scope instance
     * @throws NoSuchScopeException if no scope is registered with the given ID
     */
    public static Scope get(String id) {
        initialize();
        
        // Check if already instantiated
        Scope scope = SCOPES.get(id);
        if (scope != null) {
            return scope;
        }
        
        // Get factory and create instance
        Supplier<Scope> factory = SCOPE_FACTORIES.get(id);
        if (factory == null) {
            throw new NoSuchScopeException("No scope registered with ID: " + id + 
                ". Available scopes: " + getRegisteredScopeIds());
        }
        
        // Create and cache the instance
        scope = factory.get();
        SCOPES.put(id, scope);
        return scope;
    }
    
    /**
     * Gets a scope by ID, returning null if not found.
     * 
     * @param id the scope identifier
     * @return the scope instance, or null if not registered
     */
    public static Scope getOrNull(String id) {
        initialize();
        try {
            return get(id);
        } catch (NoSuchScopeException e) {
            return null;
        }
    }
    
    /**
     * Checks if a scope is registered.
     * 
     * @param id the scope identifier
     * @return true if the scope is registered
     */
    public static boolean contains(String id) {
        initialize();
        return SCOPES.containsKey(id) || SCOPE_FACTORIES.containsKey(id);
    }
    
    /**
     * Returns the default scope ID.
     * 
     * @return the default scope ID (typically "singleton")
     */
    public static String getDefaultScopeId() {
        initialize();
        return defaultScopeId;
    }
    
    /**
     * Sets the default scope for beans without an explicit scope.
     * 
     * @param id the scope ID to use as default
     * @throws NoSuchScopeException if no scope is registered with the given ID
     */
    public static void setDefaultScope(String id) {
        initialize();
        if (!contains(id)) {
            throw new NoSuchScopeException("Cannot set default scope to unregistered scope: " + id);
        }
        defaultScopeId = id;
    }
    
    /**
     * Gets metadata for a registered scope.
     * 
     * @param id the scope identifier
     * @return the scope metadata, or null if not found
     */
    public static ScopeMetadata getMetadata(String id) {
        initialize();
        return SCOPE_METADATA.get(id);
    }
    
    /**
     * Returns all registered scope IDs.
     * 
     * @return unmodifiable set of scope IDs
     */
    public static java.util.Set<String> getRegisteredScopeIds() {
        initialize();
        // Combine both instantiated and factory-only scopes
        java.util.Set<String> allIds = new java.util.HashSet<>(SCOPES.keySet());
        allIds.addAll(SCOPE_FACTORIES.keySet());
        return java.util.Collections.unmodifiableSet(allIds);
    }
    
    /**
     * Destroys all registered scopes and clears the registry.
     * Called during container shutdown.
     */
    public static void destroy() {
        // Destroy all instantiated scopes
        for (Scope scope : SCOPES.values()) {
            try {
                scope.destroy();
            } catch (Exception e) {
                System.err.println("Error destroying scope '" + scope.getId() + "': " + e.getMessage());
            }
        }
        
        // Clear all maps
        SCOPES.clear();
        SCOPE_FACTORIES.clear();
        SCOPE_METADATA.clear();
        
        initialized = false;
    }
    
    /**
     * Resets the registry for testing purposes.
     * NOT FOR PRODUCTION USE.
     */
    static void reset() {
        destroy();
        defaultScopeId = SingletonScope.SCOPE_ID;
    }
    
    /**
     * Exception thrown when a requested scope is not found.
     */
    public static class NoSuchScopeException extends VeldException {
        NoSuchScopeException(String message) {
            super(message);
        }
    }
    
    /**
     * Metadata about a registered scope.
     */
    public static final class ScopeMetadata {
        private final String id;
        private final String displayName;
        private final String description;
        
        ScopeMetadata(String id, String displayName, String description) {
            this.id = id;
            this.displayName = displayName;
            this.description = description;
        }
        
        public String getId() {
            return id;
        }
        
        public String getDisplayName() {
            return displayName;
        }
        
        public String getDescription() {
            return description;
        }
    }
}
