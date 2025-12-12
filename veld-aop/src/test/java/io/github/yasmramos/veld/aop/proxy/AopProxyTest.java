package io.github.yasmramos.veld.aop.proxy;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AopProxyTest {

    @Test
    void testAopProxyInterface() {
        TestAopProxy proxy = new TestAopProxy("target", String.class);
        
        assertEquals("target", proxy.getTargetObject());
        assertEquals(String.class, proxy.getTargetClass());
    }

    @Test
    void testAopProxyWithNullTarget() {
        TestAopProxy proxy = new TestAopProxy(null, Object.class);
        
        assertNull(proxy.getTargetObject());
        assertEquals(Object.class, proxy.getTargetClass());
    }

    @Test
    void testAopProxyWithDifferentTypes() {
        Integer target = 42;
        TestAopProxy proxy = new TestAopProxy(target, Integer.class);
        
        assertEquals(42, proxy.getTargetObject());
        assertEquals(Integer.class, proxy.getTargetClass());
    }

    static class TestAopProxy implements AopProxy {
        private final Object target;
        private final Class<?> targetClass;

        TestAopProxy(Object target, Class<?> targetClass) {
            this.target = target;
            this.targetClass = targetClass;
        }

        @Override
        public Object getTargetObject() {
            return target;
        }

        @Override
        public Class<?> getTargetClass() {
            return targetClass;
        }
    }
}
