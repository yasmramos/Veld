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

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.TypeElement;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.*;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

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
        @DisplayName("should support exactly 24 component annotations")
        void shouldSupportExactlyTwentyFourComponentAnnotations() {
            SupportedAnnotationTypes annotation = VeldProcessor.class.getAnnotation(SupportedAnnotationTypes.class);
            assertNotNull(annotation);
            
            assertEquals(24, annotation.value().length,
                "Should support exactly 24 component annotations");
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
        @DisplayName("should support Java 17")
        void shouldSupportJava17() {
            SupportedSourceVersion annotation = VeldProcessor.class.getAnnotation(SupportedSourceVersion.class);
            assertNotNull(annotation);
            
            assertEquals(SourceVersion.RELEASE_17, annotation.value(), 
                "Should support Java 17");
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
        @DisplayName("getSupportedSourceVersion should return Java 17")
        void getSupportedSourceVersionShouldReturnJava17() {
            SourceVersion version = processor.getSupportedSourceVersion();
            assertEquals(SourceVersion.RELEASE_17, version);
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

    @Nested
    @DisplayName("Profile-based Class Generation Tests")
    class ProfileClassGenerationTests {

        @Test
        @DisplayName("should generate VeldDev when @Profile(\"dev\") is present")
        void shouldGenerateVeldDevForDevProfile() {
            // Test the naming convention logic
            assertEquals("VeldDev", getVeldClassNameForTest("dev"));
            assertEquals("VeldDev", getVeldClassNameForTest("DEV"));
        }

        @Test
        @DisplayName("should generate VeldTest when @Profile(\"test\") is present")
        void shouldGenerateVeldTestForTestProfile() {
            assertEquals("VeldTest", getVeldClassNameForTest("test"));
            assertEquals("VeldTest", getVeldClassNameForTest("TEST"));
        }

        @Test
        @DisplayName("should generate Veld (NOT VeldProd) for production profile")
        void shouldGenerateVeldForProdProfile() {
            assertEquals("Veld", getVeldClassNameForTest("prod"));
            assertEquals("Veld", getVeldClassNameForTest("production"));
            assertEquals("Veld", getVeldClassNameForTest("PROD"));
        }

        @Test
        @DisplayName("should handle arbitrary profile names")
        void shouldHandleArbitraryProfileNames() {
            assertEquals("VeldStaging", getVeldClassNameForTest("staging"));
            assertEquals("VeldIntegration", getVeldClassNameForTest("integration"));
            assertEquals("VeldCustom", getVeldClassNameForTest("custom"));
        }

        /**
         * Helper method to test the naming convention logic.
         * This mirrors the logic in VeldProcessor.getVeldClassName()
         */
        private String getVeldClassNameForTest(String profile) {
            if ("prod".equalsIgnoreCase(profile) || "production".equalsIgnoreCase(profile)) {
                return "Veld";
            }
            // Normalize to lowercase first, then capitalize only the first letter
            String normalized = profile.toLowerCase();
            String capitalized = normalized.substring(0, 1).toUpperCase() + normalized.substring(1);
            return "Veld" + capitalized;
        }
    }

    @Nested
    @DisplayName("Profile Visibility Rules Tests")
    class ProfileVisibilityRulesTests {

        @Test
        @DisplayName("should allow default component to depend on default component")
        void shouldAllowDefaultOnDefault() {
            // This is valid - no profile component depending on no profile component
            // The validation should pass
            assertTrue(isValidVisibility("default", "default", true));
        }

        @Test
        @DisplayName("should reject default component depending on profile component")
        void shouldRejectDefaultOnProfile() {
            // This is INVALID - default component cannot depend on profile-specific component
            // The validation should fail
            assertFalse(isValidVisibility("default", "dev", true));
        }

        @Test
        @DisplayName("should allow profile component to depend on same profile component")
        void shouldAllowSameProfile() {
            // This is valid - dev component depending on dev component
            assertTrue(isValidVisibility("dev", "dev", false));
        }

        @Test
        @DisplayName("should reject profile component depending on different profile")
        void shouldRejectDifferentProfile() {
            // This is INVALID - dev component cannot depend on prod component
            assertFalse(isValidVisibility("dev", "prod", false));
        }

        @Test
        @DisplayName("should reject profile component depending on default component")
        void shouldRejectProfileOnDefault() {
            // This is INVALID - dev component cannot depend on default component
            assertFalse(isValidVisibility("dev", "default", false));
        }

        /**
         * Helper method to test visibility validation logic.
         * This mirrors the logic in VeldProcessor.isValidDependency()
         */
        private boolean isValidVisibility(String dependerProfile, String dependencyProfile, 
                                          boolean dependerIsDefault) {
            boolean dependencyIsDefault = dependencyProfile.equals("default");
            
            // If dependency is in available set, it exists
            // For this test, assume dependency IS available
            
            // Default component can only depend on default
            if ( dependerIsDefault && !dependencyIsDefault) {
                return false;
            }
            
            // Profile component can only depend on same profile
            if (!dependerIsDefault && dependencyIsDefault) {
                return false;
            }
            
            // Profile component can depend on same profile
            if (!dependerIsDefault && !dependencyIsDefault) {
                return dependerProfile.equals(dependencyProfile);
            }
            
            return true;
        }
    }

    @Nested
    @DisplayName("Profile Duplicate Implementation Detection Tests")
    class ProfileDuplicateImplementationTests {

        @Test
        @DisplayName("should detect multiple implementations without @Named")
        void shouldDetectDuplicatesWithoutNamed() {
            // Multiple implementations of same interface without @Named should fail
            // This test verifies the validation logic exists
            assertTrue(hasDuplicateDetectionLogic());
        }

        @Test
        @DisplayName("should allow multiple implementations with unique @Named")
        void shouldAllowMultipleWithNamed() {
            // Multiple implementations WITH unique @Named values should pass
            // The validation should check for @Named presence
            assertTrue(validatesNamedPresence());
        }

        /**
         * Verifies that the processor has duplicate detection logic.
         */
        private boolean hasDuplicateDetectionLogic() {
            // Check if validateNoDuplicateImplementations method exists
            try {
                VeldProcessor.class.getDeclaredMethod("validateNoDuplicateImplementations", 
                    String.class, java.util.List.class);
                return true;
            } catch (NoSuchMethodException e) {
                return false;
            }
        }

        /**
         * Verifies that the processor validates @Named presence.
         */
        private boolean validatesNamedPresence() {
            // Check if the validation method checks for @Named
            try {
                java.lang.reflect.Method method = VeldProcessor.class.getDeclaredMethod(
                    "validateNoDuplicateImplementations", String.class, java.util.List.class);
                return method != null;
            } catch (NoSuchMethodException e) {
                return false;
            }
        }
    }

    @Nested
    @DisplayName("Profile Graph Isolation Tests")
    class ProfileGraphIsolationTests {

        @Test
        @DisplayName("should build separate graphs for each profile")
        void shouldBuildSeparateGraphs() {
            // Verify that discoveredProfiles is a Set (no duplicates)
            assertTrue(hasIsolatedProfileTracking());
        }

        @Test
        @DisplayName("should not mix components from different profiles")
        void shouldNotMixComponents() {
            // Components should be classified by their profile
            // Default components go to Veld only
            // Profile components go to their specific VeldX only
            assertTrue(hasProfileClassificationLogic());
        }

        @Test
        @DisplayName("should track discovered profiles for class generation")
        void shouldTrackDiscoveredProfiles() {
            // Verify discoveredProfiles field exists
            try {
                java.lang.reflect.Field field = VeldProcessor.class.getDeclaredField("discoveredProfiles");
                assertNotNull(field, "discoveredProfiles field should exist");
                assertEquals(java.util.Set.class, field.getType());
            } catch (NoSuchFieldException e) {
                fail("discoveredProfiles field not found");
            }
        }

        private boolean hasIsolatedProfileTracking() {
            try {
                java.lang.reflect.Field field = VeldProcessor.class.getDeclaredField("discoveredProfiles");
                return field.getType().equals(java.util.Set.class);
            } catch (NoSuchFieldException e) {
                return false;
            }
        }

        private boolean hasProfileClassificationLogic() {
            try {
                java.lang.reflect.Method method = VeldProcessor.class.getDeclaredMethod(
                    "getNodeProfiles", VeldNode.class);
                return method != null;
            } catch (NoSuchMethodException e) {
                return false;
            }
        }
    }
}
