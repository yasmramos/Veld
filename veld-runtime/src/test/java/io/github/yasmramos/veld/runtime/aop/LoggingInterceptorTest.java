/*
 * Copyright 2025 Veld Framework
 */
package io.github.yasmramos.veld.runtime.aop;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class LoggingInterceptorTest {

    @Test
    void instance_isSingleton() {
        assertSame(LoggingInterceptor.INSTANCE, LoggingInterceptor.INSTANCE);
    }

    @Test
    void invoke_executesAndReturnsResult() throws Throwable {
        MethodInvocation invocation = new MethodInvocation(
            "testMethod", String.class, () -> "hello");
        
        Object result = LoggingInterceptor.INSTANCE.invoke(invocation);
        
        assertEquals("hello", result);
    }

    @Test
    void invoke_handlesNullResult() throws Throwable {
        MethodInvocation invocation = new MethodInvocation(
            "voidMethod", Void.class, () -> null);
        
        Object result = LoggingInterceptor.INSTANCE.invoke(invocation);
        
        assertNull(result);
    }

    @Test
    void invoke_measuresTime() throws Throwable {
        MethodInvocation invocation = new MethodInvocation(
            "slowMethod", Object.class, () -> {
                try { Thread.sleep(10); } catch (InterruptedException e) {}
                return "done";
            });
        
        Object result = LoggingInterceptor.INSTANCE.invoke(invocation);
        
        assertEquals("done", result);
    }
}
