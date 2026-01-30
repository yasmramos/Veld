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
package io.github.yasmramos.veld.aop;

import org.junit.jupiter.api.*;

import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests for {@link AopClassGenerator}.
 */
@DisplayName("AopClassGenerator")
class AopClassGeneratorTest {

    private AopClassGenerator generator;

    @BeforeEach
    void setUp() {
        generator = new AopClassGenerator();
    }

    // Minimal implementation of AopComponentNode for testing
    private static class TestComponentNode implements AopComponentNode {
        private final String className;
        private final String packageName;
        private final String simpleName;
        private final String internalName;
        private final TypeMirror typeMirror;
        private final List<String> interceptors;
        private final boolean hasInterceptorsFlag;

        TestComponentNode(String className, String packageName, String simpleName,
                          String internalName, TypeMirror typeMirror,
                          List<String> interceptors, boolean hasInterceptorsFlag) {
            this.className = className;
            this.packageName = packageName;
            this.simpleName = simpleName;
            this.internalName = internalName;
            this.typeMirror = typeMirror;
            this.interceptors = interceptors;
            this.hasInterceptorsFlag = hasInterceptorsFlag;
        }

        @Override
        public String getClassName() {
            return className;
        }

        @Override
        public String getInternalName() {
            return internalName;
        }

        @Override
        public String getSimpleName() {
            return simpleName;
        }

        @Override
        public String getPackageName() {
            return packageName;
        }

        @Override
        public List<String> getInterceptors() {
            return interceptors != null ? interceptors : Collections.emptyList();
        }

        @Override
        public boolean hasInterceptors() {
            return hasInterceptorsFlag;
        }

        @Override
        public TypeMirror getTypeMirror() {
            return typeMirror;
        }

        @Override
        public String toString() {
            return "AopComponentNode{" +
                    "className='" + className + '\'' +
                    ", simpleName='" + simpleName + '\'' +
                    '}';
        }
    }

    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {

        @Test
        @DisplayName("should create generator with null context")
        void shouldCreateWithNullContext() {
            AopClassGenerator gen = new AopClassGenerator();
            assertNotNull(gen);
        }

        @Test
        @DisplayName("should create generator with context")
        void shouldCreateWithContext() {
            AopGenerationContext mockContext = mock(AopGenerationContext.class);
            when(mockContext.getTypeUtils()).thenReturn(mock(Types.class));
            AopClassGenerator gen = new AopClassGenerator(mockContext);
            assertNotNull(gen);
        }
    }

    @Nested
    @DisplayName("Generate AOP Classes Tests")
    class GenerateAopClassesTests {

        @Test
        @DisplayName("should return empty map for null components")
        void shouldReturnEmptyMapForNullComponents() {
            Map<String, String> result = generator.generateAopClasses(null);
            assertNotNull(result);
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("should return empty map for empty components list")
        void shouldReturnEmptyMapForEmptyList() {
            Map<String, String> result = generator.generateAopClasses(Collections.emptyList());
            assertNotNull(result);
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("should return empty map for component without intercepted methods")
        void shouldReturnEmptyMapForNoInterceptors() {
            TypeMirror mockTypeMirror = mock(TypeMirror.class);
            TestComponentNode component = new TestComponentNode(
                "io.github.yasmramos.veld.test.NormalService",
                "io.github.yasmramos.veld.test",
                "NormalService",
                "io/github/yasmramos/veld/test/NormalService",
                mockTypeMirror,
                Collections.emptyList(),
                false
            );

            Map<String, String> result = generator.generateAopClasses(List.of(component));
            assertNotNull(result);
        }

        @Test
        @DisplayName("should return empty map when hasInterceptors returns false")
        void shouldReturnEmptyMapWhenHasInterceptorsFalse() {
            TypeMirror mockTypeMirror = mock(TypeMirror.class);
            TestComponentNode component = new TestComponentNode(
                "io.github.yasmramos.veld.test.TestService",
                "io.github.yasmramos.veld.test",
                "TestService",
                "io/github/yasmramos/veld/test/TestService",
                mockTypeMirror,
                Collections.emptyList(),
                false
            );

            Map<String, String> result = generator.generateAopClasses(List.of(component));
            assertNotNull(result);
            assertTrue(result.isEmpty());
        }
    }

    @Nested
    @DisplayName("Get AOP Class Name Tests")
    class GetAopClassNameTests {

        @Test
        @DisplayName("should return null for unknown class")
        void shouldReturnNullForUnknownClass() {
            String result = generator.getAopClassName("UnknownClass");
            assertNull(result);
        }

        @Test
        @DisplayName("should return null for empty class name")
        void shouldReturnNullForEmptyClassName() {
            String result = generator.getAopClassName("");
            assertNull(result);
        }
    }

    @Nested
    @DisplayName("Has AOP Wrapper Tests")
    class HasAopWrapperTests {

        @Test
        @DisplayName("should return false for unknown class")
        void shouldReturnFalseForUnknownClass() {
            boolean result = generator.hasAopWrapper("UnknownClass");
            assertFalse(result);
        }

        @Test
        @DisplayName("should return false for empty class name")
        void shouldReturnFalseForEmptyClassName() {
            boolean result = generator.hasAopWrapper("");
            assertFalse(result);
        }
    }

    @Nested
    @DisplayName("Generate AOP Wrappers Tests")
    class GenerateAopWrappersTests {

        @Test
        @DisplayName("should return empty map for null components")
        void shouldReturnEmptyMapForNullComponents() {
            AopGenerationContext mockContext = mock(AopGenerationContext.class);
            AopClassGenerator gen = new AopClassGenerator(mockContext);
            Map<String, String> result = gen.generateAopWrappers(null, mockContext);
            assertNotNull(result);
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("should return empty map for empty components list")
        void shouldReturnEmptyMapForEmptyList() {
            AopGenerationContext mockContext = mock(AopGenerationContext.class);
            AopClassGenerator gen = new AopClassGenerator(mockContext);
            Map<String, String> result = gen.generateAopWrappers(Collections.emptyList(), mockContext);
            assertNotNull(result);
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("should return empty map when hasInterceptors is false")
        void shouldReturnEmptyMapWhenHasInterceptorsFalse() {
            AopGenerationContext mockContext = mock(AopGenerationContext.class);
            when(mockContext.getTypeUtils()).thenReturn(mock(Types.class));
            AopClassGenerator gen = new AopClassGenerator(mockContext);
            
            TypeMirror mockTypeMirror = mock(TypeMirror.class);
            TestComponentNode component = new TestComponentNode(
                "io.github.yasmramos.veld.test.Service",
                "io.github.yasmramos.veld.test",
                "Service",
                "io/github/yasmramos/veld/test/Service",
                mockTypeMirror,
                Collections.emptyList(),
                false
            );

            Map<String, String> result = gen.generateAopWrappers(List.of(component), mockContext);
            assertNotNull(result);
            assertTrue(result.isEmpty());
        }
    }

    @Nested
    @DisplayName("Interceptor Annotations Set Tests")
    class InterceptorAnnotationsSetTests {

        @Test
        @DisplayName("should contain all expected interceptor annotations")
        void shouldContainExpectedInterceptorAnnotations() throws Exception {
            java.lang.reflect.Field field = AopClassGenerator.class.getDeclaredField("INTERCEPTOR_ANNOTATIONS");
            field.setAccessible(true);
            @SuppressWarnings("unchecked")
            Set<String> annotations = (Set<String>) field.get(null);
            
            assertNotNull(annotations);
            assertFalse(annotations.isEmpty());
            
            // Verify some key annotations are present
            assertTrue(annotations.contains("io.github.yasmramos.veld.aop.interceptor.Logged"));
            assertTrue(annotations.contains("io.github.yasmramos.veld.annotation.Around"));
            assertTrue(annotations.contains("io.github.yasmramos.veld.annotation.Before"));
            assertTrue(annotations.contains("io.github.yasmramos.veld.annotation.After"));
            assertTrue(annotations.contains("io.github.yasmramos.veld.annotation.Async"));
            assertTrue(annotations.contains("io.github.yasmramos.veld.annotation.Scheduled"));
            assertTrue(annotations.contains("io.github.yasmramos.veld.annotation.Retry"));
            assertTrue(annotations.contains("io.github.yasmramos.veld.annotation.RateLimiter"));
        }

        @Test
        @DisplayName("interceptor annotations set should be immutable")
        void interceptorAnnotationsSetShouldBeImmutable() throws Exception {
            java.lang.reflect.Field field = AopClassGenerator.class.getDeclaredField("INTERCEPTOR_ANNOTATIONS");
            field.setAccessible(true);
            @SuppressWarnings("unchecked")
            Set<String> annotations = (Set<String>) field.get(null);
            
            assertThrows(UnsupportedOperationException.class, () -> annotations.add("new.annotation"));
        }

        @Test
        @DisplayName("should contain new-style annotations")
        void shouldContainNewStyleAnnotations() throws Exception {
            java.lang.reflect.Field field = AopClassGenerator.class.getDeclaredField("INTERCEPTOR_ANNOTATIONS");
            field.setAccessible(true);
            @SuppressWarnings("unchecked")
            Set<String> annotations = (Set<String>) field.get(null);
            
            // Verify new-style annotations are present
            assertTrue(annotations.contains("io.github.yasmramos.veld.annotation.Timed"));
            assertTrue(annotations.contains("io.github.yasmramos.veld.annotation.Valid"));
        }
    }

    @Nested
    @DisplayName("AOP Suffix Tests")
    class AopSuffixTests {

        @Test
        @DisplayName("should use correct AOP suffix")
        void shouldUseCorrectAopSuffix() throws Exception {
            java.lang.reflect.Field field = AopClassGenerator.class.getDeclaredField("AOP_SUFFIX");
            field.setAccessible(true);
            String suffix = (String) field.get(null);
            assertEquals("$$Aop", suffix);
        }

        @Test
        @DisplayName("AOP suffix should not be empty")
        void aopSuffixShouldNotBeEmpty() throws Exception {
            java.lang.reflect.Field field = AopClassGenerator.class.getDeclaredField("AOP_SUFFIX");
            field.setAccessible(true);
            String suffix = (String) field.get(null);
            assertFalse(suffix.isEmpty());
        }
    }

    @Nested
    @DisplayName("AOP Class Map Tests")
    class AopClassMapTests {

        @Test
        @DisplayName("aopClassMap should be initialized as empty")
        void aopClassMapShouldBeInitializedAsEmpty() throws Exception {
            java.lang.reflect.Field field = AopClassGenerator.class.getDeclaredField("aopClassMap");
            field.setAccessible(true);
            @SuppressWarnings("unchecked")
            Map<String, String> aopClassMap = (Map<String, String>) field.get(generator);
            assertNotNull(aopClassMap);
            assertTrue(aopClassMap.isEmpty());
        }
    }

    @Nested
    @DisplayName("Generated AOP Classes Set Tests")
    class GeneratedAopClassesSetTests {

        @Test
        @DisplayName("generatedAopClasses set should be synchronized")
        void generatedAopClassesSetShouldBeSynchronized() throws Exception {
            java.lang.reflect.Field field = AopClassGenerator.class.getDeclaredField("generatedAopClasses");
            field.setAccessible(true);
            @SuppressWarnings("unchecked")
            Set<String> set = (Set<String>) field.get(null);
            
            assertNotNull(set);
            // Verify it's a synchronized set by checking if it has synchronized collection characteristics
            assertTrue(set.getClass().getName().contains("Synchronized"));
        }

        @Test
        @DisplayName("generatedAopClasses set should be initially empty")
        void generatedAopClassesSetShouldBeInitiallyEmpty() throws Exception {
            java.lang.reflect.Field field = AopClassGenerator.class.getDeclaredField("generatedAopClasses");
            field.setAccessible(true);
            @SuppressWarnings("unchecked")
            Set<String> set = (Set<String>) field.get(null);
            assertNotNull(set);
            assertTrue(set.isEmpty());
        }
    }

    @Nested
    @DisplayName("TestComponentNode Implementation Tests")
    class TestComponentNodeTests {

        @Test
        @DisplayName("TestComponentNode should return correct values")
        void testComponentNodeShouldReturnCorrectValues() {
            TypeMirror mockTypeMirror = mock(TypeMirror.class);
            List<String> interceptors = Arrays.asList("Interceptor1", "Interceptor2");
            
            TestComponentNode node = new TestComponentNode(
                "com.example.TestService",
                "com.example",
                "TestService",
                "com/example/TestService",
                mockTypeMirror,
                interceptors,
                true
            );
            
            assertEquals("com.example.TestService", node.getClassName());
            assertEquals("com/example/TestService", node.getInternalName());
            assertEquals("com.example", node.getPackageName());
            assertEquals("TestService", node.getSimpleName());
            assertEquals(mockTypeMirror, node.getTypeMirror());
            assertEquals(2, node.getInterceptors().size());
            assertTrue(node.hasInterceptors());
        }

        @Test
        @DisplayName("TestComponentNode toString should contain class name")
        void testComponentNodeToStringShouldContainClassName() {
            TypeMirror mockTypeMirror = mock(TypeMirror.class);
            
            TestComponentNode node = new TestComponentNode(
                "com.example.TestService",
                "com.example",
                "TestService",
                "com/example/TestService",
                mockTypeMirror,
                Collections.emptyList(),
                false
            );
            
            String str = node.toString();
            assertTrue(str.contains("TestService"));
            assertTrue(str.contains("com.example.TestService"));
        }

        @Test
        @DisplayName("TestComponentNode should return empty list for null interceptors")
        void testComponentNodeShouldReturnEmptyListForNullInterceptors() {
            TypeMirror mockTypeMirror = mock(TypeMirror.class);
            
            TestComponentNode node = new TestComponentNode(
                "com.example.TestService",
                "com.example",
                "TestService",
                "com/example/TestService",
                mockTypeMirror,
                null,
                false
            );
            
            assertNotNull(node.getInterceptors());
            assertTrue(node.getInterceptors().isEmpty());
        }
    }
}
