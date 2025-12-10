package io.github.yasmramos.runtime;

/**
 * Exception thrown by the Veld container when an error occurs.
 */
public class VeldException extends RuntimeException {

    public VeldException(String message) {
        super(message);
    }

    public VeldException(String message, Throwable cause) {
        super(message, cause);
    }
}
