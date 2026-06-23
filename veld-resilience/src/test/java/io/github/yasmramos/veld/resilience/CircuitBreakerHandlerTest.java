package io.github.yasmramos.veld.resilience;

import io.github.yasmramos.veld.annotation.CircuitBreaker;
import io.github.yasmramos.veld.aop.InvocationContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CircuitBreakerHandlerTest {

    private CircuitBreakerHandler handler;
    private InvocationContext context;
    private CircuitBreaker annotation;

    @BeforeEach
    void setUp() {
        handler = new CircuitBreakerHandler();
        context = mock(InvocationContext.class);
        annotation = mock(CircuitBreaker.class);
        
        when(annotation.failureThreshold()).thenReturn(2);
        when(annotation.successThreshold()).thenReturn(1);
        when(annotation.resetTimeout()).thenReturn(500L);
        when(annotation.fallbackMethod()).thenReturn("");
        
        when(context.hasAnnotation(CircuitBreaker.class)).thenReturn(true);
        when(context.getAnnotation(CircuitBreaker.class)).thenReturn(annotation);
        when(context.getDeclaringClassName()).thenReturn("TestClass");
        when(context.getMethodName()).thenReturn("testMethod");
        when(context.getParameterTypes()).thenReturn(new Class[]{});
        when(context.getParameters()).thenReturn(new Object[]{});
    }

    @Test
    void shouldOpenCircuitAfterFailures() throws Throwable {
        when(annotation.name()).thenReturn("circuit-open-test-" + System.nanoTime());
        when(context.proceed()).thenThrow(new RuntimeException("Fail"));

        // First failure
        try { handler.invoke(context); } catch (RuntimeException e) {}
        // Second failure - should trip
        try { handler.invoke(context); } catch (RuntimeException e) {}

        // Third call - should be blocked by OPEN circuit
        assertThrows(CircuitBreakerHandler.CircuitOpenException.class, () -> handler.invoke(context));
        
        verify(context, times(2)).proceed();
    }

    @Test
    void shouldResetAfterSuccessInHalfOpen() throws Throwable {
        when(annotation.name()).thenReturn("circuit-reset-test-" + System.nanoTime());
        
        // Setup failures to trip the circuit
        when(context.proceed()).thenThrow(new RuntimeException("Fail"));
        
        // Trip circuit (2 failures)
        try { handler.invoke(context); } catch (RuntimeException e) {}
        try { handler.invoke(context); } catch (RuntimeException e) {}
        
        // Wait for reset timeout (500ms)
        Thread.sleep(600);
        
        // Half-open: success
        // We must change the mock behavior BEFORE the call
        reset(context);
        when(context.hasAnnotation(CircuitBreaker.class)).thenReturn(true);
        when(context.getAnnotation(CircuitBreaker.class)).thenReturn(annotation);
        when(context.getDeclaringClassName()).thenReturn("TestClass");
        when(context.getMethodName()).thenReturn("testMethod");
        when(context.getParameterTypes()).thenReturn(new Class[]{});
        when(context.getParameters()).thenReturn(new Object[]{});
        when(context.proceed()).thenReturn("success");
        
        Object result = handler.invoke(context);
        assertEquals("success", result);
        
        // Should be CLOSED now - another call should proceed
        when(context.proceed()).thenReturn("success2");
        Object result2 = handler.invoke(context);
        assertEquals("success2", result2);
    }
}
