package io.github.yasmramos.veld.resilience;

import io.github.yasmramos.veld.annotation.Timeout;
import io.github.yasmramos.veld.aop.InvocationContext;
import io.github.yasmramos.veld.aop.MethodInterceptor;

import java.util.concurrent.*;

public class TimeoutHandler implements MethodInterceptor {
    private static final ExecutorService executor = Executors.newCachedThreadPool();

    @Override
    public Object invoke(InvocationContext ctx) throws Throwable {
        if (!ctx.hasAnnotation(Timeout.class)) return ctx.proceed();
        Timeout timeout = ctx.getAnnotation(Timeout.class);
        Future<Object> future = executor.submit(() -> {
            try { return ctx.proceed(); } 
            catch (Throwable t) { throw new RuntimeException(t); }
        });
        try {
            return future.get(timeout.value(), timeout.unit());
        } catch (TimeoutException e) {
            future.cancel(true);
            throw new TimeoutExceededException("Timeout exceeded");
        } catch (ExecutionException e) {
            throw e.getCause();
        }
    }

    public static class TimeoutExceededException extends RuntimeException {
        public TimeoutExceededException(String message) { super(message); }
    }
}
