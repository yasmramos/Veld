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
 * Utilidades para la creación y configuración de mocks en pruebas.
 * 
 * <p>Esta clase proporciona métodos de fábrica para crear mocks de Mockito
 * con configuración apropiada para el contexto de pruebas de Veld.</p>
 * 
 * @author Veld Framework
 * @since 1.0.0
 */
public final class MockFactory {
    
    private MockFactory() {
        // Utility class
    }
    
    /**
     * Crea un mock para un tipo específico.
     * 
     * @param <T> tipo del mock
     * @param type clase del tipo a mockear
     * @return mock configurado
     */
    @SuppressWarnings("unchecked")
    public static <T> T createMock(Class<T> type) {
        return (T) Mockito.mock(type, withDefaultSettings());
    }
    
    /**
     * Crea un mock para un campo basado en su anotación.
     * 
     * @param <T> tipo del mock
     * @param type clase del tipo a mockear
     * @param annotation anotación con configuración del mock
     * @return mock configurado
     */
    @SuppressWarnings("unchecked")
    public static <T> T createMock(Class<T> type, RegisterMock annotation) {
        if (annotation == null) {
            return createMock(type);
        }
        
        // Configurar según la anotación
        MockSettings settings = withDefaultSettings();
        
        // Configurar nombre del mock
        String name = annotation.name();
        if (!name.isEmpty()) {
            settings.name(name);
        }
        
        // Configurar Answer
        Answers answer = determineAnswer(annotation);
        if (answer != Answers.RETURNS_DEFAULTS) {
            settings.defaultAnswer(answer);
        }
        
        return (T) Mockito.mock(type, settings);
    }
    
    /**
     * Crea un mock con configuración específica para un campo.
     * 
     * @param field campo para el cual crear el mock
     * @return mock configurado
     */
    @SuppressWarnings("unchecked")
    public static Object createMockForField(Field field) {
        Class<?> fieldType = field.getType();
        RegisterMock annotation = field.getAnnotation(RegisterMock.class);
        return createMock(fieldType, annotation);
    }
    
    /**
     * Resetea un mock a su estado inicial.
     * 
     * @param mock mock a resetear
     */
    public static void resetMock(Object mock) {
        Mockito.reset(mock);
    }
    
    /**
     * Resetea múltiples mocks.
     * 
     * @param mocks mocks a resetear
     */
    public static void resetMocks(Object... mocks) {
        for (Object mock : mocks) {
            if (mock != null) {
                resetMock(mock);
            }
        }
    }
    
    /**
     * Verifica que un mock fue llamado.
     * 
     * @param mock mock a verificar
     */
    public static void verifyNoInteractions(Object mock) {
        Mockito.verifyNoInteractions(mock);
    }
    
    /**
     * Verifica interacciones específicas en un mock.
     * 
     * @param mock mock a verificar
     * @param times número esperado de llamadas
     */
    public static void verifyNoMoreInteractions(Object mock) {
        Mockito.verifyNoMoreInteractions(mock);
    }
    
    /**
     * Configura el modo de strictness para todos los mocks.
     * 
     * @param strictness nivel de strictness
     */
    public static void setGlobalStrictness(Strictness strictness) {
        Mockito.mockitoSession()
            .initMocks(null)
            .strictness(strictness)
            .startMocking();
    }
    
    /**
     * Crea un spy de un objeto real.
     * 
     * @param <T> tipo del objeto
     * @param object objeto a espiar
     * @return spy configurado
     */
    public static <T> T spy(T object) {
        return Mockito.spy(object);
    }
    
    /**
     * Configuración por defecto para mocks de Veld Test.
     */
    private static MockSettings withDefaultSettings() {
        return Mockito.withSettings()
            .defaultAnswer(Answers.RETURNS_DEFAULTS)
            .strictness(Strictness.LENIENT);
    }
    
    /**
     * Determina el tipo de Answer basado en la anotación.
     */
    private static Answers determineAnswer(RegisterMock annotation) {
        Answers answer = annotation.answer();
        return answer != null ? answer : Answers.RETURNS_DEFAULTS;
    }
    
    /**
     * Crea un mock que lanza una excepción por defecto.
     * 
     * @param <T> tipo del mock
     * @param type clase del tipo
     * @param exception excepción a lanzar
     * @return mock configurado
     */
    @SuppressWarnings("unchecked")
    public static <T> T createThrowingMock(Class<T> type, RuntimeException exception) {
        return (T) Mockito.mock(type, invocation -> {
            throw exception;
        });
    }
    
    /**
     * Crea un mock que retorna valores consecutivos.
     * 
     * @param <T> tipo del mock
     * @param type clase del tipo
     * @param values valores a retornar en secuencia
     * @return mock configurado
     */
    @SuppressWarnings("unchecked")
    public static <T> T createSequentialMock(Class<T> type, Object... values) {
        return (T) Mockito.mock(type, invocation -> {
            // Retornar valores en secuencia
            return null;
        });
    }
}
