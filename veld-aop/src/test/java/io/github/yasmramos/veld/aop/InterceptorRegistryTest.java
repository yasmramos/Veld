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
 * Tests for {@link InterceptorRegistry}.
 */
@DisplayName("InterceptorRegistry")
class InterceptorRegistryTest {

    private InterceptorRegistry registry;
    private PrintStream originalOut;
    private ByteArrayOutputStream outputCapture;

    @BeforeEach
    void setUp() {
        registry = InterceptorRegistry.getInstance();
        registry.clear();
        
        // Capture stdout
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
    @DisplayName("Singleton")
    class SingletonTests {

        @Test
        @DisplayName("should return same instance")
        void shouldReturnSameInstance() {
            InterceptorRegistry instance1 = InterceptorRegistry.getInstance();
            InterceptorRegistry instance2 = InterceptorRegistry.getInstance();
            
            assertSame(instance1, instance2);
        }
    }

    @Nested
    @DisplayName("Aspect Registration")
    class AspectRegistrationTests {

        @Test
        @DisplayName("should register aspect with around advice")
        void shouldRegisterAspectWithAroundAdvice() throws Exception {
            TestAspect aspect = new TestAspect();
            registry.registerAspect(aspect);
            
            // Check output
            String output = outputCapture.toString();
            assertTrue(output.contains("[AOP] Registered aspect: TestAspect"));
        }

        @Test
        @DisplayName("should reject non-aspect class")
        void shouldRejectNonAspectClass() {
            Object notAnAspect = new Object();
            
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, 
                    () -> registry.registerAspect(notAnAspect));
            
            assertTrue(ex.getMessage().contains("not annotated with @Aspect"));
        }

        @Test
        @DisplayName("should get interceptors for matching method")
        void shouldGetInterceptorsForMatchingMethod() throws Exception {
            TestAspect aspect = new TestAspect();
            registry.registerAspect(aspect);
            
            Method method = TestService.class.getMethod("doSomething");
            List<MethodInterceptor> interceptors = registry.getInterceptors(method);
            
            assertFalse(interceptors.isEmpty());
        }

        @Test
        @DisplayName("should check if class has advices")
        void shouldCheckIfClassHasAdvices() {
            TestInterceptor interceptor = new TestInterceptor();
            registry.registerInterceptor(interceptor);
            
            assertTrue(registry.hasAdvicesFor(ServiceWithBinding.class));
        }
    }

    @Nested
    @DisplayName("Interceptor Registration")
    class InterceptorRegistrationTests {

        @Test
        @DisplayName("should register interceptor with binding")
        void shouldRegisterInterceptorWithBinding() {
            TestInterceptor interceptor = new TestInterceptor();
            registry.registerInterceptor(interceptor);
            
            String output = outputCapture.toString();
            assertTrue(output.contains("[AOP] Registered interceptor: TestInterceptor"));
        }

        @Test
        @DisplayName("should reject non-interceptor class")
        void shouldRejectNonInterceptorClass() {
            Object notAnInterceptor = new Object();
            
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, 
                    () -> registry.registerInterceptor(notAnInterceptor));
            
            assertTrue(ex.getMessage().contains("not annotated with @Interceptor"));
        }

        @Test
        @DisplayName("should reject interceptor without AroundInvoke")
        void shouldRejectInterceptorWithoutAroundInvoke() {
            NoAroundInvokeInterceptor interceptor = new NoAroundInvokeInterceptor();
            
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, 
                    () -> registry.registerInterceptor(interceptor));
            
            assertTrue(ex.getMessage().contains("no @AroundInvoke method"));
        }

        @Test
        @DisplayName("should get interceptors for annotated method")
        void shouldGetInterceptorsForAnnotatedMethod() throws Exception {
            TestInterceptor interceptor = new TestInterceptor();
            registry.registerInterceptor(interceptor);
            
            Method method = ServiceWithBinding.class.getMethod("annotatedMethod");
            List<MethodInterceptor> interceptors = registry.getInterceptors(method);
            
            assertFalse(interceptors.isEmpty());
        }

        @Test
        @DisplayName("should check binding for class annotation")
        void shouldCheckBindingForClassAnnotation() throws Exception {
            TestInterceptor interceptor = new TestInterceptor();
            registry.registerInterceptor(interceptor);
            
            assertTrue(registry.hasAdvicesFor(AnnotatedService.class));
        }
    }

    @Nested
    @DisplayName("Statistics")
    class StatisticsTests {

        @Test
        @DisplayName("should provide statistics string")
        void shouldProvideStatisticsString() {
            TestAspect aspect = new TestAspect();
            registry.registerAspect(aspect);
            
            String stats = registry.getStatistics();
            
            assertTrue(stats.contains("InterceptorRegistry"));
            assertTrue(stats.contains("aspects"));
            assertTrue(stats.contains("advices"));
        }

        @Test
        @DisplayName("should clear all registrations")
        void shouldClearAllRegistrations() throws Exception {
            TestAspect aspect = new TestAspect();
            registry.registerAspect(aspect);
            
            registry.clear();
            
            Method method = TestService.class.getMethod("doSomething");
            List<MethodInterceptor> interceptors = registry.getInterceptors(method);
            assertTrue(interceptors.isEmpty());
        }
    }

    // Test annotations
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.TYPE})
    @InterceptorBinding
    @interface TestBinding {}

    // Test aspect
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

    // Test interceptor
    @Interceptor(priority = 100)
    @TestBinding
    public static class TestInterceptor {
        @AroundInvoke
        public Object intercept(InvocationContext ctx) throws Throwable {
            return ctx.proceed();
        }
    }

    // Interceptor without @AroundInvoke
    @Interceptor(priority = 100)
    @TestBinding
    public static class NoAroundInvokeInterceptor {
        public Object someMethod(InvocationContext ctx) throws Throwable {
            return ctx.proceed();
        }
    }

    // Test marker annotation
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    @interface TestMarker {}

    // Test services
    public static class TestService {
        @TestMarker
        public void doSomething() {}
    }

    public static class ServiceWithBinding {
        @TestBinding
        public void annotatedMethod() {}
    }

    @TestBinding
    public static class AnnotatedService {
        public void serviceMethod() {}
    }
}
