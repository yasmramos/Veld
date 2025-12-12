package io.github.yasmramos.veld.runtime;

import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for VeldException.
 */
@DisplayName("VeldException Tests")
class VeldExceptionTest {
    
    @Test
    @DisplayName("Should create with message")
    void shouldCreateWithMessage() {
        VeldException exception = new VeldException("Test message");
        
        assertEquals("Test message", exception.getMessage());
        assertNull(exception.getCause());
    }
    
    @Test
    @DisplayName("Should create with message and cause")
    void shouldCreateWithMessageAndCause() {
        RuntimeException cause = new RuntimeException("Original error");
        
        VeldException exception = new VeldException("Wrapper message", cause);
        
        assertEquals("Wrapper message", exception.getMessage());
        assertSame(cause, exception.getCause());
    }
    
    @Test
    @DisplayName("Should be RuntimeException")
    void shouldBeRuntimeException() {
        VeldException exception = new VeldException("Test");
        
        assertTrue(exception instanceof RuntimeException);
    }
    
    @Test
    @DisplayName("Should be throwable without declaration")
    void shouldBeThrowableWithoutDeclaration() {
        assertThrows(VeldException.class, () -> {
            throw new VeldException("Test error");
        });
    }
    
    @Test
    @DisplayName("Should preserve cause stack trace")
    void shouldPreserveCauseStackTrace() {
        IllegalStateException cause = new IllegalStateException("Cause");
        VeldException exception = new VeldException("Wrapper", cause);
        
        assertNotNull(exception.getCause().getStackTrace());
        assertTrue(exception.getCause().getStackTrace().length > 0);
    }
}
