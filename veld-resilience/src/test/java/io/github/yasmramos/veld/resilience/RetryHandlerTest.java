package io.github.yasmramos.veld.resilience;

import io.github.yasmramos.veld.annotation.Retry;
import io.github.yasmramos.veld.aop.InvocationContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RetryHandlerTest {

    private RetryHandler handler;
    private InvocationContext context;

    @BeforeEach
    void setUp() {
        handler = new RetryHandler();
        context = mock(InvocationContext.class);
    }

    @Test
    void shouldProceedWithoutAnnotation() throws Throwable {
        when(context.hasAnnotation(Retry.class)).thenReturn(false);
        when(context.proceed()).thenReturn("success");

        Object result = handler.invoke(context);

        assertEquals("success", result);
        verify(context, times(1)).proceed();
    }

    @Test
    void shouldRetryOnFailure() throws Throwable {
        Retry retry = mock(Retry.class);
        when(retry.maxAttempts()).thenReturn(3);
        when(retry.delay()).thenReturn(0L);
        when(retry.include()).thenReturn(new Class[]{});
        when(retry.exclude()).thenReturn(new Class[]{});

        when(context.hasAnnotation(Retry.class)).thenReturn(true);
        when(context.getAnnotation(Retry.class)).thenReturn(retry);
        
        AtomicInteger calls = new AtomicInteger(0);
        when(context.proceed()).thenAnswer(inv -> {
            if (calls.incrementAndGet() < 3) {
                throw new RuntimeException("Fail");
            }
            return "success";
        });

        Object result = handler.invoke(context);

        assertEquals("success", result);
        assertEquals(3, calls.get());
    }

    @Test
    void shouldThrowAfterMaxAttempts() throws Throwable {
        Retry retry = mock(Retry.class);
        when(retry.maxAttempts()).thenReturn(2);
        when(retry.delay()).thenReturn(0L);
        when(retry.include()).thenReturn(new Class[]{});
        when(retry.exclude()).thenReturn(new Class[]{});

        when(context.hasAnnotation(Retry.class)).thenReturn(true);
        when(context.getAnnotation(Retry.class)).thenReturn(retry);
        when(context.proceed()).thenThrow(new RuntimeException("Permanent Fail"));

        assertThrows(RuntimeException.class, () -> handler.invoke(context));
        verify(context, times(2)).proceed();
    }

    @Test
    void shouldNotRetryOnExcludedException() throws Throwable {
        Retry retry = mock(Retry.class);
        when(retry.maxAttempts()).thenReturn(3);
        when(retry.exclude()).thenReturn(new Class[]{IOException.class});
        when(retry.include()).thenReturn(new Class[]{});

        when(context.hasAnnotation(Retry.class)).thenReturn(true);
        when(context.getAnnotation(Retry.class)).thenReturn(retry);
        when(context.proceed()).thenThrow(new IOException("Excluded"));

        assertThrows(IOException.class, () -> handler.invoke(context));
        verify(context, times(1)).proceed();
    }
}
