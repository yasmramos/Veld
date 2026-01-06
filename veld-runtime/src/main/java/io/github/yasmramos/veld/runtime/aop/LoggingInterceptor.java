package io.github.yasmramos.veld.runtime.aop;

/**
 * Example interceptor that logs method entry/exit with timing.
 */
public final class LoggingInterceptor implements MethodInterceptor {
    
    public static final LoggingInterceptor INSTANCE = new LoggingInterceptor();
    
    private LoggingInterceptor() {}
    
    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        long start = System.nanoTime();
        Object result = invocation.proceed();
        long elapsed = System.nanoTime() - start;
        
        System.out.printf("[AOP] %s.%s completed in %dns%n",
            invocation.targetClass().getSimpleName(),
            invocation.methodName(),
            elapsed);
        
        return result;
    }
}
