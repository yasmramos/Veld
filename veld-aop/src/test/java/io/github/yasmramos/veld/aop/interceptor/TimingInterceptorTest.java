package io.github.yasmramos.veld.aop.interceptor;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas unitarias para {@link TimingInterceptor}.
 * 
 * @author Veld Team
 * @version 1.0.0
 */
@DisplayName("TimingInterceptor Tests")
class TimingInterceptorTest {

    @Test
    @DisplayName("Should create TimingInterceptor instance")
    void shouldCreateTimingInterceptorInstance() {
        // When
        TimingInterceptor interceptor = new TimingInterceptor();

        // Then
        assertNotNull(interceptor);
    }

    @Test
    @DisplayName("Should execute beforeMethod without throwing")
    void shouldExecuteBeforeMethodWithoutThrowing() {
        // Given
        TimingInterceptor interceptor = new TimingInterceptor();

        // When & Then - should not throw
        assertDoesNotThrow(() -> {
            interceptor.beforeMethod("testMethod", new Object[]{"arg1", "arg2"});
        });
    }

    @Test
    @DisplayName("Should execute afterMethod without throwing")
    void shouldExecuteAfterMethodWithoutThrowing() {
        // Given
        TimingInterceptor interceptor = new TimingInterceptor();

        // When & Then - should not throw
        assertDoesNotThrow(() -> {
            interceptor.afterMethod("testMethod", "result");
        });
    }

    @Test
    @DisplayName("Should execute afterThrowing without throwing")
    void shouldExecuteAfterThrowingWithoutThrowing() {
        // Given
        TimingInterceptor interceptor = new TimingInterceptor();
        Exception testException = new RuntimeException("Test exception");

        // When & Then - should not throw
        assertDoesNotThrow(() -> {
            interceptor.afterThrowing("testMethod", testException);
        });
    }

    @Test
    @DisplayName("Should handle null arguments in beforeMethod")
    void shouldHandleNullArgumentsInBeforeMethod() {
        // Given
        TimingInterceptor interceptor = new TimingInterceptor();

        // When & Then - should not throw
        assertDoesNotThrow(() -> {
            interceptor.beforeMethod(null, null);
        });
    }

    @Test
    @DisplayName("Should handle null result in afterMethod")
    void shouldHandleNullResultInAfterMethod() {
        // Given
        TimingInterceptor interceptor = new TimingInterceptor();

        // When & Then - should not throw
        assertDoesNotThrow(() -> {
            interceptor.afterMethod("testMethod", null);
        });
    }

    @Test
    @DisplayName("Should handle null exception in afterThrowing")
    void shouldHandleNullExceptionInAfterThrowing() {
        // Given
        TimingInterceptor interceptor = new TimingInterceptor();

        // When & Then - should not throw
        assertDoesNotThrow(() -> {
            interceptor.afterThrowing("testMethod", null);
        });
    }

    @Test
    @DisplayName("Should handle empty args array")
    void shouldHandleEmptyArgsArray() {
        // Given
        TimingInterceptor interceptor = new TimingInterceptor();

        // When & Then - should not throw
        assertDoesNotThrow(() -> {
            interceptor.beforeMethod("testMethod", new Object[]{});
        });
    }

    @Test
    @DisplayName("Should handle various argument types")
    void shouldHandleVariousArgumentTypes() {
        // Given
        TimingInterceptor interceptor = new TimingInterceptor();
        Object[] mixedArgs = {
            "string",
            42,
            true,
            3.14,
            null,
            new Object()
        };

        // When & Then - should not throw
        assertDoesNotThrow(() -> {
            interceptor.beforeMethod("testMethod", mixedArgs);
        });
    }
}
