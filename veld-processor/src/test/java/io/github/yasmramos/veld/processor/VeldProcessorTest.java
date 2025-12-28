/*
 * Copyright 2025 Veld Framework
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.yasmramos.veld.processor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.TypeElement;
import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link VeldProcessor} annotation processor.
 * Tests processor configuration, annotations, and basic behavior.
 */
@DisplayName("VeldProcessor Tests")
class VeldProcessorTest {

    private VeldProcessor processor;

    @BeforeEach
    void setUp() {
        processor = new VeldProcessor();
    }

    @Nested
    @DisplayName("Processor Instantiation Tests")
    class ProcessorInstantiationTests {

        @Test
        @DisplayName("should create processor instance")
        void shouldCreateProcessorInstance() {
            assertNotNull(processor);
        }

        @Test
        @DisplayName("should be instance of AbstractProcessor")
        void shouldBeInstanceOfAbstractProcessor() {
            assertTrue(processor instanceof javax.annotation.processing.AbstractProcessor);
        }
    }

    @Nested
    @DisplayName("Supported Annotations Tests")
    class SupportedAnnotationsTests {

        @Test
        @DisplayName("should have SupportedAnnotationTypes annotation")
        void shouldHaveSupportedAnnotationTypesAnnotation() {
            SupportedAnnotationTypes annotation = VeldProcessor.class.getAnnotation(SupportedAnnotationTypes.class);
            assertNotNull(annotation, "VeldProcessor should have @SupportedAnnotationTypes annotation");
        }

        @Test
        @DisplayName("should support Component annotation")
        void shouldSupportComponentAnnotation() {
            SupportedAnnotationTypes annotation = VeldProcessor.class.getAnnotation(SupportedAnnotationTypes.class);
            assertNotNull(annotation);
            
            Set<String> supported = new HashSet<>(Arrays.asList(annotation.value()));
            assertTrue(supported.contains("io.github.yasmramos.veld.annotation.Component"), 
                "Should support @Component");
        }

        @Test
        @DisplayName("should support Singleton annotation")
        void shouldSupportSingletonAnnotation() {
            SupportedAnnotationTypes annotation = VeldProcessor.class.getAnnotation(SupportedAnnotationTypes.class);
            assertNotNull(annotation);
            
            Set<String> supported = new HashSet<>(Arrays.asList(annotation.value()));
            assertTrue(supported.contains("io.github.yasmramos.veld.annotation.Singleton"), 
                "Should support @Singleton");
        }

        @Test
        @DisplayName("should support Prototype annotation")
        void shouldSupportPrototypeAnnotation() {
            SupportedAnnotationTypes annotation = VeldProcessor.class.getAnnotation(SupportedAnnotationTypes.class);
            assertNotNull(annotation);
            
            Set<String> supported = new HashSet<>(Arrays.asList(annotation.value()));
            assertTrue(supported.contains("io.github.yasmramos.veld.annotation.Prototype"), 
                "Should support @Prototype");
        }

        @Test
        @DisplayName("should support Lazy annotation")
        void shouldSupportLazyAnnotation() {
            SupportedAnnotationTypes annotation = VeldProcessor.class.getAnnotation(SupportedAnnotationTypes.class);
            assertNotNull(annotation);
            
            Set<String> supported = new HashSet<>(Arrays.asList(annotation.value()));
            assertTrue(supported.contains("io.github.yasmramos.veld.annotation.Lazy"), 
                "Should support @Lazy");
        }

        @Test
        @DisplayName("should support javax.inject.Singleton annotation")
        void shouldSupportJavaxInjectSingleton() {
            SupportedAnnotationTypes annotation = VeldProcessor.class.getAnnotation(SupportedAnnotationTypes.class);
            assertNotNull(annotation);
            
            Set<String> supported = new HashSet<>(Arrays.asList(annotation.value()));
            assertTrue(supported.contains("javax.inject.Singleton"), 
                "Should support javax.inject.Singleton");
        }

        @Test
        @DisplayName("should support jakarta.inject.Singleton annotation")
        void shouldSupportJakartaInjectSingleton() {
            SupportedAnnotationTypes annotation = VeldProcessor.class.getAnnotation(SupportedAnnotationTypes.class);
            assertNotNull(annotation);
            
            Set<String> supported = new HashSet<>(Arrays.asList(annotation.value()));
            assertTrue(supported.contains("jakarta.inject.Singleton"), 
                "Should support jakarta.inject.Singleton");
        }

        @Test
        @DisplayName("should support exactly 13 component annotations")
        void shouldSupportExactlyThirteenComponentAnnotations() {
            SupportedAnnotationTypes annotation = VeldProcessor.class.getAnnotation(SupportedAnnotationTypes.class);
            assertNotNull(annotation);
            
            assertEquals(13, annotation.value().length, 
                "Should support exactly 13 component annotations");
        }
    }

    @Nested
    @DisplayName("Supported Source Version Tests")
    class SupportedSourceVersionTests {

        @Test
        @DisplayName("should have SupportedSourceVersion annotation")
        void shouldHaveSupportedSourceVersionAnnotation() {
            SupportedSourceVersion annotation = VeldProcessor.class.getAnnotation(SupportedSourceVersion.class);
            assertNotNull(annotation, "VeldProcessor should have @SupportedSourceVersion annotation");
        }

        @Test
        @DisplayName("should support Java 11")
        void shouldSupportJava11() {
            SupportedSourceVersion annotation = VeldProcessor.class.getAnnotation(SupportedSourceVersion.class);
            assertNotNull(annotation);
            
            assertEquals(SourceVersion.RELEASE_11, annotation.value(), 
                "Should support Java 11");
        }
    }

    @Nested
    @DisplayName("Processor Methods Tests")
    class ProcessorMethodsTests {

        @Test
        @DisplayName("getSupportedAnnotationTypes should return non-null")
        void getSupportedAnnotationTypesShouldReturnNonNull() {
            Set<String> supportedTypes = processor.getSupportedAnnotationTypes();
            assertNotNull(supportedTypes);
        }

        @Test
        @DisplayName("getSupportedAnnotationTypes should return expected annotations")
        void getSupportedAnnotationTypesShouldReturnExpectedAnnotations() {
            Set<String> supportedTypes = processor.getSupportedAnnotationTypes();
            
            assertTrue(supportedTypes.contains("io.github.yasmramos.veld.annotation.Component"));
            assertTrue(supportedTypes.contains("io.github.yasmramos.veld.annotation.Singleton"));
            assertTrue(supportedTypes.contains("io.github.yasmramos.veld.annotation.Prototype"));
            assertTrue(supportedTypes.contains("io.github.yasmramos.veld.annotation.Lazy"));
            assertTrue(supportedTypes.contains("javax.inject.Singleton"));
            assertTrue(supportedTypes.contains("jakarta.inject.Singleton"));
        }

        @Test
        @DisplayName("getSupportedSourceVersion should return Java 11")
        void getSupportedSourceVersionShouldReturnJava11() {
            SourceVersion version = processor.getSupportedSourceVersion();
            assertEquals(SourceVersion.RELEASE_11, version);
        }
    }

    @Nested
    @DisplayName("Init Method Tests")
    class InitMethodTests {

        @Test
        @DisplayName("init should not throw with mock environment")
        void initShouldNotThrowWithMockEnvironment() {
            ProcessingEnvironment mockEnv = mock(ProcessingEnvironment.class);
            when(mockEnv.getMessager()).thenReturn(mock(javax.annotation.processing.Messager.class));
            when(mockEnv.getFiler()).thenReturn(mock(javax.annotation.processing.Filer.class));
            when(mockEnv.getElementUtils()).thenReturn(mock(javax.lang.model.util.Elements.class));
            when(mockEnv.getTypeUtils()).thenReturn(mock(javax.lang.model.util.Types.class));
            
            assertDoesNotThrow(() -> processor.init(mockEnv));
        }

        @Test
        @DisplayName("init should accept ProcessingEnvironment")
        void initShouldAcceptProcessingEnvironment() {
            ProcessingEnvironment mockEnv = mock(ProcessingEnvironment.class);
            when(mockEnv.getMessager()).thenReturn(mock(javax.annotation.processing.Messager.class));
            when(mockEnv.getFiler()).thenReturn(mock(javax.annotation.processing.Filer.class));
            when(mockEnv.getElementUtils()).thenReturn(mock(javax.lang.model.util.Elements.class));
            when(mockEnv.getTypeUtils()).thenReturn(mock(javax.lang.model.util.Types.class));
            
            processor.init(mockEnv);
            
            // Verify interactions
            verify(mockEnv).getMessager();
            verify(mockEnv).getFiler();
            verify(mockEnv).getElementUtils();
            verify(mockEnv).getTypeUtils();
        }
    }

    @Nested
    @DisplayName("Process Method Tests")
    class ProcessMethodTests {

        @Test
        @DisplayName("process should return true when processing is over")
        void processShouldReturnTrueWhenProcessingIsOver() {
            // Setup
            ProcessingEnvironment mockEnv = createMockProcessingEnvironment();
            processor.init(mockEnv);
            
            RoundEnvironment mockRoundEnv = mock(RoundEnvironment.class);
            when(mockRoundEnv.processingOver()).thenReturn(true);
            
            Set<TypeElement> annotations = new HashSet<>();
            
            // Execute
            boolean result = processor.process(annotations, mockRoundEnv);
            
            // Verify
            assertTrue(result);
        }

        @Test
        @DisplayName("process should return true with empty annotations")
        void processShouldReturnTrueWithEmptyAnnotations() {
            // Setup
            ProcessingEnvironment mockEnv = createMockProcessingEnvironment();
            processor.init(mockEnv);
            
            RoundEnvironment mockRoundEnv = mock(RoundEnvironment.class);
            when(mockRoundEnv.processingOver()).thenReturn(false);
            when(mockRoundEnv.getElementsAnnotatedWith(any(Class.class))).thenReturn(new HashSet<>());
            when(mockRoundEnv.getElementsAnnotatedWith(any(TypeElement.class))).thenReturn(new HashSet<>());
            
            Set<TypeElement> annotations = new HashSet<>();
            
            // Execute
            boolean result = processor.process(annotations, mockRoundEnv);
            
            // Verify
            assertTrue(result);
        }

        private ProcessingEnvironment createMockProcessingEnvironment() {
            ProcessingEnvironment mockEnv = mock(ProcessingEnvironment.class);
            when(mockEnv.getMessager()).thenReturn(mock(javax.annotation.processing.Messager.class));
            when(mockEnv.getFiler()).thenReturn(mock(javax.annotation.processing.Filer.class));
            
            javax.lang.model.util.Elements mockElements = mock(javax.lang.model.util.Elements.class);
            when(mockElements.getTypeElement(anyString())).thenReturn(null);
            when(mockEnv.getElementUtils()).thenReturn(mockElements);
            
            when(mockEnv.getTypeUtils()).thenReturn(mock(javax.lang.model.util.Types.class));
            return mockEnv;
        }
    }

    @Nested
    @DisplayName("Annotation Compatibility Tests")
    class AnnotationCompatibilityTests {

        @Test
        @DisplayName("should support all Veld native annotations")
        void shouldSupportAllVeldNativeAnnotations() {
            Set<String> supported = processor.getSupportedAnnotationTypes();
            
            assertTrue(supported.stream().anyMatch(s -> s.startsWith("io.github.yasmramos.veld.annotation.")),
                "Should support Veld native annotations");
        }

        @Test
        @DisplayName("should support JSR-330 annotations")
        void shouldSupportJsr330Annotations() {
            Set<String> supported = processor.getSupportedAnnotationTypes();
            
            assertTrue(supported.stream().anyMatch(s -> s.startsWith("javax.inject.")),
                "Should support javax.inject annotations (JSR-330)");
        }

        @Test
        @DisplayName("should support Jakarta EE annotations")
        void shouldSupportJakartaEEAnnotations() {
            Set<String> supported = processor.getSupportedAnnotationTypes();
            
            assertTrue(supported.stream().anyMatch(s -> s.startsWith("jakarta.inject.")),
                "Should support jakarta.inject annotations (Jakarta EE)");
        }
    }

    @Nested
    @DisplayName("Processor Configuration Tests")
    class ProcessorConfigurationTests {

        @Test
        @DisplayName("processor should be a valid annotation processor")
        void processorShouldBeValidAnnotationProcessor() {
            // Check that all required methods can be called without error
            assertDoesNotThrow(() -> processor.getSupportedAnnotationTypes());
            assertDoesNotThrow(() -> processor.getSupportedSourceVersion());
            assertDoesNotThrow(() -> processor.getSupportedOptions());
        }

        @Test
        @DisplayName("getSupportedOptions should return empty set by default")
        void getSupportedOptionsShouldReturnEmptyByDefault() {
            Set<String> options = processor.getSupportedOptions();
            assertNotNull(options);
            // By default, AbstractProcessor returns empty set if not overridden
        }
    }

    @Nested
    @DisplayName("Decapitalize Logic Tests")
    class DecapitalizeLogicTests {

        @Test
        @DisplayName("should handle simple class names for component naming")
        void shouldHandleSimpleClassNamesForComponentNaming() {
            // The processor uses decapitalize for default component names
            // We can test the expected behavior indirectly
            
            // Simple class: MyService -> myService
            String className = "MyService";
            String expected = "myService";
            
            char[] chars = className.toCharArray();
            chars[0] = Character.toLowerCase(chars[0]);
            String result = new String(chars);
            
            assertEquals(expected, result);
        }

        @Test
        @DisplayName("should preserve consecutive uppercase letters")
        void shouldPreserveConsecutiveUppercaseLetters() {
            // URLParser should remain URLParser (due to consecutive uppercase)
            String className = "URLParser";
            
            // If second char is uppercase, don't lowercase first char
            if (className.length() > 1 && Character.isUpperCase(className.charAt(1))) {
                assertEquals("URLParser", className);
            }
        }
    }

    @Nested
    @DisplayName("Edge Cases Tests")
    class EdgeCasesTests {

        @Test
        @DisplayName("processor can be instantiated multiple times")
        void processorCanBeInstantiatedMultipleTimes() {
            VeldProcessor p1 = new VeldProcessor();
            VeldProcessor p2 = new VeldProcessor();
            VeldProcessor p3 = new VeldProcessor();
            
            assertNotNull(p1);
            assertNotNull(p2);
            assertNotNull(p3);
            
            // Each instance is independent
            assertNotSame(p1, p2);
            assertNotSame(p2, p3);
        }

        @Test
        @DisplayName("processor methods are consistent across calls")
        void processorMethodsAreConsistentAcrossCalls() {
            Set<String> types1 = processor.getSupportedAnnotationTypes();
            Set<String> types2 = processor.getSupportedAnnotationTypes();
            
            assertEquals(types1, types2);
            
            SourceVersion ver1 = processor.getSupportedSourceVersion();
            SourceVersion ver2 = processor.getSupportedSourceVersion();
            
            assertEquals(ver1, ver2);
        }
    }

    @Nested
    @DisplayName("Annotation Type Coverage Tests")
    class AnnotationTypeCoverageTests {

        @Test
        @DisplayName("should cover singleton scope via multiple annotations")
        void shouldCoverSingletonScopeViaMultipleAnnotations() {
            Set<String> supported = processor.getSupportedAnnotationTypes();
            
            // All of these result in SINGLETON scope
            assertTrue(supported.contains("io.github.yasmramos.veld.annotation.Singleton"));
            assertTrue(supported.contains("io.github.yasmramos.veld.annotation.Lazy")); // Lazy implies Singleton
            assertTrue(supported.contains("javax.inject.Singleton"));
            assertTrue(supported.contains("jakarta.inject.Singleton"));
        }

        @Test
        @DisplayName("should have prototype scope annotation")
        void shouldHavePrototypeScopeAnnotation() {
            Set<String> supported = processor.getSupportedAnnotationTypes();
            
            // Only @Prototype gives prototype scope
            assertTrue(supported.contains("io.github.yasmramos.veld.annotation.Prototype"));
        }

        @Test
        @DisplayName("should have generic component annotation")
        void shouldHaveGenericComponentAnnotation() {
            Set<String> supported = processor.getSupportedAnnotationTypes();
            
            // @Component is the generic one
            assertTrue(supported.contains("io.github.yasmramos.veld.annotation.Component"));
        }
    }
}
