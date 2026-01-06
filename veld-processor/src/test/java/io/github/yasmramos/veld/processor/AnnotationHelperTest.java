package io.github.yasmramos.veld.processor;

import io.github.yasmramos.veld.processor.AnnotationHelper.InjectSource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Name;
import javax.lang.model.type.DeclaredType;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AnnotationHelper class.
 * Tests the detection and handling of JSR-330, Jakarta, and Veld annotations.
 */
@DisplayName("AnnotationHelper Tests")
class AnnotationHelperTest {
    
    @Nested
    @DisplayName("InjectSource Enum Tests")
    class InjectSourceTests {
        
        @Test
        @DisplayName("VELD source should have correct package")
        void veldSourceHasCorrectPackage() {
            assertEquals("io.github.yasmramos.veld.annotation", InjectSource.VELD.getPackageName());
        }
        
        @Test
        @DisplayName("JAVAX source should have correct package")
        void javaxSourceHasCorrectPackage() {
            assertEquals("javax.inject", InjectSource.JAVAX.getPackageName());
        }
        
        @Test
        @DisplayName("JAKARTA source should have correct package")
        void jakartaSourceHasCorrectPackage() {
            assertEquals("jakarta.inject", InjectSource.JAKARTA.getPackageName());
        }
        
        @Test
        @DisplayName("NONE source should have null package")
        void noneSourceHasNullPackage() {
            assertNull(InjectSource.NONE.getPackageName());
        }
        
        @Test
        @DisplayName("JAVAX and JAKARTA should be standard")
        void standardSourcesAreCorrectlyIdentified() {
            assertTrue(InjectSource.JAVAX.isStandard());
            assertTrue(InjectSource.JAKARTA.isStandard());
            assertFalse(InjectSource.VELD.isStandard());
            assertFalse(InjectSource.NONE.isStandard());
        }
    }
    
    @Nested
    @DisplayName("Provider Type Detection Tests")
    class ProviderTypeTests {
        
        @Test
        @DisplayName("Should detect javax.inject.Provider")
        void shouldDetectJavaxProvider() {
            assertTrue(AnnotationHelper.isProviderType("javax.inject.Provider"));
        }
        
        @Test
        @DisplayName("Should detect jakarta.inject.Provider")
        void shouldDetectJakartaProvider() {
            assertTrue(AnnotationHelper.isProviderType("jakarta.inject.Provider"));
        }
        
        @Test
        @DisplayName("Should not detect non-Provider types")
        void shouldNotDetectNonProviderTypes() {
            assertFalse(AnnotationHelper.isProviderType("java.util.function.Supplier"));
            assertFalse(AnnotationHelper.isProviderType("com.custom.Provider"));
            assertFalse(AnnotationHelper.isProviderType("Provider"));
        }
        
        @Test
        @DisplayName("Should handle null and empty strings")
        void shouldHandleNullAndEmpty() {
            assertFalse(AnnotationHelper.isProviderType(null));
            assertFalse(AnnotationHelper.isProviderType(""));
        }
    }
    
    @Nested
    @DisplayName("Inject Annotation Detection Tests")
    class InjectAnnotationTests {
        
        @Test
        @DisplayName("Should detect Veld @Inject annotation")
        void shouldDetectVeldInject() {
            Element element = createMockElementWithAnnotation("io.github.yasmramos.veld.annotation.Inject");
            assertTrue(AnnotationHelper.hasInjectAnnotation(element));
        }
        
        @Test
        @DisplayName("Should detect javax @Inject annotation")
        void shouldDetectJavaxInject() {
            Element element = createMockElementWithAnnotation("javax.inject.Inject");
            assertTrue(AnnotationHelper.hasInjectAnnotation(element));
        }
        
        @Test
        @DisplayName("Should detect jakarta @Inject annotation")
        void shouldDetectJakartaInject() {
            Element element = createMockElementWithAnnotation("jakarta.inject.Inject");
            assertTrue(AnnotationHelper.hasInjectAnnotation(element));
        }
        
        @Test
        @DisplayName("Should return false when no @Inject annotation")
        void shouldReturnFalseWhenNoInject() {
            Element element = createMockElementWithAnnotation("java.lang.Override");
            assertFalse(AnnotationHelper.hasInjectAnnotation(element));
        }
        
        @Test
        @DisplayName("Should return false for empty annotations")
        void shouldReturnFalseForEmptyAnnotations() {
            Element element = mock(Element.class);
            when(element.getAnnotationMirrors()).thenReturn(Collections.emptyList());
            assertFalse(AnnotationHelper.hasInjectAnnotation(element));
        }
    }
    
    @Nested
    @DisplayName("Singleton Annotation Detection Tests")
    class SingletonAnnotationTests {
        
        @Test
        @DisplayName("Should detect Veld @Singleton annotation")
        void shouldDetectVeldSingleton() {
            Element element = createMockElementWithAnnotation("io.github.yasmramos.veld.annotation.Singleton");
            assertTrue(AnnotationHelper.hasSingletonAnnotation(element));
        }
        
        @Test
        @DisplayName("Should detect javax @Singleton annotation")
        void shouldDetectJavaxSingleton() {
            Element element = createMockElementWithAnnotation("javax.inject.Singleton");
            assertTrue(AnnotationHelper.hasSingletonAnnotation(element));
        }
        
        @Test
        @DisplayName("Should detect jakarta @Singleton annotation")
        void shouldDetectJakartaSingleton() {
            Element element = createMockElementWithAnnotation("jakarta.inject.Singleton");
            assertTrue(AnnotationHelper.hasSingletonAnnotation(element));
        }
        
        @Test
        @DisplayName("Should return false when no @Singleton annotation")
        void shouldReturnFalseWhenNoSingleton() {
            Element element = createMockElementWithAnnotation("java.lang.Deprecated");
            assertFalse(AnnotationHelper.hasSingletonAnnotation(element));
        }
    }
    
    @Nested
    @DisplayName("InjectSource Detection Tests")
    class InjectSourceDetectionTests {
        
        @Test
        @DisplayName("Should return VELD for Veld @Inject")
        void shouldReturnVeldSource() {
            Element element = createMockElementWithAnnotation("io.github.yasmramos.veld.annotation.Inject");
            assertEquals(InjectSource.VELD, AnnotationHelper.getInjectSource(element));
        }
        
        @Test
        @DisplayName("Should return JAVAX for javax @Inject")
        void shouldReturnJavaxSource() {
            Element element = createMockElementWithAnnotation("javax.inject.Inject");
            assertEquals(InjectSource.JAVAX, AnnotationHelper.getInjectSource(element));
        }
        
        @Test
        @DisplayName("Should return JAKARTA for jakarta @Inject")
        void shouldReturnJakartaSource() {
            Element element = createMockElementWithAnnotation("jakarta.inject.Inject");
            assertEquals(InjectSource.JAKARTA, AnnotationHelper.getInjectSource(element));
        }
        
        @Test
        @DisplayName("Should return NONE when no @Inject annotation")
        void shouldReturnNoneSource() {
            Element element = createMockElementWithAnnotation("java.lang.Override");
            assertEquals(InjectSource.NONE, AnnotationHelper.getInjectSource(element));
        }
    }
    
    @Nested
    @DisplayName("Named Value Extraction Tests")
    class NamedValueTests {
        
        @Test
        @DisplayName("Should extract Veld @Named value")
        void shouldExtractVeldNamedValue() {
            Element element = createMockElementWithAnnotationValue(
                "io.github.yasmramos.veld.annotation.Named", "value", "myBean");
            Optional<String> result = AnnotationHelper.getNamedValue(element);
            assertTrue(result.isPresent());
            assertEquals("myBean", result.get());
        }
        
        @Test
        @DisplayName("Should extract javax @Named value")
        void shouldExtractJavaxNamedValue() {
            Element element = createMockElementWithAnnotationValue(
                "javax.inject.Named", "value", "service");
            Optional<String> result = AnnotationHelper.getNamedValue(element);
            assertTrue(result.isPresent());
            assertEquals("service", result.get());
        }
        
        @Test
        @DisplayName("Should extract jakarta @Named value")
        void shouldExtractJakartaNamedValue() {
            Element element = createMockElementWithAnnotationValue(
                "jakarta.inject.Named", "value", "repository");
            Optional<String> result = AnnotationHelper.getNamedValue(element);
            assertTrue(result.isPresent());
            assertEquals("repository", result.get());
        }
        
        @Test
        @DisplayName("Should return empty when no @Named annotation")
        void shouldReturnEmptyWhenNoNamed() {
            Element element = createMockElementWithAnnotation("java.lang.Override");
            Optional<String> result = AnnotationHelper.getNamedValue(element);
            assertFalse(result.isPresent());
        }
        
        @Test
        @DisplayName("Should return empty for empty value")
        void shouldReturnEmptyForEmptyValue() {
            Element element = createMockElementWithAnnotationValue(
                "javax.inject.Named", "value", "");
            Optional<String> result = AnnotationHelper.getNamedValue(element);
            assertFalse(result.isPresent());
        }
    }
    
    @Nested
    @DisplayName("Qualifier Value Extraction Tests")
    class QualifierValueTests {
        
        @Test
        @DisplayName("Should return @Named value as qualifier")
        void shouldReturnNamedValueAsQualifier() {
            Element element = createMockElementWithAnnotationValue(
                "javax.inject.Named", "value", "qualifier");
            Optional<String> result = AnnotationHelper.getQualifierValue(element);
            assertTrue(result.isPresent());
            assertEquals("qualifier", result.get());
        }
        
        @Test
        @DisplayName("Should return empty when no qualifier")
        void shouldReturnEmptyWhenNoQualifier() {
            Element element = mock(Element.class);
            when(element.getAnnotationMirrors()).thenReturn(Collections.emptyList());
            Optional<String> result = AnnotationHelper.getQualifierValue(element);
            assertFalse(result.isPresent());
        }
    }
    
    @Nested
    @DisplayName("All InjectSource Enum Values Tests")
    class AllInjectSourceValuesTests {
        
        @Test
        @DisplayName("Should have exactly 4 enum values")
        void shouldHaveExactlyFourValues() {
            assertEquals(4, InjectSource.values().length);
        }
        
        @Test
        @DisplayName("Should be able to get enum by name")
        void shouldGetEnumByName() {
            assertEquals(InjectSource.VELD, InjectSource.valueOf("VELD"));
            assertEquals(InjectSource.JAVAX, InjectSource.valueOf("JAVAX"));
            assertEquals(InjectSource.JAKARTA, InjectSource.valueOf("JAKARTA"));
            assertEquals(InjectSource.NONE, InjectSource.valueOf("NONE"));
        }
    }
    
    // Helper methods for creating mock elements
    
    @SuppressWarnings("unchecked")
    private Element createMockElementWithAnnotation(String annotationName) {
        Element element = mock(Element.class);
        AnnotationMirror mirror = mock(AnnotationMirror.class);
        DeclaredType declaredType = mock(DeclaredType.class);
        
        when(declaredType.toString()).thenReturn(annotationName);
        when(mirror.getAnnotationType()).thenReturn(declaredType);
        doReturn(List.of(mirror)).when(element).getAnnotationMirrors();
        
        return element;
    }
    
    @SuppressWarnings("unchecked")
    private Element createMockElementWithAnnotationValue(String annotationName, String attrName, String attrValue) {
        Element element = mock(Element.class);
        AnnotationMirror mirror = mock(AnnotationMirror.class);
        DeclaredType declaredType = mock(DeclaredType.class);
        ExecutableElement key = mock(ExecutableElement.class);
        AnnotationValue value = mock(AnnotationValue.class);
        Name name = mock(Name.class);
        
        when(declaredType.toString()).thenReturn(annotationName);
        when(mirror.getAnnotationType()).thenReturn(declaredType);
        when(name.toString()).thenReturn(attrName);
        when(key.getSimpleName()).thenReturn(name);
        when(value.getValue()).thenReturn(attrValue);
        doReturn(Map.of(key, value)).when(mirror).getElementValues();
        doReturn(List.of(mirror)).when(element).getAnnotationMirrors();
        
        return element;
    }
}
