package io.github.yasmramos.veld.aop.proxy;

import io.github.yasmramos.veld.aop.InterceptorRegistry;
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
    void testCreateProxyFromClass() {
        // Without advices, should return new instance without proxy
        SampleService result = ProxyFactory.getInstance().createProxy(SampleService.class);
        assertNotNull(result);
        assertEquals("Hello World", result.greet("World"));
    }

    @Test
    void testServiceMethods() {
        SampleService service = new SampleService();
        assertEquals("Hello World", service.greet("World"));
        assertEquals(5, service.add(2, 3));
    }

    public static class SampleService {
        public SampleService() {}
        
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
