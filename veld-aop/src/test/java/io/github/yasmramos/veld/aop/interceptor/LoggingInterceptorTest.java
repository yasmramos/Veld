package io.github.yasmramos.veld.aop.interceptor;

import io.github.yasmramos.veld.aop.InvocationContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

class LoggingInterceptorTest {

    private LoggingInterceptor interceptor;
    private ByteArrayOutputStream outputStream;
    private PrintStream originalOut;

    @BeforeEach
    void setUp() {
        interceptor = new LoggingInterceptor();
        outputStream = new ByteArrayOutputStream();
        originalOut = System.out;
        System.setOut(new PrintStream(outputStream));
    }

    void tearDown() {
        System.setOut(originalOut);
    }

    @Test
    void testLogMethodCallSuccess() throws Throwable {
        TestInvocationContext ctx = new TestInvocationContext(
            this, "testMethod", new Object[]{"arg1"}, "result"
        );

        Object result = interceptor.logMethodCall(ctx);

        assertEquals("result", result);
        String output = outputStream.toString();
        assertTrue(output.contains("Entering"));
        assertTrue(output.contains("Exiting"));
        tearDown();
    }

    @Test
    void testLogMethodCallWithException() throws Throwable {
        TestInvocationContext ctx = new TestInvocationContext(
            this, "testMethod", new Object[]{}, null
        );
        ctx.throwException = new RuntimeException("Test error");

        assertThrows(RuntimeException.class, () -> interceptor.logMethodCall(ctx));
        
        String output = outputStream.toString();
        assertTrue(output.contains("threw exception"));
        tearDown();
    }

    @Test
    void testProceedIsCalled() throws Throwable {
        TestInvocationContext ctx = new TestInvocationContext(
            this, "testMethod", new Object[]{}, "value"
        );

        interceptor.logMethodCall(ctx);
        
        assertTrue(ctx.proceedCalled);
        tearDown();
    }

    static class TestInvocationContext implements InvocationContext {
        Object target;
        String methodName;
        Object[] params;
        Object returnValue;
        boolean proceedCalled = false;
        Throwable throwException = null;
        Map<String, Object> contextData = new HashMap<>();

        TestInvocationContext(Object target, String methodName, Object[] params, Object returnValue) {
            this.target = target;
            this.methodName = methodName;
            this.params = params;
            this.returnValue = returnValue;
        }

        @Override
        public Object proceed() throws Throwable {
            proceedCalled = true;
            if (throwException != null) throw throwException;
            return returnValue;
        }

        @Override public Object getTarget() { return target; }
        @Override public Method getMethod() {
            try {
                return Object.class.getMethod("toString");
            } catch (NoSuchMethodException e) {
                return null;
            }
        }
        @Override public String getMethodName() { return methodName; }
        @Override public Object[] getArgs() { return params; }
        @Override public Object[] getParameters() { return params; }
        @Override public void setParameters(Object[] params) { this.params = params; }
        @Override public Map<String, Object> getContextData() { return contextData; }
        @Override public Object getTimer() { return null; }
        @Override public Object getInterceptor() { return null; }
        @Override public Class<?> getDeclaringClass() { return target.getClass(); }
        @Override public String getSignature() { return methodName + "()"; }
        @Override public String toShortString() { return methodName; }
        @Override public String toLongString() { return target.getClass().getName() + "." + methodName; }
    }
}
