package io.github.yasmramos.veld.runtime;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.function.Supplier;

/**
 * Lock-free lazy initialization using VarHandle CAS.
 * Zero allocations after initialization. Sub-nanosecond hot path.
 * 
 * Uses Racy Single-Check idiom with CAS for thread safety.
 */
public final class LockFreeLazyHolder<T> implements Provider<T> {
    
    private static final VarHandle VALUE;
    private static final Object UNINITIALIZED = new Object();
    
    static {
        try {
            VALUE = MethodHandles.lookup()
                .findVarHandle(LockFreeLazyHolder.class, "value", Object.class);
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }
    
    @SuppressWarnings("unused") // accessed via VarHandle
    private volatile Object value = UNINITIALIZED;
    private final Supplier<T> supplier;
    
    public LockFreeLazyHolder(Supplier<T> supplier) {
        this.supplier = supplier;
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public T get() {
        Object v = VALUE.getAcquire(this);
        if (v != UNINITIALIZED) {
            return (T) v;
        }
        return initializeAndGet();
    }
    
    @SuppressWarnings("unchecked")
    private T initializeAndGet() {
        T newValue = supplier.get();
        // CAS: if still UNINITIALIZED, set to newValue
        // If another thread won, use their value (idempotent suppliers only)
        if (VALUE.compareAndSet(this, UNINITIALIZED, newValue)) {
            return newValue;
        }
        // Another thread initialized - return their value
        return (T) VALUE.getAcquire(this);
    }
    
    public boolean isInitialized() {
        return VALUE.getAcquire(this) != UNINITIALIZED;
    }
}
