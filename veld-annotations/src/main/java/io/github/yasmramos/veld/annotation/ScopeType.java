package io.github.yasmramos.veld.annotation;

/**
 * Enumeration of built-in scope types for Veld components.
 * 
 * <p>This enum provides type-safe scope definitions for components,
 * eliminating the need for string-based scope identifiers and preventing
 * typos and invalid scope names at compile time.</p>
 * 
 * <p>Usage:</p>
 * <pre>{@code
 * @Component
 * public class MyService {
 *     // Uses singleton scope (default)
 * }
 * 
 * @Component(scope = ScopeType.PROTOTYPE)
 * public class StatelessProcessor {
 *     // New instance created for each request
 * }
 * }</pre>
 * 
 * <h2>Built-in Scope Types:</h2>
 * <ul>
 *   <li>{@link #SINGLETON} - One shared instance per container (default)</li>
 *   <li>{@link #PROTOTYPE} - New instance for each request</li>
 * </ul>
 * 
 * <h2>Custom Scopes:</h2>
 * <p>Custom scopes can be defined using the {@link io.github.yasmramos.veld.annotation.VeldScope}
 * annotation and registered via {@link io.github.yasmramos.veld.runtime.scope.ScopeRegistry}.</p>
 * 
 * @see io.github.yasmramos.veld.annotation.VeldScope
 * @see io.github.yasmramos.veld.runtime.scope.ScopeRegistry
 */
public enum ScopeType {
    
    /**
     * Singleton scope - one shared instance per container.
     * 
     * <p>This is the default scope for all components. A single instance
     * is created and shared across all injections and references.</p>
     */
    SINGLETON("singleton"),
    
    /**
     * Prototype scope - new instance for each request.
     * 
     * <p>A new instance is created each time the component is requested
     * from the container. The container does not track or manage these
     * instances.</p>
     */
    PROTOTYPE("prototype");
    
    /**
     * The scope identifier for this scope type.
     */
    private final String scopeId;
    
    /**
     * Creates a new ScopeType with the given scope ID.
     * 
     * @param scopeId the scope identifier string
     */
    ScopeType(String scopeId) {
        this.scopeId = scopeId;
    }
    
    /**
     * Returns the scope identifier for this scope type.
     * 
     * @return the scope ID string (e.g., "singleton", "prototype")
     */
    public String getScopeId() {
        return scopeId;
    }
    
    /**
     * Returns the default scope type (SINGLETON).
     * 
     * @return the default ScopeType
     */
    public static ScopeType getDefault() {
        return SINGLETON;
    }
    
    /**
     * Converts a scope ID string to a ScopeType enum value.
     * 
     * @param id the scope identifier (e.g., "singleton", "prototype")
     * @return the corresponding ScopeType enum, or null if not a built-in type
     */
    public static ScopeType fromScopeId(String id) {
        if (id == null) {
            return null;
        }
        return switch (id.toLowerCase()) {
            case "singleton" -> SINGLETON;
            case "prototype" -> PROTOTYPE;
            default -> null;
        };
    }
    
    /**
     * Checks if the given scope ID represents a built-in scope type.
     * 
     * @param id the scope identifier to check
     * @return true if it's a built-in scope (singleton or prototype)
     */
    public static boolean isBuiltInScope(String id) {
        return fromScopeId(id) != null;
    }
}
