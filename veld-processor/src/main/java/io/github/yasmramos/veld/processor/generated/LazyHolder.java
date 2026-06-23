package io.github.yasmramos.veld.processor.generated;

import java.util.function.Supplier;

/**
 * A thread-safe lazy initialization holder.
 * 
 * This class is generated at compile-time and moved to the processor
 * to eliminate runtime dependencies. Used internally by Veld to 
 * implement @Lazy injection points. The value is computed only once, 
 * on first access.
 *
 * @param <T> the type of the lazily-initialized value
 */
public final class LazyHolder<T> implements Provider<T> {
    
    private volatile T value;
    private volatile boolean initialized;
    private final Supplier<T> supplier;
    private final Object lock = new Object();
    
    /**
     * Creates a new LazyHolder with the given supplier.
     *
     * @param supplier the supplier that creates the value
     */
    public LazyHolder(Supplier<T> supplier) {
        this.supplier = supplier;
    }
    
    /**
     * Gets the value, initializing it if necessary.
     * This method is thread-safe and guarantees the supplier
     * is called at most once.
     *
     * @return the lazily-initialized value
     */
    @Override
    public T get() {
        if (!initialized) {
            synchronized (lock) {
                if (!initialized) {
                    value = supplier.get();
                    initialized = true;
                }
            }
        }
        return value;
    }
    
    /**
     * Returns true if the value has been initialized.
     *
     * @return true if initialized
     */
    public boolean isInitialized() {
        return initialized;
    }
}
