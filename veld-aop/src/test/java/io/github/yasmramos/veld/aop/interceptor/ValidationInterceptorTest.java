package io.github.yasmramos.veld.aop.interceptor;

import io.github.yasmramos.veld.aop.InvocationContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

class ValidationInterceptorTest {

    private ValidationInterceptor interceptor;

    @BeforeEach
    void setUp() {
        interceptor = new ValidationInterceptor();
    }

    @Test
    void testValidArgumentsPass() throws Throwable {
        TestContext ctx = new TestContext(new Object[]{"valid"}, "success");
        
        Object result = interceptor.validateArguments(ctx);
        
        assertEquals("success", result);
        assertTrue(ctx.proceedCalled);
    }

    @Test
    void testNullArgumentFails() throws Throwable {
        TestContext ctx = new TestContext(new Object[]{null}, "success");
        
        assertThrows(IllegalArgumentException.class, () -> interceptor.validateArguments(ctx));
    }

    @Test
    void testProceedCalledOnSuccess() throws Throwable {
        TestContext ctx = new TestContext(new Object[]{"test"}, "result");
        
        interceptor.validateArguments(ctx);
        
        assertTrue(ctx.proceedCalled);
    }

    static class TestContext implements InvocationContext {
        Object[] params;
        Object returnValue;
        boolean proceedCalled = false;
        Map<String, Object> contextData = new HashMap<>();

        TestContext(Object[] params, Object returnValue) {
            this.params = params;
            this.returnValue = returnValue;
        }

        @Override
        public Object proceed() throws Throwable {
            proceedCalled = true;
            return returnValue;
        }

        @Override
        public Method getMethod() {
            try {
                return TestService.class.getMethod("process", String.class);
            } catch (NoSuchMethodException e) {
                return null;
            }
        }

        @Override public Object getTarget() { return new TestService(); }
        @Override public String getMethodName() { return "process"; }
        @Override public Object[] getArgs() { return params; }
        @Override public Object[] getParameters() { return params; }
        @Override public void setParameters(Object[] params) { this.params = params; }
        @Override public Map<String, Object> getContextData() { return contextData; }
        @Override public Object getTimer() { return null; }
        @Override public Object getInterceptor() { return null; }
        @Override public Class<?> getDeclaringClass() { return TestService.class; }
        @Override public String getSignature() { return "process(String)"; }
        @Override public String toShortString() { return "process"; }
        @Override public String toLongString() { return "TestService.process(String)"; }
    }

    public static class TestService {
        public String process(String input) {
            return input;
        }
    }
}
