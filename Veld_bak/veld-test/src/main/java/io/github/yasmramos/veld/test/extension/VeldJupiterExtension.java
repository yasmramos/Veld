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
 * JUnit 5 extension for automatic Veld integration in tests.
 * 
 * <p>This extension automatically manages the lifecycle of the
 * test container, including:</p>
 * <ul>
 *   <li>Creating mocks for fields annotated with {@code @RegisterMock}</li>
 *   <li>Initializing the test context</li>
 *   <li>Injecting beans into fields annotated with {@code @Inject}</li>
 *   <li>Resetting mocks between tests</li>
 *   <li>Closing the context at the end</li>
 * </ul>
 * 
 * <h2>Activation</h2>
 * <p>The extension is automatically activated when a test class
 * is annotated with {@code @VeldTest}.</p>
 * 
 * <h2>Lifecycle</h2>
 * <ol>
 *   <li><b>PostProcessTestInstance:</b> Detects mocks, creates context, injects fields</li>
 *   <li><b>BeforeEachCallback:</b> Resets mocks for isolation</li>
 *   <li><b>AfterEachCallback:</b> Optional: per-test cleanup</li>
 *   <li><b>AfterAllCallback:</b> Closes the context</li>
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
        
        // Get configuration from @VeldTest annotation
        VeldTest veldTest = testClass.getAnnotation(VeldTest.class);
        if (veldTest == null) {
            return; // Not a Veld test
        }
        
        // Get profile if defined
        String profile = extractProfile(testClass, veldTest);
        
        try {
            // Scan and create mocks
            Map<Class<?>, Object> mocks = scanAndCreateMocks(testInstance);
            storeMocks(context, mocks);
            
            // Create test context
            TestContext testContext = createTestContext(veldTest, mocks, profile);
            storeContext(context, testContext);
            
            // Inject fields into test instance
            injectFields(testInstance, testContext, mocks);
            
        } catch (Exception e) {
            throw new ExtensionInitializationException(
                "Error initializing Veld context for test: " + 
                testClass.getName(), e);
        }
    }
    
    @Override
    public void beforeEach(ExtensionContext context) {
        TestContext testContext = getContext(context);
        if (testContext != null) {
            // Reset mocks for test isolation
            testContext.resetMocks();
        }
    }
    
    @Override
    public void afterEach(ExtensionContext context) {
        // Optional: per-test specific cleanup
        // By default, we do nothing to maintain performance
    }
    
    @Override
    public void afterAll(ExtensionContext context) {
        TestContext testContext = getContext(context);
        if (testContext != null) {
            try {
                testContext.close();
            } catch (Exception e) {
                // Log warning but don't fail the test
                System.err.println("Warning: Error closing Veld context: " + e.getMessage());
            }
            removeContext(context);
        }
    }
    
    /**
     * Extracts the profile from the test class.
     */
    private String extractProfile(Class<?> testClass, VeldTest veldTest) {
        // Check @TestProfile annotation on the class
        TestProfile profileAnnotation = testClass.getAnnotation(TestProfile.class);
        if (profileAnnotation != null) {
            return profileAnnotation.value();
        }
        
        // Use profile from @VeldTest
        return veldTest.profile();
    }
    
    /**
     * Scans the test instance for fields annotated
     * with @RegisterMock and creates the corresponding mocks.
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
     * Creates a mock for a specific field.
     */
    private Object createMockForField(Field field, Object testInstance) {
        Class<?> fieldType = field.getType();
        RegisterMock annotation = field.getAnnotation(RegisterMock.class);
        
        // Create mock
        Object mock = MockFactory.createMock(fieldType, annotation);
        
        // Inject into test instance field
        field.setAccessible(true);
        try {
            field.set(testInstance, mock);
        } catch (IllegalAccessException e) {
            throw new ExtensionInitializationException(
                "Could not inject mock into field: " + field.getName(), e);
        }
        
        return mock;
    }
    
    /**
     * Creates the test context.
     */
    private TestContext createTestContext(VeldTest veldTest, 
                                          Map<Class<?>, Object> mocks,
                                          String profile) {
        TestContext.Builder builder = TestContext.Builder.create()
            .withProfile(profile)
            .withProperties(veldTest.properties());
        
        // Register mocks in context
        for (Map.Entry<Class<?>, Object> entry : mocks.entrySet()) {
            builder.withMockRaw(entry.getKey(), entry.getValue());
        }
        
        // Register configuration classes if defined
        for (Class<?> configClass : veldTest.classes()) {
            builder.withMock(configClass.getSimpleName(), createInstance(configClass));
        }
        
        return builder.build();
    }
    
    /**
     * Injects beans into the fields of the test instance.
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
     * Determines if a field should be injected.
     */
    private boolean shouldInject(Field field, Map<Class<?>, Object> mocks) {
        // If already has @RegisterMock, already handled
        if (field.isAnnotationPresent(RegisterMock.class)) {
            return false;
        }
        
        // If has @Inject, inject
        return field.isAnnotationPresent(Inject.class) ||
               field.getType().getAnnotation(Component.class) != null;
    }
    
    /**
     * Injects a specific field.
     */
    private void injectField(Object testInstance, Field field, 
                           TestContext context) {
        field.setAccessible(true);
        
        try {
            // Check if a mock exists for this type
            if (context.hasMock(field.getType())) {
                Object mock = context.getMock(field.getType()).orElse(null);
                field.set(testInstance, mock);
            } else {
                // Try to get bean from context
                Object bean = context.getBean(field.getType());
                field.set(testInstance, bean);
            }
        } catch (IllegalAccessException e) {
            // Field not accessible, ignore
        } catch (Exception e) {
            // Bean not found, check if it's optional
            Inject inject = field.getAnnotation(Inject.class);
            if (inject != null && inject.optional()) {
                // Optional, ignore
            } else {
                // Not optional, throw exception
                throw new ExtensionInitializationException(
                    "Could not inject field: " + field.getName(), e);
            }
        }
    }
    
    /**
     * Creates an instance of a configuration class.
     */
    private Object createInstance(Class<?> clazz) {
        try {
            return clazz.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new ExtensionInitializationException(
                "Could not create instance of: " + clazz.getName(), e);
        }
    }
    
    /**
     * Stores the context in the JUnit store.
     */
    private void storeContext(ExtensionContext context, TestContext testContext) {
        context.getStore(Namespace.create(CONTEXT_KEY))
            .put(CONTEXT_KEY, testContext);
    }
    
    /**
     * Gets the context from the JUnit store.
     */
    private TestContext getContext(ExtensionContext context) {
        return context.getStore(Namespace.create(CONTEXT_KEY))
            .get(CONTEXT_KEY, TestContext.class);
    }
    
    /**
     * Removes the context from the JUnit store.
     */
    private void removeContext(ExtensionContext context) {
        context.getStore(Namespace.create(CONTEXT_KEY))
            .remove(CONTEXT_KEY);
    }
    
    /**
     * Stores the mocks in the JUnit store.
     */
    private void storeMocks(ExtensionContext context, Map<Class<?>, Object> mocks) {
        context.getStore(Namespace.create(MOCKS_KEY))
            .put(MOCKS_KEY, mocks);
    }
    
    /**
     * Namespace for the extension store.
     */
    private static final class Namespace {
        static ExtensionContext.Namespace create(Object... ids) {
            return ExtensionContext.Namespace.create(
                "io.github.yasmramos.veld.test", ids);
        }
    }
    
    /**
     * Exception for extension initialization errors.
     */
    public static class ExtensionInitializationException extends RuntimeException {
        private static final long serialVersionUID = 1L;
        
        public ExtensionInitializationException(String message) {
            super(message);
        }
        
        public ExtensionInitializationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
