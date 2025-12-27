package io.github.yasmramos.veld.metrics;

import io.github.yasmramos.veld.annotation.Timed;
import io.github.yasmramos.veld.aop.AspectHandler;
import io.github.yasmramos.veld.aop.MethodInvocation;

import java.lang.annotation.Annotation;

/**
 * Records execution time of methods.
 */
public class TimedHandler implements AspectHandler {

    @Override
    public Class<? extends Annotation> getAnnotationType() {
        return Timed.class;
    }

    @Override
    public Object handle(MethodInvocation invocation) throws Throwable {
        Timed timed = invocation.getMethod().getAnnotation(Timed.class);
        String name = timed.value().isEmpty() 
            ? invocation.getMethod().getDeclaringClass().getSimpleName() + "." + invocation.getMethod().getName()
            : timed.value();
        
        long start = System.currentTimeMillis();
        try {
            return invocation.proceed();
        } finally {
            long duration = System.currentTimeMillis() - start;
            MetricsRegistry.recordTime(name, duration);
        }
    }
}
