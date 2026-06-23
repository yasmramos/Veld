package io.github.yasmramos.veld.resilience;

import io.github.yasmramos.veld.annotation.Bulkhead;
import io.github.yasmramos.veld.aop.InvocationContext;
import io.github.yasmramos.veld.aop.MethodInterceptor;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class BulkheadHandler implements MethodInterceptor {
    private static final ConcurrentHashMap<String, Semaphore> bulkheads = new ConcurrentHashMap<>();

    @Override
    public Object invoke(InvocationContext ctx) throws Throwable {
        if (!ctx.hasAnnotation(Bulkhead.class)) return ctx.proceed();
        Bulkhead bh = ctx.getAnnotation(Bulkhead.class);
        String key = bh.name().isEmpty() ? ctx.getDeclaringClassName() + "." + ctx.getMethodName() : bh.name();
        Semaphore semaphore = bulkheads.computeIfAbsent(key, k -> new Semaphore(bh.maxConcurrent()));
        boolean acquired = semaphore.tryAcquire(bh.maxWait(), TimeUnit.MILLISECONDS);
        if (!acquired) throw new BulkheadFullException("Bulkhead " + key + " is full");
        try { return ctx.proceed(); } finally { semaphore.release(); }
    }

    public static class BulkheadFullException extends RuntimeException {
        public BulkheadFullException(String message) { super(message); }
    }
}
