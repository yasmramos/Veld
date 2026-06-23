package io.github.yasmramos.veld.test.extension;

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
 * </ul>
 *
 * NOTE: This extension needs to be updated to work with the new Veld API
 * where beans are accessed via generated static methods.
 *
 * @author Veld Framework
 * @since 1.0.0
 */
public class VeldJupiterExtension implements
        TestInstancePostProcessor,
        BeforeEachCallback,
        AfterEachCallback,
        AfterAllCallback {

    private TestContext context;

    @Override
    public void postProcessTestInstance(Object testInstance, ExtensionContext context) throws Exception {
        // Create mocks for @RegisterMock annotated fields
        Map<Class<?>, Object> mocks = new HashMap<>();
        Map<String, Object> namedMocks = new HashMap<>();

        for (Field field : testInstance.getClass().getDeclaredFields()) {
            if (field.isAnnotationPresent(RegisterMock.class)) {
                RegisterMock annotation = field.getAnnotation(RegisterMock.class);
                Class<?> fieldType = field.getType();
                Object mock = Mockito.mock(fieldType);

                if (annotation.name().isEmpty()) {
                    mocks.put(fieldType, mock);
                } else {
                    namedMocks.put(annotation.name(), mock);
                }

                field.setAccessible(true);
                field.set(testInstance, mock);
            }
        }

        // Determine test profile
        String profile = testInstance.getClass().isAnnotationPresent(TestProfile.class)
                ? testInstance.getClass().getAnnotation(TestProfile.class).value()
                : "test";

        // Create test context
        this.context = new TestContext.Builder()
                .withProfile(profile)
                .build();

        // Inject dependencies using reflection
        // NOTE: This needs update for new Veld API
        injectDependencies(testInstance);
    }

    private void injectDependencies(Object testInstance) throws Exception {
        for (Field field : testInstance.getClass().getDeclaredFields()) {
            if (field.isAnnotationPresent(Inject.class)) {
                Class<?> fieldType = field.getType();
                field.setAccessible(true);

                // For new Veld API, use generated static methods
                // This is a placeholder - actual implementation needs update
                Object bean = getBeanForType(fieldType);
                if (bean != null) {
                    field.set(testInstance, bean);
                }
            }
        }
    }

    private Object getBeanForType(Class<?> type) {
        // Placeholder - needs implementation for new Veld API
        return null;
    }

    @Override
    public void beforeEach(ExtensionContext context) throws Exception {
        // Test setup if needed
    }

    @Override
    public void afterEach(ExtensionContext context) throws Exception {
        // Test cleanup if needed
    }

    @Override
    public void afterAll(ExtensionContext context) throws Exception {
        if (this.context != null) {
            this.context.close();
        }
    }
}
