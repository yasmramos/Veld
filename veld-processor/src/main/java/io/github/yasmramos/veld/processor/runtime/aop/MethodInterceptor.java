package io.github.yasmramos.veld.processor.runtime.aop;

/**
 * Interceptor for method invocations.
 */
@FunctionalInterface
public interface MethodInterceptor {
    Object invoke(MethodInvocation invocation) throws Throwable;
}
