package io.github.yasmramos.aop.proxy;

import io.github.yasmramos.aop.InterceptorRegistry;
import io.github.yasmramos.aop.MethodInterceptor;
import io.github.yasmramos.aop.MethodInvocation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ProxyFactoryTest {

    @BeforeEach
    void setUp() {
        InterceptorRegistry.getInstance().clear();
    }

    @Test
    void testGetInstance() {
        ProxyFactory factory = ProxyFactory.getInstance();
        assertNotNull(factory);
        assertSame(factory, ProxyFactory.getInstance());
    }

    @Test
    void testCreateProxyWithNoAdvices() {
        SampleService target = new SampleService();
        SampleService result = ProxyFactory.getInstance().createProxy(target);
        
        // No advices registered, should return original
        assertSame(target, result);
    }

    @Test
    void testCreateProxyWithAdvice() {
        InterceptorRegistry.getInstance().registerInterceptor(
            SampleService.class,
            "greet",
            new MethodInterceptor() {
                @Override
                public Object invoke(MethodInvocation invocation) throws Throwable {
                    return "Intercepted: " + invocation.proceed();
                }
            }
        );

        SampleService target = new SampleService();
        SampleService proxy = ProxyFactory.getInstance().createProxy(target);
        
        assertNotSame(target, proxy);
        assertTrue(proxy instanceof AopProxy);
        assertEquals("Intercepted: Hello World", proxy.greet("World"));
    }

    @Test
    void testProxyImplementsAopProxy() {
        InterceptorRegistry.getInstance().registerInterceptor(
            SampleService.class,
            "greet",
            invocation -> invocation.proceed()
        );

        SampleService target = new SampleService();
        SampleService proxy = ProxyFactory.getInstance().createProxy(target);
        
        assertTrue(proxy instanceof AopProxy);
        AopProxy aopProxy = (AopProxy) proxy;
        assertSame(target, aopProxy.getTargetObject());
        assertEquals(SampleService.class, aopProxy.getTargetClass());
    }

    @Test
    void testProxyWithPrimitiveReturn() {
        InterceptorRegistry.getInstance().registerInterceptor(
            SampleService.class,
            "add",
            invocation -> (int) invocation.proceed() * 2
        );

        SampleService target = new SampleService();
        SampleService proxy = ProxyFactory.getInstance().createProxy(target);
        
        assertEquals(10, proxy.add(2, 3)); // (2+3) * 2
    }

    @Test
    void testProxyWithVoidMethod() {
        StringBuilder log = new StringBuilder();
        
        InterceptorRegistry.getInstance().registerInterceptor(
            SampleService.class,
            "doSomething",
            invocation -> {
                log.append("before:");
                Object result = invocation.proceed();
                log.append("after");
                return result;
            }
        );

        SampleService target = new SampleService();
        SampleService proxy = ProxyFactory.getInstance().createProxy(target);
        
        proxy.doSomething();
        assertEquals("before:after", log.toString());
    }

    public static class SampleService {
        public String greet(String name) {
            return "Hello " + name;
        }

        public int add(int a, int b) {
            return a + b;
        }

        public void doSomething() {
        }
    }
}
