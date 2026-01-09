package io.github.yasmramos.veld.metrics;

import io.github.yasmramos.veld.annotation.Counted;
import io.github.yasmramos.veld.aop.InvocationContext;
import io.github.yasmramos.veld.aop.MethodInterceptor;

public class CountedHandler implements MethodInterceptor {
    @Override
    public Object invoke(InvocationContext ctx) throws Throwable {
        if (!ctx.hasAnnotation(Counted.class)) return ctx.proceed();
        Counted counted = ctx.getAnnotation(Counted.class);
        String name = counted.value().isEmpty() ? getSimpleClassName(ctx.getDeclaringClassName()) + "." + ctx.getMethodName() : counted.value();
        MetricsRegistry.incrementCounter(name);
        try { return ctx.proceed(); } catch (Throwable t) { if (counted.recordFailuresOnly()) MetricsRegistry.incrementCounter(name + ".failures"); throw t; }
    }

    private String getSimpleClassName(String fullClassName) {
        int lastDot = fullClassName.lastIndexOf('.');
        return lastDot > 0 ? fullClassName.substring(lastDot + 1) : fullClassName;
    }
}
