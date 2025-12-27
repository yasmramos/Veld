package io.github.yasmramos.veld.resilience;

import io.github.yasmramos.veld.annotation.CircuitBreaker;
import io.github.yasmramos.veld.aop.AspectHandler;
import io.github.yasmramos.veld.aop.MethodInvocation;

import java.lang.annotation.Annotation;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Circuit Breaker pattern implementation.
 * States: CLOSED (normal) -> OPEN (failing) -> HALF_OPEN (testing)
 */
public class CircuitBreakerHandler implements AspectHandler {

    private static final ConcurrentHashMap<String, CircuitState> circuits = new ConcurrentHashMap<>();

    @Override
    public Class<? extends Annotation> getAnnotationType() {
        return CircuitBreaker.class;
    }

    @Override
    public Object handle(MethodInvocation invocation) throws Throwable {
        CircuitBreaker cb = invocation.getMethod().getAnnotation(CircuitBreaker.class);
        String key = cb.name().isEmpty() ? invocation.getMethod().toString() : cb.name();
        
        CircuitState state = circuits.computeIfAbsent(key, k -> new CircuitState(cb));
        
        if (state.isOpen()) {
            if (state.shouldAttemptReset()) {
                state.toHalfOpen();
            } else {
                return invokeFallback(invocation, cb, new CircuitOpenException("Circuit " + key + " is OPEN"));
            }
        }
        
        try {
            Object result = invocation.proceed();
            state.recordSuccess();
            return result;
        } catch (Throwable t) {
            state.recordFailure();
            if (state.shouldTrip()) {
                state.toOpen();
            }
            return invokeFallback(invocation, cb, t);
        }
    }

    private Object invokeFallback(MethodInvocation invocation, CircuitBreaker cb, Throwable cause) throws Throwable {
        if (!cb.fallbackMethod().isEmpty()) {
            try {
                return invocation.getTarget().getClass()
                    .getMethod(cb.fallbackMethod(), invocation.getMethod().getParameterTypes())
                    .invoke(invocation.getTarget(), invocation.getArguments());
            } catch (Exception e) {
                throw cause;
            }
        }
        throw cause;
    }

    private static class CircuitState {
        enum State { CLOSED, OPEN, HALF_OPEN }
        
        private volatile State state = State.CLOSED;
        private final AtomicInteger failureCount = new AtomicInteger(0);
        private final AtomicInteger successCount = new AtomicInteger(0);
        private final AtomicLong lastFailureTime = new AtomicLong(0);
        private final int failureThreshold;
        private final int successThreshold;
        private final long waitDuration;

        CircuitState(CircuitBreaker cb) {
            this.failureThreshold = cb.failureThreshold();
            this.successThreshold = cb.successThreshold();
            this.waitDuration = cb.waitDuration();
        }

        boolean isOpen() { return state == State.OPEN; }
        
        boolean shouldAttemptReset() {
            return System.currentTimeMillis() - lastFailureTime.get() >= waitDuration;
        }
        
        boolean shouldTrip() {
            return failureCount.get() >= failureThreshold;
        }

        void recordSuccess() {
            failureCount.set(0);
            if (state == State.HALF_OPEN) {
                if (successCount.incrementAndGet() >= successThreshold) {
                    toClosed();
                }
            }
        }

        void recordFailure() {
            failureCount.incrementAndGet();
            lastFailureTime.set(System.currentTimeMillis());
            successCount.set(0);
        }

        void toOpen() { state = State.OPEN; }
        void toHalfOpen() { state = State.HALF_OPEN; successCount.set(0); }
        void toClosed() { state = State.CLOSED; failureCount.set(0); }
    }

    public static class CircuitOpenException extends RuntimeException {
        public CircuitOpenException(String message) { super(message); }
    }
}
