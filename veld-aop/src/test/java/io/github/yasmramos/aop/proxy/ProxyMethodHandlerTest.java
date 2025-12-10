package io.github.yasmramos.aop.proxy;

import io.github.yasmramos.aop.InterceptorRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ProxyMethodHandlerTest {

    @BeforeEach
    void setUp() {
        InterceptorRegistry.getInstance().clear();
    }

    @Test
    void testInvokeSimpleMethod() throws Throwable {
        TestService target = new TestService();
        
        Object result = ProxyMethodHandler.invoke(
            target, "getMessage", "()Ljava/lang/String;", new Object[]{}
        );
        
        assertEquals("Hello", result);
    }

    @Test
    void testInvokeWithArguments() throws Throwable {
        TestService target = new TestService();
        
        Object result = ProxyMethodHandler.invoke(
            target, "greet", "(Ljava/lang/String;)Ljava/lang/String;", new Object[]{"World"}
        );
        
        assertEquals("Hello World", result);
    }

    @Test
    void testInvokeWithPrimitiveArgs() throws Throwable {
        TestService target = new TestService();
        
        Object result = ProxyMethodHandler.invoke(
            target, "add", "(II)I", new Object[]{3, 4}
        );
        
        assertEquals(7, result);
    }

    @Test
    void testInvokeVoidMethod() throws Throwable {
        TestService target = new TestService();
        
        Object result = ProxyMethodHandler.invoke(
            target, "doNothing", "()V", new Object[]{}
        );
        
        assertNull(result);
    }

    @Test
    void testInvokeWithBooleanReturn() throws Throwable {
        TestService target = new TestService();
        
        Object result = ProxyMethodHandler.invoke(
            target, "isEnabled", "()Z", new Object[]{}
        );
        
        assertEquals(true, result);
    }

    public static class TestService {
        public String getMessage() {
            return "Hello";
        }

        public String greet(String name) {
            return "Hello " + name;
        }

        public int add(int a, int b) {
            return a + b;
        }

        public void doNothing() {
        }

        public boolean isEnabled() {
            return true;
        }
    }
}
