package io.github.yasmramos.veld.runtime.scope;

import java.util.function.UnaryOperator;

/**
 * Thread-safe context holder that allows setting and getting values across threads.
 * Unlike ThreadLocal, this is designed for cases where the same context
 * needs to be accessed from multiple threads (e.g., thread pools).
 *
 * <p>This class provides atomic operations for thread-safe read-modify-write
 * operations on the held value.</p>
 *
 * <h2>Usage:</h2>
 * <pre>{@code
 * // Share context across threads in a thread pool
 * ContextHolder<RequestContext> holder = new ContextHolder<>();
 *
 * executor.submit(() -> {
 *     holder.set(requestContext);
 *     // All threads in the pool can access the same context
 *     processRequest();
 * });
 * }</pre>
 *
 * @param <T> the type of context value
 */
public final class ContextHolder<T> {

    private volatile T value;

    /**
     * Creates a new ContextHolder with a null initial value.
     */
    public ContextHolder() {
        this.value = null;
    }

    /**
     * Creates a new ContextHolder with the specified initial value.
     *
     * @param initialValue the initial value
     */
    public ContextHolder(T initialValue) {
        this.value = initialValue;
    }

    /**
     * Sets the context value.
     *
     * @param value the value to set
     */
    public void set(T value) {
        this.value = value;
    }

    /**
     * Gets the current context value.
     *
     * @return the current value, or null if not set
     */
    public T get() {
        return value;
    }

    /**
     * Gets the current value and sets a new value atomically.
     *
     * @param newValue the new value to set
     * @return the previous value
     */
    public T getAndSet(T newValue) {
        T oldValue = this.value;
        this.value = newValue;
        return oldValue;
    }

    /**
     * Atomically updates the current value using the given function.
     *
     * @param updateFunction the function to apply to the current value
     * @return the new value
     * @throws NullPointerException if the update function returns null
     */
    public T updateAndGet(UnaryOperator<T> updateFunction) {
        T newValue = updateFunction.apply(this.value);
        if (newValue == null) {
            throw new NullPointerException("ContextHolder update function cannot return null");
        }
        this.value = newValue;
        return newValue;
    }

    /**
     * Clears the context value.
     */
    public void clear() {
        this.value = null;
    }

    /**
     * Returns whether the holder contains a value.
     *
     * @return true if a value is present
     */
    public boolean hasValue() {
        return value != null;
    }

    /**
     * Gets the value, or the default value if null.
     *
     * @param defaultValue the default value to return if null
     * @return the current value or the default
     */
    public T getOrDefault(T defaultValue) {
        T v = value;
        return v != null ? v : defaultValue;
    }
}
