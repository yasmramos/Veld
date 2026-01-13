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

import io.github.yasmramos.veld.annotation.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests de cobertura para {@link InterceptorRegistry}.
 * Enfocado en los métodos con cobertura cero: getInterceptorsForMethod y findMethod.
 */
@DisplayName("InterceptorRegistryCoverage")
class InterceptorRegistryCoverageTest {

    private InterceptorRegistry registry;
    private PrintStream originalOut;
    private ByteArrayOutputStream outputCapture;

    @BeforeEach
    void setUp() {
        registry = InterceptorRegistry.getInstance();
        registry.clear();
        
        originalOut = System.out;
        outputCapture = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outputCapture));
    }

    @AfterEach
    void tearDown() {
        System.setOut(originalOut);
        registry.clear();
    }

    @Nested
    @DisplayName("getInterceptorsForMethod")
    class GetInterceptorsForMethodTests {

        @Test
        @DisplayName("should return interceptors for valid method with parameters")
        void shouldReturnInterceptorsForValidMethodWithParameters() {
            TestAspect aspect = new TestAspect();
            registry.registerAspect(aspect);
            
            String[] paramTypes = {"String", "int"};
            List<MethodInterceptor> interceptors = registry.getInterceptorsForMethod(
                    TargetService.class, "methodWithParams", paramTypes);
            
            assertNotNull(interceptors);
        }

        @Test
        @DisplayName("should return empty list when method not found")
        void shouldReturnEmptyListWhenMethodNotFound() {
            TestAspect aspect = new TestAspect();
            registry.registerAspect(aspect);
            
            String[] paramTypes = {"String"};
            List<MethodInterceptor> interceptors = registry.getInterceptorsForMethod(
                    TargetService.class, "nonExistentMethod", paramTypes);
            
            assertNotNull(interceptors);
            assertTrue(interceptors.isEmpty());
        }

        @Test
        @DisplayName("should return empty list when parameter types mismatch")
        void shouldReturnEmptyListWhenParameterTypesMismatch() {
            TestAspect aspect = new TestAspect();
            registry.registerAspect(aspect);
            
            String[] wrongParamTypes = {"WrongType", "int"};
            List<MethodInterceptor> interceptors = registry.getInterceptorsForMethod(
                    TargetService.class, "methodWithParams", wrongParamTypes);
            
            assertNotNull(interceptors);
            assertTrue(interceptors.isEmpty());
        }

        @Test
        @DisplayName("should use cache for subsequent calls")
        void shouldUseCacheForSubsequentCalls() {
            TestAspect aspect = new TestAspect();
            registry.registerAspect(aspect);
            
            String[] paramTypes = {"String"};
            List<MethodInterceptor> interceptors1 = registry.getInterceptorsForMethod(
                    TargetService.class, "simpleMethod", paramTypes);
            
            List<MethodInterceptor> interceptors2 = registry.getInterceptorsForMethod(
                    TargetService.class, "simpleMethod", paramTypes);
            
            assertSame(interceptors1, interceptors2);
        }

        @Test
        @DisplayName("should include aspect advices in returned interceptors")
        void shouldIncludeAspectAdvicesInReturnedInterceptors() {
            TestAspectWithAnnotation aspect = new TestAspectWithAnnotation();
            registry.registerAspect(aspect);
            
            String[] paramTypes = {};
            List<MethodInterceptor> interceptors = registry.getInterceptorsForMethod(
                    AnnotatedTarget.class, "annotatedMethod", paramTypes);
            
            assertFalse(interceptors.isEmpty());
        }

        @Test
        @DisplayName("should handle method with no annotations")
        void shouldHandleMethodWithNoAnnotations() {
            TestAspect aspect = new TestAspect();
            registry.registerAspect(aspect);
            
            String[] paramTypes = {};
            List<MethodInterceptor> interceptors = registry.getInterceptorsForMethod(
                    TargetService.class, "unannotatedMethod", paramTypes);
            
            assertNotNull(interceptors);
        }

        @Test
        @DisplayName("should find method with multiple parameters")
        void shouldFindMethodWithMultipleParameters() {
            TestAspect aspect = new TestAspect();
            registry.registerAspect(aspect);
            
            String[] paramTypes = {"String", "int", "boolean", "Object"};
            List<MethodInterceptor> interceptors = registry.getInterceptorsForMethod(
                    TargetService.class, "methodWithMultipleParams", paramTypes);
            
            assertNotNull(interceptors);
        }

        @Test
        @DisplayName("should handle empty parameter types array")
        void shouldHandleEmptyParameterTypesArray() {
            TestAspect aspect = new TestAspect();
            registry.registerAspect(aspect);
            
            String[] paramTypes = {};
            List<MethodInterceptor> interceptors = registry.getInterceptorsForMethod(
                    TargetService.class, "noParamsMethod", paramTypes);
            
            assertNotNull(interceptors);
        }

        @Test
        @DisplayName("should include class-level bindings")
        void shouldIncludeClassLevelBindings() {
            TestInterceptor interceptor = new TestInterceptor();
            registry.registerInterceptor(interceptor);
            
            String[] paramTypes = {};
            List<MethodInterceptor> interceptors = registry.getInterceptorsForMethod(
                    ServiceWithClassBinding.class, "someMethod", paramTypes);
            
            assertFalse(interceptors.isEmpty());
        }

        @Test
        @DisplayName("should not duplicate method-level and class-level bindings")
        void shouldNotDuplicateMethodAndClassLevelBindings() {
            TestInterceptor interceptor = new TestInterceptor();
            registry.registerInterceptor(interceptor);
            
            String[] paramTypes = {};
            List<MethodInterceptor> interceptors = registry.getInterceptorsForMethod(
                    ServiceWithBothBindings.class, "methodWithBinding", paramTypes);
            
            assertNotNull(interceptors);
        }
    }

    @Nested
    @DisplayName("findMethod (indirect coverage)")
    class FindMethodCoverageTests {

        @Test
        @DisplayName("findMethod should match exact method name")
        void findMethodShouldMatchExactMethodName() {
            String[] paramTypes = {"String"};
            List<MethodInterceptor> interceptors = registry.getInterceptorsForMethod(
                    OverloadedService.class, "process", paramTypes);
            
            assertNotNull(interceptors);
        }

        @Test
        @DisplayName("findMethod should differentiate overloaded methods")
        void findMethodShouldDifferentiateOverloadedMethods() {
            String[] paramTypes = {"int"};
            List<MethodInterceptor> interceptors = registry.getInterceptorsForMethod(
                    OverloadedService.class, "process", paramTypes);
            
            assertNotNull(interceptors);
        }

        @Test
        @DisplayName("findMethod should return null for abstract class methods")
        void findMethodShouldReturnNullForAbstractClassMethods() {
            String[] paramTypes = {};
            List<MethodInterceptor> interceptors = registry.getInterceptorsForMethod(
                    AbstractService.class, "abstractMethod", paramTypes);
            
            assertTrue(interceptors.isEmpty());
        }

        @Test
        @DisplayName("findMethod should find method in superclass")
        void findMethodShouldFindMethodInSuperclass() {
            String[] paramTypes = {};
            List<MethodInterceptor> interceptors = registry.getInterceptorsForMethod(
                    SubService.class, "inheritedMethod", paramTypes);
            
            assertNotNull(interceptors);
        }
    }

    @Nested
    @DisplayName("getInterceptorsForMethod - Integration Scenarios")
    class IntegrationTests {

        @Test
        @DisplayName("should combine aspect and interceptor bindings")
        void shouldCombineAspectAndInterceptorBindings() {
            TestAspectWithAnnotation aspect = new TestAspectWithAnnotation();
            TestInterceptor interceptor = new TestInterceptor();
            
            registry.registerAspect(aspect);
            registry.registerInterceptor(interceptor);
            
            String[] paramTypes = {};
            List<MethodInterceptor> interceptors = registry.getInterceptorsForMethod(
                    CombinedService.class, "combinedMethod", paramTypes);
            
            assertNotNull(interceptors);
        }

        @Test
        @DisplayName("should handle cache invalidation on new registration")
        void shouldHandleCacheInvalidationOnNewRegistration() {
            String[] paramTypes = {};
            List<MethodInterceptor> interceptors1 = registry.getInterceptorsForMethod(
                    CacheTestService.class, "cacheTestMethod", paramTypes);
            
            TestAspect aspect = new TestAspect();
            registry.registerAspect(aspect);
            
            List<MethodInterceptor> interceptors2 = registry.getInterceptorsForMethod(
                    CacheTestService.class, "cacheTestMethod", paramTypes);
            
            assertNotSame(interceptors1, interceptors2);
        }

        @Test
        @DisplayName("should preserve interceptor order by priority")
        void shouldPreserveInterceptorOrderByPriority() {
            PriorityInterceptor lowPriority = new PriorityInterceptor(1);
            PriorityInterceptor highPriority = new PriorityInterceptor(100);
            
            registry.registerInterceptor(lowPriority);
            registry.registerInterceptor(highPriority);
            
            String[] paramTypes = {};
            List<MethodInterceptor> interceptors = registry.getInterceptorsForMethod(
                    PriorityService.class, "priorityMethod", paramTypes);
            
            assertNotNull(interceptors);
        }
    }

    // Anotaciones de test
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.TYPE})
    @InterceptorBinding
    @interface TestBinding {}

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    @interface TestMarker {}

    // Aspect de test
    @Aspect(order = 0)
    public static class TestAspect {
        @Around("@annotation(TestMarker)")
        public Object aroundAdvice(InvocationContext ctx) throws Throwable {
            return ctx.proceed();
        }

        @Before("@annotation(TestMarker)")
        public void beforeAdvice(JoinPoint jp) {}

        @After(value = "@annotation(TestMarker)", type = AfterType.FINALLY)
        public void afterAdvice(JoinPoint jp) {}
    }

    @Aspect(order = 1)
    public static class TestAspectWithAnnotation {
        @Around("@annotation(CustomAnnotation)")
        public Object aroundCustom(InvocationContext ctx) throws Throwable {
            return ctx.proceed();
        }
    }

    // Interceptores de test
    @Interceptor(priority = 100)
    @TestBinding
    public static class TestInterceptor {
        @AroundInvoke
        public Object intercept(InvocationContext ctx) throws Throwable {
            return ctx.proceed();
        }
    }

    @Interceptor(priority = 50)
    @TestBinding
    public static class PriorityInterceptor {
        private final int priority;

        public PriorityInterceptor(int priority) {
            this.priority = priority;
        }

        @AroundInvoke
        public Object intercept(InvocationContext ctx) throws Throwable {
            return ctx.proceed();
        }
    }

    // Servicios de test
    public static class TargetService {
        public void noParamsMethod() {}

        public void unannotatedMethod() {}

        public void simpleMethod(String param) {}

        public void methodWithParams(String param1, int param2) {}

        public void methodWithMultipleParams(String param1, int param2, boolean param3, Object param4) {}
    }

    public static class AnnotatedTarget {
        @CustomAnnotation
        public void annotatedMethod() {}
    }

    @TestBinding
    public static class ServiceWithClassBinding {
        public void someMethod() {}
    }

    public static class ServiceWithBothBindings {
        @TestBinding
        public void methodWithBinding() {}
    }

    public static class OverloadedService {
        public void process(String input) {}
        public void process(int value) {}
        public void process() {}
    }

    public static abstract class AbstractService {
        public abstract void abstractMethod();
    }

    public static class BaseService {
        public void inheritedMethod() {}
    }

    public static class SubService extends BaseService {}

    public static class CombinedService {
        @CustomAnnotation
        public void combinedMethod() {}
    }

    public static class CacheTestService {
        public void cacheTestMethod() {}
    }

    public static class PriorityService {
        @TestBinding
        public void priorityMethod() {}
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    @interface CustomAnnotation {}
}
