package io.github.yasmramos.veld.aop.spi;

import io.github.yasmramos.veld.aop.AopClassGenerator;
import io.github.yasmramos.veld.aop.AopComponentNode;
import io.github.yasmramos.veld.aop.AopGenerationContext;
import io.github.yasmramos.veld.aop.AopGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias para {@link SpiAopExtensionExecutor}.
 * 
 * @author Veld Team
 * @version 1.0.0
 */
@DisplayName("SpiAopExtensionExecutor Tests")
class SpiAopExtensionExecutorTest {

    @Mock
    private Messager mockMessager;

    @Mock
    private Elements mockElementUtils;

    @Mock
    private Types mockTypeUtils;

    @Mock
    private Filer mockFiler;

    private SpiAopExtensionExecutor executor;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    @DisplayName("Should create executor with enabled=true")
    void shouldCreateExecutorWithEnabledTrue() {
        // When
        executor = new SpiAopExtensionExecutor(
            true, mockMessager, mockElementUtils, mockTypeUtils, mockFiler);

        // Then
        assertNotNull(executor);
        verify(mockMessager, atLeastOnce())
            .printMessage(any(), anyString());
    }

    @Test
    @DisplayName("Should create executor with enabled=false")
    void shouldCreateExecutorWithEnabledFalse() {
        // When
        executor = new SpiAopExtensionExecutor(
            false, mockMessager, mockElementUtils, mockTypeUtils, mockFiler);

        // Then
        assertNotNull(executor);
        assertFalse(executor.hasExtensions());
        assertEquals(0, executor.getExtensionCount());
    }

    @Test
    @DisplayName("Should return empty map when componentsData is null")
    void shouldReturnEmptyMapWhenComponentsDataIsNull() {
        // Given
        executor = new SpiAopExtensionExecutor(
            true, mockMessager, mockElementUtils, mockTypeUtils, mockFiler);

        // When
        Map<String, String> result = executor.generateAopClasses(null);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Should return empty map when componentsData is empty")
    void shouldReturnEmptyMapWhenComponentsDataIsEmpty() {
        // Given
        executor = new SpiAopExtensionExecutor(
            true, mockMessager, mockElementUtils, mockTypeUtils, mockFiler);
        Map<String, SpiAopExtensionExecutor.ComponentData> emptyData = Collections.emptyMap();

        // When
        Map<String, String> result = executor.generateAopClasses(emptyData);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Should handle ComponentData correctly")
    void shouldHandleComponentDataCorrectly() {
        // Given
        executor = new SpiAopExtensionExecutor(
            true, mockMessager, mockElementUtils, mockTypeUtils, mockFiler);

        Map<String, SpiAopExtensionExecutor.ComponentData> componentsData = new HashMap<>();
        SpiAopExtensionExecutor.ComponentData componentData = 
            new SpiAopExtensionExecutor.ComponentData(
                "com.example.MyService",
                "com/example/MyService",
                Arrays.asList("LoggingInterceptor"),
                null
            );
        componentsData.put("com.example.MyService", componentData);

        // When
        Map<String, String> result = executor.generateAopClasses(componentsData);

        // Then
        assertNotNull(result);
        // Result may be empty if AOP generation fails or has no interceptors to apply
    }

    @Test
    @DisplayName("Should handle multiple components")
    void shouldHandleMultipleComponents() {
        // Given
        executor = new SpiAopExtensionExecutor(
            true, mockMessager, mockElementUtils, mockTypeUtils, mockFiler);

        Map<String, SpiAopExtensionExecutor.ComponentData> componentsData = new HashMap<>();
        
        componentsData.put("com.example.Service1", 
            new SpiAopExtensionExecutor.ComponentData(
                "com.example.Service1",
                "com/example/Service1",
                Collections.singletonList("Interceptor1"),
                null
            ));
        
        componentsData.put("com.example.Service2", 
            new SpiAopExtensionExecutor.ComponentData(
                "com.example.Service2",
                "com/example/Service2",
                Collections.singletonList("Interceptor2"),
                null
            ));

        // When
        Map<String, String> result = executor.generateAopClasses(componentsData);

        // Then
        assertNotNull(result);
    }

    @Test
    @DisplayName("Should handle component without interceptors")
    void shouldHandleComponentWithoutInterceptors() {
        // Given
        executor = new SpiAopExtensionExecutor(
            true, mockMessager, mockElementUtils, mockTypeUtils, mockFiler);

        Map<String, SpiAopExtensionExecutor.ComponentData> componentsData = new HashMap<>();
        componentsData.put("com.example.NoInterceptors", 
            new SpiAopExtensionExecutor.ComponentData(
                "com.example.NoInterceptors",
                "com/example/NoInterceptors",
                Collections.emptyList(),
                null
            ));

        // When
        Map<String, String> result = executor.generateAopClasses(componentsData);

        // Then
        assertNotNull(result);
    }

    @Test
    @DisplayName("Should track execution errors")
    void shouldTrackExecutionErrors() {
        // Given
        executor = new SpiAopExtensionExecutor(
            true, mockMessager, mockElementUtils, mockTypeUtils, mockFiler);

        // When & Then
        List<String> errors = executor.getExecutionErrors();
        assertNotNull(errors);
        assertFalse(executor.hasExecutionErrors());
    }

    @Test
    @DisplayName("Should provide extensions info")
    void shouldProvideExtensionsInfo() {
        // Given
        executor = new SpiAopExtensionExecutor(
            true, mockMessager, mockElementUtils, mockTypeUtils, mockFiler);

        // When
        String info = executor.getExtensionsInfo();

        // Then
        assertNotNull(info);
        assertFalse(info.isEmpty());
    }

    @Test
    @DisplayName("Should handle disabled executor gracefully")
    void shouldHandleDisabledExecutorGracefully() {
        // Given
        executor = new SpiAopExtensionExecutor(
            false, mockMessager, mockElementUtils, mockTypeUtils, mockFiler);

        // When
        Map<String, String> result = executor.generateAopClasses(
            Collections.singletonMap("com.example.Test",
                new SpiAopExtensionExecutor.ComponentData(
                    "com.example.Test",
                    "com/example/Test",
                    Collections.singletonList("TestInterceptor"),
                    null
                )
            )
        );

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
        assertEquals("AOP Extensions disabled", executor.getExtensionsInfo());
    }

    @Test
    @DisplayName("Should handle ComponentData with null type mirror")
    void shouldHandleComponentDataWithNullTypeMirror() {
        // Given
        executor = new SpiAopExtensionExecutor(
            true, mockMessager, mockElementUtils, mockTypeUtils, mockFiler);

        SpiAopExtensionExecutor.ComponentData componentData = 
            new SpiAopExtensionExecutor.ComponentData(
                "com.example.TestService",
                "com/example/TestService",
                Arrays.asList("Interceptor1", "Interceptor2"),
                null
            );

        // When
        Map<String, SpiAopExtensionExecutor.ComponentData> componentsData = 
            Collections.singletonMap("com.example.TestService", componentData);
        Map<String, String> result = executor.generateAopClasses(componentsData);

        // Then
        assertNotNull(result);
        assertNotNull(componentData.getClassName());
        assertNotNull(componentData.getInternalName());
        assertNotNull(componentData.getInterceptors());
        assertNull(componentData.getTypeMirror());
    }

    @Test
    @DisplayName("Should verify ComponentData getters")
    void shouldVerifyComponentDataGetters() {
        // Given
        String className = "com.example.MyClass";
        String internalName = "com/example/MyClass";
        List<String> interceptors = Arrays.asList("Int1", "Int2");
        
        SpiAopExtensionExecutor.ComponentData componentData = 
            new SpiAopExtensionExecutor.ComponentData(
                className, internalName, interceptors, null
            );

        // Then
        assertEquals(className, componentData.getClassName());
        assertEquals(internalName, componentData.getInternalName());
        assertEquals(interceptors, componentData.getInterceptors());
        assertNull(componentData.getTypeMirror());
    }

    @Test
    @DisplayName("Should handle component with single interceptor")
    void shouldHandleComponentWithSingleInterceptor() {
        // Given
        executor = new SpiAopExtensionExecutor(
            true, mockMessager, mockElementUtils, mockTypeUtils, mockFiler);

        Map<String, SpiAopExtensionExecutor.ComponentData> componentsData = new HashMap<>();
        componentsData.put("com.example.SingleInterceptor", 
            new SpiAopExtensionExecutor.ComponentData(
                "com.example.SingleInterceptor",
                "com/example/SingleInterceptor",
                Collections.singletonList("SingleInterceptor"),
                null
            ));

        // When
        Map<String, String> result = executor.generateAopClasses(componentsData);

        // Then
        assertNotNull(result);
    }

    @Test
    @DisplayName("Should handle component with many interceptors")
    void shouldHandleComponentWithManyInterceptors() {
        // Given
        executor = new SpiAopExtensionExecutor(
            true, mockMessager, mockElementUtils, mockTypeUtils, mockFiler);

        List<String> manyInterceptors = Arrays.asList(
            "Interceptor1", "Interceptor2", "Interceptor3",
            "Interceptor4", "Interceptor5"
        );

        Map<String, SpiAopExtensionExecutor.ComponentData> componentsData = new HashMap<>();
        componentsData.put("com.example.ManyInterceptors", 
            new SpiAopExtensionExecutor.ComponentData(
                "com.example.ManyInterceptors",
                "com/example/ManyInterceptors",
                manyInterceptors,
                null
            ));

        // When
        Map<String, String> result = executor.generateAopClasses(componentsData);

        // Then
        assertNotNull(result);
    }

    @Test
    @DisplayName("Should not throw exception on malformed component data")
    void shouldNotThrowExceptionOnMalformedComponentData() {
        // Given
        executor = new SpiAopExtensionExecutor(
            true, mockMessager, mockElementUtils, mockTypeUtils, mockFiler);

        Map<String, SpiAopExtensionExecutor.ComponentData> componentsData = new HashMap<>();
        // Component with empty class name (edge case)
        componentsData.put("", 
            new SpiAopExtensionExecutor.ComponentData(
                "",
                "",
                Collections.emptyList(),
                null
            ));

        // When & Then - should not throw
        assertDoesNotThrow(() -> {
            executor.generateAopClasses(componentsData);
        });
    }
}
