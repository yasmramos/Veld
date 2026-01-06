package io.github.yasmramos.veld.runtime;

import io.github.yasmramos.veld.runtime.scope.Scope;
import io.github.yasmramos.veld.runtime.scope.SingletonScope;
import io.github.yasmramos.veld.runtime.scope.PrototypeScope;

/**
 * Defines the lifecycle scope of a component.
 * 
 * <p>This enum is maintained for backwards compatibility.
 * New code should use the {@link Scope} interface and
 * {@link io.github.yasmramos.veld.runtime.scope.ScopeRegistry} directly.</p>
 * 
 * <h2>Built-in Scopes:</h2>
 * <ul>
 *   <li>{@link #SINGLETON} - One instance shared across the container</li>
 *   <li>{@link #PROTOTYPE} - New instance for each request</li>
 * </ul>
 * 
 * <h2>Custom Scopes:</h2>
 * <p>Custom scopes can be registered via {@link io.github.yasmramos.veld.runtime.scope.ScopeRegistry}
 * and referenced by name in annotations.</p>
 * 
 * @deprecated Use {@link Scope} interface and {@link io.github.yasmramos.veld.runtime.scope.ScopeRegistry} instead
 */
@Deprecated
public enum LegacyScope {
    
    /**
     * Only one instance is created and shared.
     * This is the default scope for most components.
     */
    SINGLETON,
    
    /**
     * A new instance is created for each request.
     * The container does not track or manage these instances.
     */
    PROTOTYPE;
    
    /**
     * Converts this enum value to a Scope instance.
     * 
     * @return the corresponding Scope implementation
     * @deprecated Use {@link io.github.yasmramos.veld.runtime.scope.ScopeRegistry#get(String)} instead
     */
    @Deprecated
    public Scope toScope() {
        return switch (this) {
            case SINGLETON -> new SingletonScope();
            case PROTOTYPE -> new PrototypeScope();
        };
    }
    
    /**
     * Returns the scope ID for this scope.
     * 
     * @return the scope identifier string
     * @deprecated Use {@link Scope#getId()} instead
     */
    @Deprecated
    public String getScopeId() {
        return switch (this) {
            case SINGLETON -> "singleton";
            case PROTOTYPE -> "prototype";
        };
    }
    
    /**
     * Returns the default scope (SINGLETON).
     * 
     * @return the default scope enum value
     */
    public static LegacyScope getDefault() {
        return SINGLETON;
    }
    
    /**
     * Converts a scope ID string to a LegacyScope enum value.
     * 
     * @param id the scope identifier (e.g., "singleton", "prototype")
     * @return the corresponding LegacyScope enum, or null if not found
     */
    public static LegacyScope fromId(String id) {
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
     * Checks if a scope ID is valid.
     * 
     * @param id the scope identifier to check
     * @return true if a scope with this ID exists (including custom scopes)
     */
    public static boolean isValidScopeId(String id) {
        return fromId(id) != null;
    }
}
