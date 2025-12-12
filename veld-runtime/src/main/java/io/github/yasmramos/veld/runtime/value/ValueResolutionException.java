package io.github.yasmramos.veld.runtime.value;

/**
 * Exception thrown when a value cannot be resolved or converted.
 * 
 * <p>This exception is thrown in the following scenarios:
 * <ul>
 *   <li>A required property is not found and no default value is specified</li>
 *   <li>A property value cannot be converted to the required type</li>
 *   <li>Configuration file cannot be parsed</li>
 * </ul>
 * 
 * @author Veld Framework
 * @since 1.0.0-alpha.5
 */
public class ValueResolutionException extends RuntimeException {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * Creates a new exception with the specified message.
     * 
     * @param message the error message
     */
    public ValueResolutionException(String message) {
        super(message);
    }
    
    /**
     * Creates a new exception with the specified message and cause.
     * 
     * @param message the error message
     * @param cause the underlying cause
     */
    public ValueResolutionException(String message, Throwable cause) {
        super(message, cause);
    }
}
