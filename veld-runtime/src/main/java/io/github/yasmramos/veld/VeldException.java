package io.github.yasmramos.veld;

/**
 * Exception thrown when Veld cannot satisfy a dependency or find a component.
 * 
 * This exception is thrown when:
 * <ul>
 *   <li>A requested component is not found in the dependency injection container</li>
 *   <li>Dependency injection fails due to missing dependencies</li>
 *   <li>Circular dependencies are detected</li>
 *   <li>Component initialization fails during startup</li>
 * </ul>
 * 
 * @author Veld Team
 * @since 1.0.0
 */
public class VeldException extends RuntimeException {
    
    /**
     * Creates a new VeldException with the specified message.
     * 
     * @param message the detail message
     */
    public VeldException(String message) {
        super(message);
    }
    
    /**
     * Creates a new VeldException with the specified message and cause.
     * 
     * @param message the detail message
     * @param cause the underlying cause
     */
    public VeldException(String message, Throwable cause) {
        super(message, cause);
    }
    
    // ==================== VeldLite Exceptions ====================
    
    /**
     * Exception thrown when a requested component is not found in VeldLite.
     */
    public static class ComponentNotFoundException extends VeldException {
        
        /**
         * Creates a new ComponentNotFoundException.
         * 
         * @param message the detail message
         */
        public ComponentNotFoundException(String message) {
            super(message);
        }
    }
}