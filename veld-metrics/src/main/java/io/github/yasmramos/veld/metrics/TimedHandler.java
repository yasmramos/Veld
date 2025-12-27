package io.github.yasmramos.veld.metrics;

import io.github.yasmramos.veld.annotation.Timed;
import io.github.yasmramos.veld.aop.InvocationContext;
import io.github.yasmramos.veld.aop.MethodInterceptor;

public class TimedHandler implements MethodInterceptor {
    @Override
    public Object invoke(InvocationContext ctx) throws Throwable {
        Timed timed = ctx.getMethod().getAnnotation(Timed.class);
        if (timed == null) return ctx.proceed();
        String name = timed.value().isEmpty() ? ctx.getMethod().getDeclaringClass().getSimpleName() + "." + ctx.getMethod().getName() : timed.value();
        long start = System.currentTimeMillis();
        try { return ctx.proceed(); } finally { MetricsRegistry.recordTime(name, System.currentTimeMillis() - start); }
    }
}
