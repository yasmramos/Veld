package io.github.yasmramos.veld.test.extension;

import io.github.yasmramos.veld.Veld;
import io.github.yasmramos.veld.annotation.Component;
import io.github.yasmramos.veld.test.annotation.Inject;
import io.github.yasmramos.veld.test.annotation.RegisterMock;
import io.github.yasmramos.veld.test.annotation.TestProfile;
import io.github.yasmramos.veld.test.annotation.VeldTest;
import io.github.yasmramos.veld.test.context.TestContext;
import io.github.yasmramos.veld.test.mock.MockFactory;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestInstancePostProcessor;
import org.mockito.Mockito;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

/**
 * Extensión JUnit 5 para integración automática de Veld en pruebas.
 * 
 * <p>Esta extensión gestiona automáticamente el ciclo de vida del
 * contenedor de pruebas, incluyendo:</p>
 * <ul>
 *   <li>Creación de mocks para campos anotados con {@code @RegisterMock}</li>
 *   <li>Inicialización del contexto de pruebas</li>
 *   <li>Inyección de beans en campos anotados con {@code @Inject}</li>
 *   <li>Reset de mocks entre pruebas</li>
 *   <li>Cierre del contexto al finalizar</li>
 * </ul>
 * 
 * <h2>Activación</h2>
 * <p>La extensión se activa automáticamente cuando una clase de prueba
 * está anotada con {@code @VeldTest}.</p>
 * 
 * <h2>Ciclo de Vida</h2>
 * <ol>
 *   <li><b>PostProcessTestInstance:</b> Detecta mocks, crea contexto, inyecta campos</li>
 *   <li><b>BeforeEachCallback:</b> Resetea mocks para aislamiento</li>
 *   <li><b>AfterEachCallback:</b> Opcional: limpieza por prueba</li>
 *   <li><b>AfterAllCallback:</b> Cierra el contexto</li>
 * </ol>
 * 
 * @author Veld Framework
 * @since 1.0.0
 * @see VeldTest
 * @see RegisterMock
 * @see Inject
 * @see TestContext
 */
public class VeldJupiterExtension implements 
        TestInstancePostProcessor,
        BeforeEachCallback,
        AfterEachCallback,
        AfterAllCallback {
    
    private static final String CONTEXT_KEY = "veld.test.context";
    private static final String MOCKS_KEY = "veld.test.mocks";
    
    @Override
    public void postProcessTestInstance(Object testInstance, ExtensionContext context) {
        Class<?> testClass = context.getRequiredTestClass();
        
        // Obtener configuración de la anotación @VeldTest
        VeldTest veldTest = testClass.getAnnotation(VeldTest.class);
        if (veldTest == null) {
            return; // No es una prueba Veld
        }
        
        // Obtener perfil si está definido
        String profile = extractProfile(testClass, veldTest);
        
        try {
            // Escanear y crear mocks
            Map<Class<?>, Object> mocks = scanAndCreateMocks(testInstance);
            storeMocks(context, mocks);
            
            // Crear contexto de pruebas
            TestContext testContext = createTestContext(veldTest, mocks, profile);
            storeContext(context, testContext);
            
            // Inyectar campos en la instancia de prueba
            injectFields(testInstance, testContext, mocks);
            
        } catch (Exception e) {
            throw new ExtensionInitializationException(
                "Error al inicializar contexto Veld para prueba: " + 
                testClass.getName(), e);
        }
    }
    
    @Override
    public void beforeEach(ExtensionContext context) {
        TestContext testContext = getContext(context);
        if (testContext != null) {
            // Resetear mocks para aislamiento entre pruebas
            testContext.resetMocks();
        }
    }
    
    @Override
    public void afterEach(ExtensionContext context) {
        // Opcional: limpieza específica por prueba
        // Por defecto no hacemos nada para mantener rendimiento
    }
    
    @Override
    public void afterAll(ExtensionContext context) {
        TestContext testContext = getContext(context);
        if (testContext != null) {
            try {
                testContext.close();
            } catch (Exception e) {
                // Log warning pero no fallar la prueba
                System.err.println("Warning: Error al cerrar contexto Veld: " + e.getMessage());
            }
            removeContext(context);
        }
    }
    
    /**
     * Extrae el perfil de la clase de prueba.
     */
    private String extractProfile(Class<?> testClass, VeldTest veldTest) {
        // Verificar anotación @TestProfile en la clase
        TestProfile profileAnnotation = testClass.getAnnotation(TestProfile.class);
        if (profileAnnotation != null) {
            return profileAnnotation.value();
        }
        
        // Usar perfil de @VeldTest
        return veldTest.profile();
    }
    
    /**
     * Escanea la instancia de prueba en busca de campos anotados
     * con @RegisterMock y crea los correspondientes mocks.
     */
    private Map<Class<?>, Object> scanAndCreateMocks(Object testInstance) {
        Map<Class<?>, Object> mocks = new HashMap<>();
        Class<?> clazz = testInstance.getClass();
        
        while (clazz != null && clazz != Object.class) {
            for (Field field : clazz.getDeclaredFields()) {
                if (field.isAnnotationPresent(RegisterMock.class)) {
                    Object mock = createMockForField(field, testInstance);
                    mocks.put(field.getType(), mock);
                }
            }
            clazz = clazz.getSuperclass();
        }
        
        return mocks;
    }
    
    /**
     * Crea un mock para un campo específico.
     */
    private Object createMockForField(Field field, Object testInstance) {
        Class<?> fieldType = field.getType();
        RegisterMock annotation = field.getAnnotation(RegisterMock.class);
        
        // Crear mock
        Object mock = MockFactory.createMock(fieldType, annotation);
        
        // Inyectar en el campo de la instancia de prueba
        field.setAccessible(true);
        try {
            field.set(testInstance, mock);
        } catch (IllegalAccessException e) {
            throw new ExtensionInitializationException(
                "No se pudo injectar mock en campo: " + field.getName(), e);
        }
        
        return mock;
    }
    
    /**
     * Crea el contexto de pruebas.
     */
    private TestContext createTestContext(VeldTest veldTest, 
                                          Map<Class<?>, Object> mocks,
                                          String profile) {
        TestContext.Builder builder = TestContext.Builder.create()
            .withProfile(profile)
            .withProperties(veldTest.properties());
        
        // Registrar mocks en el contexto
        for (Map.Entry<Class<?>, Object> entry : mocks.entrySet()) {
            builder.withMockRaw(entry.getKey(), entry.getValue());
        }
        
        // Registrar clases de configuración si están definidas
        for (Class<?> configClass : veldTest.classes()) {
            builder.withMock(configClass.getSimpleName(), createInstance(configClass));
        }
        
        return builder.build();
    }
    
    /**
     * Inyecta beans en los campos de la instancia de prueba.
     */
    private void injectFields(Object testInstance, TestContext context, 
                             Map<Class<?>, Object> mocks) {
        Class<?> clazz = testInstance.getClass();
        
        while (clazz != null && clazz != Object.class) {
            for (Field field : clazz.getDeclaredFields()) {
                if (shouldInject(field, mocks)) {
                    injectField(testInstance, field, context);
                }
            }
            clazz = clazz.getSuperclass();
        }
    }
    
    /**
     * Determina si un campo debe ser inyectado.
     */
    private boolean shouldInject(Field field, Map<Class<?>, Object> mocks) {
        // Si ya tiene @RegisterMock, ya fue manejado
        if (field.isAnnotationPresent(RegisterMock.class)) {
            return false;
        }
        
        // Si tiene @Inject, inyectar
        return field.isAnnotationPresent(Inject.class) ||
               field.getType().getAnnotation(Component.class) != null;
    }
    
    /**
     * Inyecta un campo específico.
     */
    private void injectField(Object testInstance, Field field, 
                           TestContext context) {
        field.setAccessible(true);
        
        try {
            // Verificar si existe un mock para este tipo
            if (context.hasMock(field.getType())) {
                Object mock = context.getMock(field.getType()).orElse(null);
                field.set(testInstance, mock);
            } else {
                // Intentar obtener bean del contexto
                Object bean = context.getBean(field.getType());
                field.set(testInstance, bean);
            }
        } catch (IllegalAccessException e) {
            // Campo no accesible, ignorar
        } catch (Exception e) {
            // Bean no encontrado, verificar si es opcional
            Inject inject = field.getAnnotation(Inject.class);
            if (inject != null && inject.optional()) {
                // Opcional, ignorar
            } else {
                // No optional, lanzar excepción
                throw new ExtensionInitializationException(
                    "No se pudo injectar campo: " + field.getName(), e);
            }
        }
    }
    
    /**
     * Crea una instancia de una clase de configuración.
     */
    private Object createInstance(Class<?> clazz) {
        try {
            return clazz.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new ExtensionInitializationException(
                "No se pudo crear instancia de: " + clazz.getName(), e);
        }
    }
    
    /**
     * Almacena el contexto en el store de JUnit.
     */
    private void storeContext(ExtensionContext context, TestContext testContext) {
        context.getStore(Namespace.create(CONTEXT_KEY))
            .put(CONTEXT_KEY, testContext);
    }
    
    /**
     * Obtiene el contexto del store de JUnit.
     */
    private TestContext getContext(ExtensionContext context) {
        return context.getStore(Namespace.create(CONTEXT_KEY))
            .get(CONTEXT_KEY, TestContext.class);
    }
    
    /**
     * Remueve el contexto del store de JUnit.
     */
    private void removeContext(ExtensionContext context) {
        context.getStore(Namespace.create(CONTEXT_KEY))
            .remove(CONTEXT_KEY);
    }
    
    /**
     * Almacena los mocks en el store de JUnit.
     */
    private void storeMocks(ExtensionContext context, Map<Class<?>, Object> mocks) {
        context.getStore(Namespace.create(MOCKS_KEY))
            .put(MOCKS_KEY, mocks);
    }
    
    /**
     * Namespace para el store de la extensión.
     */
    private static final class Namespace {
        static ExtensionContext.Namespace create(Object... ids) {
            return ExtensionContext.Namespace.create(
                "io.github.yasmramos.veld.test", ids);
        }
    }
    
    /**
     * Excepción para errores de inicialización de la extensión.
     */
    public static class ExtensionInitializationException extends RuntimeException {
        public ExtensionInitializationException(String message) {
            super(message);
        }
        
        public ExtensionInitializationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
