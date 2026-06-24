package io.github.yasmramos.veld.aop.spi;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.lang.model.type.TypeMirror;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias para {@link AopComponentNodeImpl}.
 * 
 * @author Veld Team
 * @version 1.0.0
 */
@DisplayName("AopComponentNodeImpl Tests")
class AopComponentNodeImplTest {

    @Mock
    private SpiAopExtensionExecutor.ComponentData mockComponentData;

    @Mock
    private TypeMirror mockTypeMirror;

    private AopComponentNodeImpl node;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    @DisplayName("Should create node with valid component data")
    void shouldCreateNodeWithValidComponentData() {
        // Given
        when(mockComponentData.getClassName()).thenReturn("com.example.MyService");
        when(mockComponentData.getInternalName()).thenReturn("com/example/MyService");
        when(mockComponentData.getInterceptors()).thenReturn(Arrays.asList("Interceptor1", "Interceptor2"));
        when(mockComponentData.getTypeMirror()).thenReturn(mockTypeMirror);

        // When
        node = new AopComponentNodeImpl(mockComponentData);

        // Then
        assertNotNull(node);
        assertEquals("com.example.MyService", node.getClassName());
        assertEquals("com/example/MyService", node.getInternalName());
        assertEquals("MyService", node.getSimpleName());
        assertEquals("com.example", node.getPackageName());
        assertEquals(2, node.getInterceptors().size());
        assertTrue(node.hasInterceptors());
        assertEquals(mockTypeMirror, node.getTypeMirror());
    }

    @Test
    @DisplayName("Should throw exception when component data is null")
    void shouldThrowExceptionWhenComponentDataIsNull() {
        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> new AopComponentNodeImpl(null)
        );
        assertEquals("ComponentData cannot be null", exception.getMessage());
    }

    @Test
    @DisplayName("Should handle class without package")
    void shouldHandleClassWithoutPackage() {
        // Given
        when(mockComponentData.getClassName()).thenReturn("MyService");
        when(mockComponentData.getInternalName()).thenReturn("MyService");
        when(mockComponentData.getInterceptors()).thenReturn(Collections.emptyList());
        when(mockComponentData.getTypeMirror()).thenReturn(mockTypeMirror);

        // When
        node = new AopComponentNodeImpl(mockComponentData);

        // Then
        assertEquals("MyService", node.getClassName());
        assertEquals("MyService", node.getInternalName());
        assertEquals("MyService", node.getSimpleName());
        assertEquals("", node.getPackageName());
        assertFalse(node.hasInterceptors());
    }

    @Test
    @DisplayName("Should handle class in default package")
    void shouldHandleClassInDefaultPackage() {
        // Given
        when(mockComponentData.getClassName()).thenReturn("DefaultPackageClass");
        when(mockComponentData.getInternalName()).thenReturn("DefaultPackageClass");
        when(mockComponentData.getInterceptors()).thenReturn(null);
        when(mockComponentData.getTypeMirror()).thenReturn(mockTypeMirror);

        // When
        node = new AopComponentNodeImpl(mockComponentData);

        // Then
        assertEquals("DefaultPackageClass", node.getSimpleName());
        assertEquals("", node.getPackageName());
        assertFalse(node.hasInterceptors());
    }

    @Test
    @DisplayName("Should return empty interceptor list correctly")
    void shouldReturnEmptyInterceptorListCorrectly() {
        // Given
        when(mockComponentData.getClassName()).thenReturn("com.example.NoInterceptors");
        when(mockComponentData.getInternalName()).thenReturn("com/example/NoInterceptors");
        when(mockComponentData.getInterceptors()).thenReturn(Collections.emptyList());
        when(mockComponentData.getTypeMirror()).thenReturn(mockTypeMirror);

        // When
        node = new AopComponentNodeImpl(mockComponentData);

        // Then
        assertTrue(node.getInterceptors().isEmpty());
        assertFalse(node.hasInterceptors());
    }

    @Test
    @DisplayName("Should generate correct toString representation")
    void shouldGenerateCorrectToStringRepresentation() {
        // Given
        when(mockComponentData.getClassName()).thenReturn("com.example.TestService");
        when(mockComponentData.getInternalName()).thenReturn("com/example/TestService");
        when(mockComponentData.getInterceptors()).thenReturn(Collections.singletonList("LoggingInterceptor"));
        when(mockComponentData.getTypeMirror()).thenReturn(mockTypeMirror);

        // When
        node = new AopComponentNodeImpl(mockComponentData);
        String toString = node.toString();

        // Then
        assertTrue(toString.contains("AopComponentNodeImpl"));
        assertTrue(toString.contains("className='com.example.TestService'"));
        assertTrue(toString.contains("simpleName='TestService'"));
        assertTrue(toString.contains("packageName='com.example'"));
    }

    @Test
    @DisplayName("Should check equality based on className")
    void shouldCheckEqualityBasedOnClassName() {
        // Given
        when(mockComponentData.getClassName()).thenReturn("com.example.SameClass");
        when(mockComponentData.getInternalName()).thenReturn("com/example/SameClass");
        when(mockComponentData.getInterceptors()).thenReturn(Collections.emptyList());
        when(mockComponentData.getTypeMirror()).thenReturn(mockTypeMirror);

        AopComponentNodeImpl node1 = new AopComponentNodeImpl(mockComponentData);

        SpiAopExtensionExecutor.ComponentData mockComponentData2 = mock(SpiAopExtensionExecutor.ComponentData.class);
        when(mockComponentData2.getClassName()).thenReturn("com.example.SameClass");

        AopComponentNodeImpl node2 = new AopComponentNodeImpl(mockComponentData2);

        // Then
        assertEquals(node1, node2);
        assertEquals(node1.hashCode(), node2.hashCode());
    }

    @Test
    @DisplayName("Should not be equal to different class names")
    void shouldNotBeEqualToDifferentClassNames() {
        // Given
        when(mockComponentData.getClassName()).thenReturn("com.example.ClassA");
        when(mockComponentData.getInternalName()).thenReturn("com/example/ClassA");
        when(mockComponentData.getInterceptors()).thenReturn(Collections.emptyList());
        when(mockComponentData.getTypeMirror()).thenReturn(mockTypeMirror);

        AopComponentNodeImpl node1 = new AopComponentNodeImpl(mockComponentData);

        SpiAopExtensionExecutor.ComponentData mockComponentData2 = mock(SpiAopExtensionExecutor.ComponentData.class);
        when(mockComponentData2.getClassName()).thenReturn("com.example.ClassB");

        AopComponentNodeImpl node2 = new AopComponentNodeImpl(mockComponentData2);

        // Then
        assertNotEquals(node1, node2);
    }

    @Test
    @DisplayName("Should not be equal to null or other types")
    void shouldNotBeEqualToNullOrOtherTypes() {
        // Given
        when(mockComponentData.getClassName()).thenReturn("com.example.MyService");
        when(mockComponentData.getInternalName()).thenReturn("com/example/MyService");
        when(mockComponentData.getInterceptors()).thenReturn(Collections.emptyList());
        when(mockComponentData.getTypeMirror()).thenReturn(mockTypeMirror);

        node = new AopComponentNodeImpl(mockComponentData);

        // Then
        assertNotEquals(null, node);
        assertNotEquals("String", node);
    }

    @Test
    @DisplayName("Should be equal to itself")
    void shouldBeEqualToItself() {
        // Given
        when(mockComponentData.getClassName()).thenReturn("com.example.MyService");
        when(mockComponentData.getInternalName()).thenReturn("com/example/MyService");
        when(mockComponentData.getInterceptors()).thenReturn(Collections.emptyList());
        when(mockComponentData.getTypeMirror()).thenReturn(mockTypeMirror);

        node = new AopComponentNodeImpl(mockComponentData);

        // Then
        assertEquals(node, node);
    }

    @Test
    @DisplayName("Should handle nested class names correctly")
    void shouldHandleNestedClassNamesCorrectly() {
        // Given
        when(mockComponentData.getClassName()).thenReturn("com.example.OuterClass.InnerClass");
        when(mockComponentData.getInternalName()).thenReturn("com/example/OuterClass$InnerClass");
        when(mockComponentData.getInterceptors()).thenReturn(Collections.emptyList());
        when(mockComponentData.getTypeMirror()).thenReturn(mockTypeMirror);

        // When
        node = new AopComponentNodeImpl(mockComponentData);

        // Then
        assertEquals("com.example.OuterClass.InnerClass", node.getClassName());
        assertEquals("com/example/OuterClass$InnerClass", node.getInternalName());
        assertEquals("InnerClass", node.getSimpleName());
        assertEquals("com.example.OuterClass", node.getPackageName());
    }
}
