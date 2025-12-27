package io.github.yasmramos.veld.metrics;

import io.github.yasmramos.veld.annotation.Counted;
import io.github.yasmramos.veld.aop.InvocationContext;
import io.github.yasmramos.veld.aop.MethodInterceptor;

public class CountedHandler implements MethodInterceptor {
    @Override
    public Object invoke(InvocationContext ctx) throws Throwable {
        Counted counted = ctx.getMethod().getAnnotation(Counted.class);
        if (counted == null) return ctx.proceed();
        String name = counted.value().isEmpty() ? ctx.getMethod().getDeclaringClass().getSimpleName() + "." + ctx.getMethod().getName() : counted.value();
        MetricsRegistry.incrementCounter(name);
        try { return ctx.proceed(); } catch (Throwable t) { if (counted.recordFailuresOnly()) MetricsRegistry.incrementCounter(name + ".failures"); throw t; }
    }
}
