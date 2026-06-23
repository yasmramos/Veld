package io.github.yasmramos.veld.test.mock;

import io.github.yasmramos.veld.test.annotation.RegisterMock;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockSettings;
import org.mockito.Mockito;
import org.mockito.mock.MockCreationSettings;
import org.mockito.quality.Strictness;

import java.lang.reflect.Field;

/**
 * Utilities for creating and configuring mocks in tests.
 * 
 * <p>This class provides factory methods to create Mockito mocks
 * with appropriate configuration for the Veld testing context.</p>
 * 
 * @author Veld Framework
 * @since 1.0.0
 */
public final class MockFactory {
    
    private MockFactory() {
        // Utility class
    }
    
    /**
     * Creates a mock for a specific type.
     * 
     * @param <T> mock type
     * @param type class of the type to mock
     * @return configured mock
     */
    public static <T> T createMock(Class<T> type) {
        return Mockito.mock(type, withDefaultSettings());
    }
    
    /**
     * Creates a mock for a field based on its annotation.
     * 
     * @param <T> mock type
     * @param type class of the type to mock
     * @param annotation annotation with mock configuration
     * @return configured mock
     */
    public static <T> T createMock(Class<T> type, RegisterMock annotation) {
        if (annotation == null) {
            return createMock(type);
        }
        
        // Configure according to the annotation
        MockSettings settings = withDefaultSettings();
        
        // Configure mock name
        String name = annotation.name();
        if (!name.isEmpty()) {
            settings.name(name);
        }
        
        // Configure Answer
        Answers answer = determineAnswer(annotation);
        if (answer != Answers.RETURNS_DEFAULTS) {
            settings.defaultAnswer(answer);
        }
        
        return Mockito.mock(type, settings);
    }
    
    /**
     * Creates a mock with specific configuration for a field.
     * 
     * @param field field for which to create the mock
     * @return configured mock
     */
    @SuppressWarnings("unchecked")
    public static Object createMockForField(Field field) {
        Class<?> fieldType = field.getType();
        RegisterMock annotation = field.getAnnotation(RegisterMock.class);
        return createMock(fieldType, annotation);
    }
    
    /**
     * Resets a mock to its initial state.
     * 
     * @param mock mock to reset
     */
    public static void resetMock(Object mock) {
        Mockito.reset(mock);
    }
    
    /**
     * Resets multiple mocks.
     * 
     * @param mocks mocks to reset
     */
    public static void resetMocks(Object... mocks) {
        for (Object mock : mocks) {
            if (mock != null) {
                resetMock(mock);
            }
        }
    }
    
    /**
     * Verifies that a mock was not called.
     * 
     * @param mock mock to verify
     */
    public static void verifyNoInteractions(Object mock) {
        Mockito.verifyNoInteractions(mock);
    }
    
    /**
     * Verifies specific interactions on a mock.
     * 
     * @param mock mock to verify
     * @param times expected number of calls
     */
    public static void verifyNoMoreInteractions(Object mock) {
        Mockito.verifyNoMoreInteractions(mock);
    }
    
    /**
     * Configures the strictness mode for all mocks.
     * 
     * @param strictness strictness level
     */
    public static void setGlobalStrictness(Strictness strictness) {
        Mockito.mockitoSession()
            .initMocks((Object[]) null)
            .strictness(strictness)
            .startMocking();
    }
    
    /**
     * Creates a spy of a real object.
     * 
     * @param <T> object type
     * @param object object to spy
     * @return configured spy
     */
    public static <T> T spy(T object) {
        return Mockito.spy(object);
    }
    
    /**
     * Default configuration for Veld Test mocks.
     */
    private static MockSettings withDefaultSettings() {
        return Mockito.withSettings()
            .defaultAnswer(Answers.RETURNS_DEFAULTS)
            .strictness(Strictness.LENIENT);
    }
    
    /**
     * Determines the Answer type based on the annotation.
     */
    private static Answers determineAnswer(RegisterMock annotation) {
        Answers answer = annotation.answer();
        return answer != null ? answer : Answers.RETURNS_DEFAULTS;
    }
    
    /**
     * Creates a mock that throws an exception by default.
     * 
     * @param <T> mock type
     * @param type class of the type
     * @param exception exception to throw
     * @return configured mock
     */
    @SuppressWarnings("unchecked")
    public static <T> T createThrowingMock(Class<T> type, RuntimeException exception) {
        return Mockito.mock(type, invocation -> {
            throw exception;
        });
    }
    
    /**
     * Creates a mock that returns consecutive values.
     * 
     * @param <T> mock type
     * @param type class of the type
     * @param values values to return in sequence
     * @return configured mock
     */
    @SuppressWarnings("unchecked")
    public static <T> T createSequentialMock(Class<T> type, Object... values) {
        return Mockito.mock(type, invocation -> {
            // Return values in sequence
            return null;
        });
    }
}
