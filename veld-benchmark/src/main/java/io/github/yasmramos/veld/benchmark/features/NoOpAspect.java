package io.github.yasmramos.veld.benchmark.features;

import io.github.yasmramos.veld.aop.InvocationContext;
import io.github.yasmramos.veld.annotation.Around;
import io.github.yasmramos.veld.annotation.Aspect;

@Aspect
public class NoOpAspect {
    @Around("execution(* io.github.yasmramos.veld.benchmark.features.AopTargetBean.doWork())")
    public void aroundDoWork(InvocationContext ctx) throws Throwable {
        // Empty advice - measures the overhead of AOP mechanism itself
        ctx.proceed();
    }
}
