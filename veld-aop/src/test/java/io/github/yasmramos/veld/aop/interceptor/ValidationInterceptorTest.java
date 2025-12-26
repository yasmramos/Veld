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

    @Test
    void testEmptyArgsPass() throws Throwable {
        TestContext ctx = new TestContext(new Object[]{}, "success");
        
        Object result = interceptor.validateArguments(ctx);
        
        assertEquals("success", result);
    }

    @Test
    void testStringArgPass() throws Throwable {
        TestContext ctx = new TestContext(new Object[]{"valid-string"}, "success");
        
        Object result = interceptor.validateArguments(ctx);
        
        assertEquals("success", result);
        assertTrue(ctx.proceedCalled);
    }

    @Test
    void testValidationWithNonStrictMode() throws Throwable {
        TestContextNonStrict ctx = new TestContextNonStrict(new Object[]{null}, "result");
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        java.io.PrintStream originalOut = System.out;
        System.setOut(new java.io.PrintStream(out));
        
        Object result = interceptor.validateArguments(ctx);
        
        System.setOut(originalOut);
        assertEquals("result", result); // Should not throw, just warn
        assertTrue(out.toString().contains("WARNING"));
    }

    @Test
    void testExceptionMessageContainsMethodInfo() throws Throwable {
        TestContext ctx = new TestContext(new Object[]{null}, "success");
        
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, 
            () -> interceptor.validateArguments(ctx));
        assertTrue(ex.getMessage().contains("Validation failed"));
    }

    static class TestContextNonStrict implements InvocationContext {
        Object[] params;
        Object returnValue;
        Map<String, Object> contextData = new HashMap<>();

        TestContextNonStrict(Object[] params, Object returnValue) {
            this.params = params;
            this.returnValue = returnValue;
        }

        @Override public Object proceed() throws Throwable { return returnValue; }
        @Override public Method getMethod() {
            try { return NonStrictService.class.getMethod("process", String.class); } 
            catch (NoSuchMethodException e) { return null; }
        }
        @Override public Object getTarget() { return new NonStrictService(); }
        @Override public String getMethodName() { return "process"; }
        @Override public Object[] getArgs() { return params; }
        @Override public Object[] getParameters() { return params; }
        @Override public void setParameters(Object[] params) { this.params = params; }
        @Override public Map<String, Object> getContextData() { return contextData; }
        @Override public Object getTimer() { return null; }
        @Override public Object getInterceptor() { return null; }
        @Override public Class<?> getDeclaringClass() { return NonStrictService.class; }
        @Override public String getSignature() { return "process(String)"; }
        @Override public String toShortString() { return "process"; }
        @Override public String toLongString() { return "NonStrictService.process(String)"; }
    }

    @Validated(strict = false)
    public static class NonStrictService {
        public String process(String input) { return input; }
    }
}
