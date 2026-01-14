package io.github.yasmramos.veld.runtime.scope;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.UnaryOperator;

/**
 * Thread-safe context holder that allows setting and getting values across threads.
 * Unlike ThreadLocal, this is designed for cases where the same context
 * needs to be accessed from multiple threads (e.g., thread pools).
 *
 * <p>This class provides atomic operations for thread-safe read-modify-write
 * operations on the held value using AtomicReference internally.</p>
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

    private final AtomicReference<T> value;

    /**
     * Creates a new ContextHolder with a null initial value.
     */
    public ContextHolder() {
        this.value = new AtomicReference<>(null);
    }

    /**
     * Creates a new ContextHolder with the specified initial value.
     *
     * @param initialValue the initial value
     */
    public ContextHolder(T initialValue) {
        this.value = new AtomicReference<>(initialValue);
    }

    /**
     * Sets the context value.
     *
     * @param value the value to set
     */
    public void set(T value) {
        this.value.set(value);
    }

    /**
     * Gets the current context value.
     *
     * @return the current value, or null if not set
     */
    public T get() {
        return value.get();
    }

    /**
     * Gets the current value and sets a new value atomically.
     *
     * @param newValue the new value to set
     * @return the previous value
     */
    public T getAndSet(T newValue) {
        return value.getAndSet(newValue);
    }

    /**
     * Atomically updates the current value using the given function.
     * Uses AtomicReference's built-in updateAndGet for efficient atomic updates.
     *
     * @param updateFunction the function to apply to the current value
     * @return the new value
     * @throws NullPointerException if the update function returns null
     */
    public T updateAndGet(UnaryOperator<T> updateFunction) {
        T result = value.updateAndGet(current -> {
            T newValue = updateFunction.apply(current);
            if (newValue == null) {
                throw new NullPointerException("ContextHolder update function cannot return null");
            }
            return newValue;
        });
        return result;
    }

    /**
     * Atomically updates the current value using the given function.
     * Gets the current value first, then applies the update atomically.
     *
     * @param updateFunction the function to apply to the current value
     * @return the new value
     * @throws NullPointerException if the update function returns null
     */
    public T getAndUpdate(UnaryOperator<T> updateFunction) {
        return value.getAndUpdate(updateFunction);
    }

    /**
     * Clears the context value.
     */
    public void clear() {
        value.set(null);
    }

    /**
     * Returns whether the holder contains a value.
     *
     * @return true if a value is present
     */
    public boolean hasValue() {
        return value.get() != null;
    }

    /**
     * Gets the value, or the default value if null.
     *
     * @param defaultValue the default value to return if null
     * @return the current value or the default
     */
    public T getOrDefault(T defaultValue) {
        return value.getOrDefault(defaultValue);
    }

    /**
     * Atomically sets the value to the given new value if the current value
     * equals the expected value.
     *
     * @param expected the expected current value
     * @param newValue the new value to set
     * @return true if successful, false if the current value was not equal to expected
     */
    public boolean compareAndSet(T expected, T newValue) {
        return value.compareAndSet(expected, newValue);
    }
}
