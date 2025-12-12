package io.github.yasmramos.veld.runtime;

/**
 * Defines the lifecycle scope of a component.
 */
public enum Scope {
    
    /**
     * Only one instance is created and shared.
     */
    SINGLETON,
    
    /**
     * A new instance is created for each request.
     */
    PROTOTYPE
}
