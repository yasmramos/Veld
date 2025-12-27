package io.github.yasmramos.veld.resilience;

import io.github.yasmramos.veld.annotation.Timeout;
import io.github.yasmramos.veld.aop.AspectHandler;
import io.github.yasmramos.veld.aop.MethodInvocation;

import java.lang.annotation.Annotation;
import java.util.concurrent.*;

/**
 * Timeout handler - cancels execution if it exceeds the time limit.
 */
public class TimeoutHandler implements AspectHandler {

    private static final ExecutorService executor = Executors.newCachedThreadPool();

    @Override
    public Class<? extends Annotation> getAnnotationType() {
        return Timeout.class;
    }

    @Override
    public Object handle(MethodInvocation invocation) throws Throwable {
        Timeout timeout = invocation.getMethod().getAnnotation(Timeout.class);
        
        Future<Object> future = executor.submit(() -> {
            try {
                return invocation.proceed();
            } catch (Throwable t) {
                throw new ExecutionException(t);
            }
        });
        
        try {
            return future.get(timeout.value(), timeout.unit());
        } catch (TimeoutException e) {
            future.cancel(true);
            throw new TimeoutExceededException("Method execution exceeded timeout of " + timeout.value() + " " + timeout.unit());
        } catch (ExecutionException e) {
            throw e.getCause();
        }
    }

    public static class TimeoutExceededException extends RuntimeException {
        public TimeoutExceededException(String message) { super(message); }
    }
}
