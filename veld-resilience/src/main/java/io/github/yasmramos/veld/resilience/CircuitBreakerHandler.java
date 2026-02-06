package io.github.yasmramos.veld.resilience;

import io.github.yasmramos.veld.annotation.CircuitBreaker;
import io.github.yasmramos.veld.aop.InvocationContext;
import io.github.yasmramos.veld.aop.MethodInterceptor;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Advanced Circuit Breaker implementation with Sliding Window support.
 * 
 * @author Veld Framework Team
 * @since 1.1.0
 */
public class CircuitBreakerHandler implements MethodInterceptor {

    private static final ConcurrentHashMap<String, CircuitState> circuits = new ConcurrentHashMap<>();

    @Override
    public Object invoke(InvocationContext ctx) throws Throwable {
        if (!ctx.hasAnnotation(CircuitBreaker.class)) {
            return ctx.proceed();
        }

        CircuitBreaker cb = ctx.getAnnotation(CircuitBreaker.class);
        String key = cb.name().isEmpty() ? ctx.getDeclaringClassName() + "." + ctx.getMethodName() : cb.name();
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
            if (state.shouldTrip()) {
                state.toOpen();
            }
            return invokeFallback(ctx, cb, t);
        }
    }

    private Object invokeFallback(InvocationContext ctx, CircuitBreaker cb, Throwable cause) throws Throwable {
        if (!cb.fallbackMethod().isEmpty()) {
            try {
                return ctx.getTarget().getClass()
                    .getMethod(cb.fallbackMethod(), ctx.getParameterTypes())
                    .invoke(ctx.getTarget(), ctx.getParameters());
            } catch (Exception e) {
                // If fallback fails, throw the original cause
                throw cause;
            }
        }
        throw cause;
    }

    private static class CircuitState {
        enum State { CLOSED, OPEN, HALF_OPEN }
        
        private final AtomicReference<State> state = new AtomicReference<>(State.CLOSED);
        private final SlidingWindow window;
        private final AtomicLong lastFailureTime = new AtomicLong(0);
        private final AtomicInteger halfOpenSuccessCount = new AtomicInteger(0);
        
        private final int failureThreshold;
        private final int successThreshold;
        private final long resetTimeout;

        CircuitState(CircuitBreaker cb) {
            this.failureThreshold = cb.failureThreshold();
            this.successThreshold = cb.successThreshold();
            this.resetTimeout = cb.resetTimeout();
            // Using a fixed size window for simplicity in this implementation
            this.window = new SlidingWindow(failureThreshold * 2); 
        }

        boolean isOpen() {
            return state.get() == State.OPEN;
        }

        boolean shouldAttemptReset() {
            return System.currentTimeMillis() - lastFailureTime.get() >= resetTimeout;
        }

        boolean shouldTrip() {
            if (state.get() == State.HALF_OPEN) return true;
            return window.getFailureCount() >= failureThreshold;
        }

        void recordSuccess() {
            window.record(true);
            if (state.get() == State.HALF_OPEN) {
                if (halfOpenSuccessCount.incrementAndGet() >= successThreshold) {
                    toClosed();
                }
            }
        }

        void recordFailure() {
            window.record(false);
            lastFailureTime.set(System.currentTimeMillis());
            if (state.get() == State.HALF_OPEN) {
                toOpen();
            }
        }

        void toOpen() {
            state.set(State.OPEN);
        }

        void toHalfOpen() {
            state.set(State.HALF_OPEN);
            halfOpenSuccessCount.set(0);
        }

        void toClosed() {
            state.set(State.CLOSED);
            window.reset();
        }
    }

    /**
     * Simple fixed-size sliding window implementation.
     */
    private static class SlidingWindow {
        private final boolean[] samples;
        private int head = 0;
        private int count = 0;
        private int failures = 0;

        SlidingWindow(int size) {
            this.samples = new boolean[size];
        }

        synchronized void record(boolean success) {
            if (count == samples.length) {
                if (!samples[head]) failures--;
            } else {
                count++;
            }
            
            samples[head] = success;
            if (!success) failures++;
            
            head = (head + 1) % samples.length;
        }

        synchronized int getFailureCount() {
            return failures;
        }

        synchronized void reset() {
            head = 0;
            count = 0;
            failures = 0;
        }
    }

    public static class CircuitOpenException extends RuntimeException {
        public CircuitOpenException(String message) {
            super(message);
        }
    }
}
