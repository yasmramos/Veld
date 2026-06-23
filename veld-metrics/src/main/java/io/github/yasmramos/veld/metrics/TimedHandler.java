package io.github.yasmramos.veld.metrics;

import io.github.yasmramos.veld.annotation.Timed;
import io.github.yasmramos.veld.aop.InvocationContext;
import io.github.yasmramos.veld.aop.MethodInterceptor;

public class TimedHandler implements MethodInterceptor {
    @Override
    public Object invoke(InvocationContext ctx) throws Throwable {
        if (!ctx.hasAnnotation(Timed.class)) return ctx.proceed();
        Timed timed = ctx.getAnnotation(Timed.class);
        String name = timed.value().isEmpty() ? getSimpleClassName(ctx.getDeclaringClassName()) + "." + ctx.getMethodName() : timed.value();
        long start = System.currentTimeMillis();
        try { return ctx.proceed(); } finally { MetricsRegistry.recordTime(name, System.currentTimeMillis() - start); }
    }

    private String getSimpleClassName(String fullClassName) {
        int lastDot = fullClassName.lastIndexOf('.');
        return lastDot > 0 ? fullClassName.substring(lastDot + 1) : fullClassName;
    }
}
