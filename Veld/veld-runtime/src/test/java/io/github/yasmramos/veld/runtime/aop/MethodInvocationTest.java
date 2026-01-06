/*
 * Copyright 2025 Veld Framework
 */
package io.github.yasmramos.veld.runtime.aop;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MethodInvocationTest {

    @Test
    void proceed_executesSupplier() {
        MethodInvocation invocation = new MethodInvocation(
            "testMethod", String.class, () -> "result");
        
        assertEquals("result", invocation.proceed());
    }

    @Test
    void methodName_returnsCorrectName() {
        MethodInvocation invocation = new MethodInvocation(
            "myMethod", Object.class, () -> null);
        
        assertEquals("myMethod", invocation.methodName());
    }

    @Test
    void targetClass_returnsCorrectClass() {
        MethodInvocation invocation = new MethodInvocation(
            "test", Integer.class, () -> 42);
        
        assertEquals(Integer.class, invocation.targetClass());
    }

    @Test
    void proceed_canReturnNull() {
        MethodInvocation invocation = new MethodInvocation(
            "voidMethod", Void.class, () -> null);
        
        assertNull(invocation.proceed());
    }
}
