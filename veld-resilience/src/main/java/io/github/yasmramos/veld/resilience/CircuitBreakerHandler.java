package io.github.yasmramos.veld.resilience;

import io.github.yasmramos.veld.annotation.CircuitBreaker;
import io.github.yasmramos.veld.aop.InvocationContext;
import io.github.yasmramos.veld.aop.MethodInterceptor;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Circuit Breaker pattern implementation.
 */
public class CircuitBreakerHandler implements MethodInterceptor {

    private static final ConcurrentHashMap<String, CircuitState> circuits = new ConcurrentHashMap<>();

    @Override
    public Object invoke(InvocationContext ctx) throws Throwable {
        CircuitBreaker cb = ctx.getMethod().getAnnotation(CircuitBreaker.class);
        if (cb == null) return ctx.proceed();
        
        String key = cb.name().isEmpty() ? ctx.getMethod().toString() : cb.name();
        CircuitState state = circuits.computeIfAbsent(key, k -> new CircuitState(cb));
        
        if (state.isOpen()) {
            if (state.shouldAttemptReset()) {
                state.toHalfOpen();
            } else {
                return invokeFallback(ctx, cb, new CircuitOpenException("Circuit " + key + " is OPEN"));
            }
        }
        
        try {
            Object result = ctx.proceed();
            state.recordSuccess();
            return result;
        } catch (Throwable t) {
            state.recordFailure();
            if (state.shouldTrip()) state.toOpen();
            return invokeFallback(ctx, cb, t);
        }
    }

    private Object invokeFallback(InvocationContext ctx, CircuitBreaker cb, Throwable cause) throws Throwable {
        if (!cb.fallbackMethod().isEmpty()) {
            try {
                return ctx.getTarget().getClass()
                    .getMethod(cb.fallbackMethod(), ctx.getMethod().getParameterTypes())
                    .invoke(ctx.getTarget(), ctx.getParameters());
            } catch (Exception e) { throw cause; }
        }
        throw cause;
    }

    private static class CircuitState {
        enum State { CLOSED, OPEN, HALF_OPEN }
        private volatile State state = State.CLOSED;
        private final AtomicInteger failureCount = new AtomicInteger(0);
        private final AtomicInteger successCount = new AtomicInteger(0);
        private final AtomicLong lastFailureTime = new AtomicLong(0);
        private final int failureThreshold, successThreshold;
        private final long resetTimeout;

        CircuitState(CircuitBreaker cb) {
            this.failureThreshold = cb.failureThreshold();
            this.successThreshold = cb.successThreshold();
            this.resetTimeout = cb.resetTimeout();
        }
        boolean isOpen() { return state == State.OPEN; }
        boolean shouldAttemptReset() { return System.currentTimeMillis() - lastFailureTime.get() >= resetTimeout; }
        boolean shouldTrip() { return failureCount.get() >= failureThreshold; }
        void recordSuccess() { failureCount.set(0); if (state == State.HALF_OPEN && successCount.incrementAndGet() >= successThreshold) toClosed(); }
        void recordFailure() { failureCount.incrementAndGet(); lastFailureTime.set(System.currentTimeMillis()); successCount.set(0); }
        void toOpen() { state = State.OPEN; }
        void toHalfOpen() { state = State.HALF_OPEN; successCount.set(0); }
        void toClosed() { state = State.CLOSED; failureCount.set(0); }
    }

    public static class CircuitOpenException extends RuntimeException {
        public CircuitOpenException(String message) { super(message); }
    }
}
