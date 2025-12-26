package io.github.yasmramos.veld.aop.proxy;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for AopProxy static methods.
 */
class AopProxyStaticMethodsTest {

    // Mock implementation of AopProxy for testing
    static class MockAopProxy implements AopProxy {
        private final Object target;
        
        MockAopProxy(Object target) {
            this.target = target;
        }
        
        @Override
        public Object getTargetObject() {
            return target;
        }
        
        @Override
        public Class<?> getTargetClass() {
            return target.getClass();
        }
    }

    @Test
    void testIsProxyReturnsTrueForProxy() {
        AopProxy proxy = new MockAopProxy("target");
        
        assertTrue(AopProxy.isProxy(proxy));
    }

    @Test
    void testIsProxyReturnsFalseForNonProxy() {
        String notAProxy = "regular object";
        
        assertFalse(AopProxy.isProxy(notAProxy));
    }

    @Test
    void testIsProxyReturnsFalseForNull() {
        assertFalse(AopProxy.isProxy(null));
    }

    @Test
    void testUnwrapReturnsTargetForProxy() {
        String target = "target object";
        AopProxy proxy = new MockAopProxy(target);
        
        Object result = AopProxy.unwrap(proxy);
        
        assertSame(target, result);
    }

    @Test
    void testUnwrapReturnsSameObjectForNonProxy() {
        String notAProxy = "regular object";
        
        Object result = AopProxy.unwrap(notAProxy);
        
        assertSame(notAProxy, result);
    }

    @Test
    void testUnwrapReturnsNullForNull() {
        Object result = AopProxy.unwrap(null);
        
        assertNull(result);
    }

    @Test
    void testMockAopProxyGetTargetObject() {
        Object target = new Object();
        MockAopProxy proxy = new MockAopProxy(target);
        
        assertSame(target, proxy.getTargetObject());
    }

    @Test
    void testMockAopProxyGetTargetClass() {
        String target = "test";
        MockAopProxy proxy = new MockAopProxy(target);
        
        assertEquals(String.class, proxy.getTargetClass());
    }
}
