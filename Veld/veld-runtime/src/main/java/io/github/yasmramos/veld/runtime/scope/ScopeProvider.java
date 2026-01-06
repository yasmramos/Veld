package io.github.yasmramos.veld.runtime.scope;

/**
 * Service Provider Interface for discovering custom scopes.
 * 
 * <p>Implement this interface to provide custom scopes that will be
 * automatically discovered and registered by Veld at startup.</p>
 * 
 * <h2>Usage:</h2>
 * <ol>
 *   <li>Implement this interface with your custom scope</li>
 *   <li>Register implementation in META-INF/services</li>
 *   <li>Scopes will be automatically loaded</li>
 * </ol>
 * 
 * <h2>Example:</h2>
 * <pre>{@code
 * public class CustomScopeProvider implements ScopeProvider {
 *     @Override
 *     public String getScopeId() {
 *         return "custom";
 *     }
 *     
 *     @Override
 *     public Scope getScope() {
 *         return new CustomScope();
 *     }
 *     
 *     @Override
 *     public String getDisplayName() {
 *         return "Custom Scope";
 *     }
 *     
 *     @Override
 *     public String getDescription() {
 *         return "A custom scope for special use cases";
 *     }
 * }
 * }</pre>
 * 
 * <h2>META-INF/services registration:</h2>
 * <pre>{@code
 * # File: META-INF/services/io.github.yasmramos.veld.runtime.scope.ScopeProvider
 * 
 * com.example.myapp.CustomScopeProvider
 * }</pre>
 * 
 * @see Scope
 * @see ScopeRegistry
 */
public interface ScopeProvider {
    
    /**
     * Returns the unique identifier for this scope.
     * This ID is used to reference the scope in annotations and configuration.
     * 
     * <p>Common scope IDs:</p>
     * <ul>
     *   <li>{@code "singleton"} - Default scope, one instance per container</li>
     *   <li>{@code "prototype"} - New instance for each request</li>
     *   <li>{@code "request"} - One instance per HTTP request</li>
     *   <li>{@code "session"} - One instance per HTTP session</li>
     *   <li>{@code "application"} - One instance per application context</li>
     * </ul>
     * 
     * @return the scope identifier (should be unique across the application)
     */
    String getScopeId();
    
    /**
     * Creates and returns the scope instance.
     * 
     * <p>This method is called once when the scope is first needed.
     * The returned instance will be cached and reused.</p>
     * 
     * @return a new instance of this scope
     */
    Scope getScope();
    
    /**
     * Returns a human-readable display name for this scope.
     * Used in debugging, logging, and tooling output.
     * 
     * @return the display name (e.g., "Request Scope", "Session Scope")
     */
    String getDisplayName();
    
    /**
     * Returns a description of this scope's behavior.
     * Used in documentation and debugging tools.
     * 
     * @return a description of what this scope does
     */
    String getDescription();
}
