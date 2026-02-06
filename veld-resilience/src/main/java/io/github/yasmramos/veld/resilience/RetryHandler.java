package io.github.yasmramos.veld.resilience;

import io.github.yasmramos.veld.annotation.Retry;
import io.github.yasmramos.veld.aop.InvocationContext;
import io.github.yasmramos.veld.aop.MethodInterceptor;

/**
 * Implementation of the Retry pattern with exponential backoff support.
 * 
 * @author Veld Framework Team
 * @since 1.1.0
 */
public class RetryHandler implements MethodInterceptor {

    @Override
    public Object invoke(InvocationContext ctx) throws Throwable {
        if (!ctx.hasAnnotation(Retry.class)) {
            return ctx.proceed();
        }

        Retry retry = ctx.getAnnotation(Retry.class);
        int maxAttempts = retry.maxAttempts();
        long delay = retry.delay();
        double multiplier = retry.multiplier();
        long maxDelay = retry.maxDelay();

        int attempt = 0;
        Throwable lastThrowable;

        do {
            attempt++;
            try {
                return ctx.proceed();
            } catch (Throwable t) {
                lastThrowable = t;
                
                if (attempt >= maxAttempts || !shouldRetry(retry, t)) {
                    throw t;
                }

                // Calculate backoff
                if (delay > 0) {
                    Thread.sleep(delay);
                    delay = (long) (delay * multiplier);
                    if (delay > maxDelay) {
                        delay = maxDelay;
                    }
                }
            }
        } while (attempt < maxAttempts);

        throw lastThrowable;
    }

    private boolean shouldRetry(Retry retry, Throwable t) {
        // Check excluded exceptions first
        for (Class<? extends Throwable> excludeClass : retry.exclude()) {
            if (excludeClass.isInstance(t)) {
                return false;
            }
        }

        // If include is empty, retry on all exceptions
        if (retry.include().length == 0) {
            return true;
        }

        // Check included exceptions
        for (Class<? extends Throwable> includeClass : retry.include()) {
            if (includeClass.isInstance(t)) {
                return true;
            }
        }

        return false;
    }
}
