package io.github.yasmramos.veld.metrics;

import io.github.yasmramos.veld.annotation.Counted;
import io.github.yasmramos.veld.aop.AspectHandler;
import io.github.yasmramos.veld.aop.MethodInvocation;

import java.lang.annotation.Annotation;

/**
 * Counts method invocations.
 */
public class CountedHandler implements AspectHandler {

    @Override
    public Class<? extends Annotation> getAnnotationType() {
        return Counted.class;
    }

    @Override
    public Object handle(MethodInvocation invocation) throws Throwable {
        Counted counted = invocation.getMethod().getAnnotation(Counted.class);
        String name = counted.value().isEmpty() 
            ? invocation.getMethod().getDeclaringClass().getSimpleName() + "." + invocation.getMethod().getName()
            : counted.value();
        
        MetricsRegistry.incrementCounter(name);
        
        try {
            return invocation.proceed();
        } catch (Throwable t) {
            if (counted.recordFailuresOnly()) {
                MetricsRegistry.incrementCounter(name + ".failures");
            }
            throw t;
        }
    }
}
