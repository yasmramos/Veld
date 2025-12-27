package io.github.yasmramos.veld.resilience;

import io.github.yasmramos.veld.annotation.Bulkhead;
import io.github.yasmramos.veld.aop.AspectHandler;
import io.github.yasmramos.veld.aop.MethodInvocation;

import java.lang.annotation.Annotation;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * Bulkhead pattern - limits concurrent executions to prevent resource exhaustion.
 */
public class BulkheadHandler implements AspectHandler {

    private static final ConcurrentHashMap<String, Semaphore> bulkheads = new ConcurrentHashMap<>();

    @Override
    public Class<? extends Annotation> getAnnotationType() {
        return Bulkhead.class;
    }

    @Override
    public Object handle(MethodInvocation invocation) throws Throwable {
        Bulkhead bh = invocation.getMethod().getAnnotation(Bulkhead.class);
        String key = bh.name().isEmpty() ? invocation.getMethod().toString() : bh.name();
        
        Semaphore semaphore = bulkheads.computeIfAbsent(key, k -> new Semaphore(bh.maxConcurrentCalls()));
        
        boolean acquired = semaphore.tryAcquire(bh.maxWaitDuration(), TimeUnit.MILLISECONDS);
        if (!acquired) {
            throw new BulkheadFullException("Bulkhead " + key + " is full");
        }
        
        try {
            return invocation.proceed();
        } finally {
            semaphore.release();
        }
    }

    public static class BulkheadFullException extends RuntimeException {
        public BulkheadFullException(String message) { super(message); }
    }
}
