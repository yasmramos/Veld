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
package io.github.yasmramos.veld.aop.proxy;

import io.github.yasmramos.veld.aop.*;
import io.github.yasmramos.veld.annotation.*;
import org.junit.jupiter.api.*;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for OptimizedProxyFactory and OptimizedProxyMethodHandler.
 *
 * <p>These tests verify the optimized proxy generation implementation
 * provides correct method interception while maintaining compatibility
 * with the existing AOP API.
 *
 * @author Veld Framework Team
 * @since 1.0.0-alpha.6
 */
@DisplayName("Optimized Proxy Tests")
class OptimizedProxyTest {

    private OptimizedProxyFactory proxyFactory;

    @BeforeEach
    void setUp() {
        proxyFactory = OptimizedProxyFactory.getInstance();
        InterceptorRegistry.getInstance().clear();
    }

    @AfterEach
    void tearDown() {
        InterceptorRegistry.getInstance().clear();
    }

    // ==================== Basic Proxy Tests ====================

    @Nested
    @DisplayName("Basic Proxy Creation Tests")
    class BasicProxyTests {

        @Test
        @DisplayName("should create proxy for class without interceptors")
        void shouldCreateProxyForClassWithoutInterceptors() {
            // Given
            SimpleService service = new SimpleService("test");

            // When
            SimpleService proxy = proxyFactory.createProxy(service);

            // Then
            assertNotNull(proxy);
            assertSame(service, AopProxy.unwrap(proxy));
            assertEquals("test", proxy.getName());
        }

        @Test
        @DisplayName("should create proxy using class constructor")
        void shouldCreateProxyUsingClassConstructor() {
            // When
            SimpleService proxy = proxyFactory.createProxy(SimpleService.class);

            // Then
            assertNotNull(proxy);
            assertNotNull(proxy.getName());
        }

        @Test
        @DisplayName("should return original object when no advices exist")
        void shouldReturnOriginalWhenNoAdvices() {
            // Given
            SimpleService service = new SimpleService("original");

            // When
            Object result = proxyFactory.createProxy(service);

            // Then
            assertSame(service, result);
        }
    }

    // ==================== Interceptor Tests ====================

    @Nested
    @DisplayName("Interceptor Integration Tests")
    class InterceptorTests {

        @Test
        @DisplayName("should intercept method calls with advice")
        void shouldInterceptMethodCallsWithAdvice() {
            // Given
            CountingAspect aspect = new CountingAspect();
            InterceptorRegistry.getInstance().registerAspect(aspect);

            SimpleService service = new SimpleService("test");
            SimpleService proxy = proxyFactory.createProxy(service);

            // When
            String result = proxy.greet("World");

            // Then
            assertEquals("Hello World", result);
            assertEquals(1, aspect.getBeforeCount());
            assertEquals(1, aspect.getAfterCount());
        }

        @Test
        @DisplayName("should handle method with return value through interceptor")
        void shouldHandleReturnValueThroughInterceptor() {
            // Given
            LoggingAspect aspect = new LoggingAspect();
            InterceptorRegistry.getInstance().registerAspect(aspect);

            SimpleService service = new SimpleService("test");
            SimpleService proxy = proxyFactory.createProxy(service);

            // When
            int result = proxy.calculate(5, 3);

            // Then
            assertEquals(8, result);
        }

        @Test
        @DisplayName("should propagate exceptions through interceptor chain")
        void shouldPropagateExceptionsThroughInterceptor() {
            // Given
            ExceptionAspect aspect = new ExceptionAspect();
            InterceptorRegistry.getInstance().registerAspect(aspect);

            SimpleService service = new SimpleService("test");
            SimpleService proxy = proxyFactory.createProxy(service);

            // When/Then
            RuntimeException ex = assertThrows(RuntimeException.class, () -> {
                proxy.throwsException("test error");
            });
            assertEquals("test error", ex.getMessage());
        }

        @Test
        @DisplayName("should handle multiple interceptors in chain")
        void shouldHandleMultipleInterceptorsInChain() {
            // Given
            MultiInterceptorAspect aspect = new MultiInterceptorAspect();
            InterceptorRegistry.getInstance().registerAspect(aspect);

            SimpleService service = new SimpleService("test");
            SimpleService proxy = proxyFactory.createProxy(service);

            // When
            String result = proxy.getName();

            // Then
            assertEquals("test", result);
            assertTrue(aspect.getCallOrder().contains("first"));
            assertTrue(aspect.getCallOrder().contains("second"));
        }

        @Test
        @DisplayName("should support before advice")
        void shouldSupportBeforeAdvice() {
            // Given
            BeforeAspect aspect = new BeforeAspect();
            InterceptorRegistry.getInstance().registerAspect(aspect);

            SimpleService service = new SimpleService("test");
            SimpleService proxy = proxyFactory.createProxy(service);

            // When
            String result = proxy.getName();

            // Then
            assertEquals("test", result);
            assertTrue(aspect.isBeforeCalled());
        }

        @Test
        @DisplayName("should support after returning advice")
        void shouldSupportAfterReturningAdvice() {
            // Given
            AfterReturningAspect aspect = new AfterReturningAspect();
            InterceptorRegistry.getInstance().registerAspect(aspect);

            SimpleService service = new SimpleService("test");
            SimpleService proxy = proxyFactory.createProxy(service);

            // When
            String result = proxy.getName();

            // Then
            assertEquals("test", result);
            assertTrue(aspect.isAfterReturningCalled());
            assertEquals("test", aspect.getLastResult());
        }

        @Test
        @DisplayName("should support after throwing advice")
        void shouldSupportAfterThrowingAdvice() {
            // Given
            AfterThrowingAspect aspect = new AfterThrowingAspect();
            InterceptorRegistry.getInstance().registerAspect(aspect);

            SimpleService service = new SimpleService("test");
            SimpleService proxy = proxyFactory.createProxy(service);

            // When/Then
            assertThrows(RuntimeException.class, () -> proxy.throwsException("error"));
            assertTrue(aspect.isAfterThrowingCalled());
        }
    }

    // ==================== AopProxy Interface Tests ====================

    @Nested
    @DisplayName("AopProxy Interface Tests")
    class AopProxyInterfaceTests {

        @Test
        @DisplayName("proxied object should implement AopProxy")
        void proxiedObjectShouldImplementAopProxy() {
            // Given
            LoggingAspect aspect = new LoggingAspect();
            InterceptorRegistry.getInstance().registerAspect(aspect);

            SimpleService service = new SimpleService("test");
            SimpleService proxy = proxyFactory.createProxy(service);

            // Then
            assertTrue(proxy instanceof AopProxy);
        }

        @Test
        @DisplayName("getTargetObject should return original object")
        void getTargetObjectShouldReturnOriginal() {
            // Given
            LoggingAspect aspect = new LoggingAspect();
            InterceptorRegistry.getInstance().registerAspect(aspect);

            SimpleService service = new SimpleService("test");
            SimpleService proxy = proxyFactory.createProxy(service);

            // When
            AopProxy aopProxy = (AopProxy) proxy;
            Object target = aopProxy.getTargetObject();

            // Then
            assertSame(service, target);
        }

        @Test
        @DisplayName("getTargetClass should return original class")
        void getTargetClassShouldReturnOriginal() {
            // Given
            LoggingAspect aspect = new LoggingAspect();
            InterceptorRegistry.getInstance().registerAspect(aspect);

            SimpleService service = new SimpleService("test");
            SimpleService proxy = proxyFactory.createProxy(service);

            // When
            AopProxy aopProxy = (AopProxy) proxy;
            Class<?> targetClass = aopProxy.getTargetClass();

            // Then
            assertSame(SimpleService.class, targetClass);
        }

        @Test
        @DisplayName("isProxy should correctly identify proxied objects")
        void isProxyShouldCorrectlyIdentifyProxiedObjects() {
            // Given
            SimpleService withProxy = new SimpleService("with");
            SimpleService withoutProxy = new SimpleService("without");

            LoggingAspect aspect = new LoggingAspect();
            InterceptorRegistry.getInstance().registerAspect(aspect);

            SimpleService proxy = proxyFactory.createProxy(withProxy);

            // Then
            assertTrue(AopProxy.isProxy(proxy));
            assertFalse(AopProxy.isProxy(withoutProxy));
            assertFalse(AopProxy.isProxy("string"));
            assertFalse(AopProxy.isProxy(null));
        }

        @Test
        @DisplayName("unwrap should return original object")
        void unwrapShouldReturnOriginal() {
            // Given
            LoggingAspect aspect = new LoggingAspect();
            InterceptorRegistry.getInstance().registerAspect(aspect);

            SimpleService service = new SimpleService("test");
            SimpleService proxy = proxyFactory.createProxy(service);

            // When
            Object unwrapped = AopProxy.unwrap(proxy);

            // Then
            assertSame(service, unwrapped);
        }

        @Test
        @DisplayName("unwrap should return non-proxied objects as-is")
        void unwrapShouldReturnNonProxiedAsIs() {
            // Given
            SimpleService service = new SimpleService("test");

            // When
            Object unwrapped = AopProxy.unwrap(service);

            // Then
            assertSame(service, unwrapped);
        }
    }

    // ==================== Method Type Tests ====================

    @Nested
    @DisplayName("Method Type Tests")
    class MethodTypeTests {

        @Test
        @DisplayName("should handle void methods")
        void shouldHandleVoidMethods() {
            // Given
            CountingAspect aspect = new CountingAspect();
            InterceptorRegistry.getInstance().registerAspect(aspect);

            SimpleService service = new SimpleService("test");
            SimpleService proxy = proxyFactory.createProxy(service);

            // When
            assertDoesNotThrow(() -> proxy.doSomething());

            // Then
            assertEquals(1, aspect.getBeforeCount());
        }

        @Test
        @DisplayName("should handle primitive return types")
        void shouldHandlePrimitiveReturnTypes() {
            // Given
            LoggingAspect aspect = new LoggingAspect();
            InterceptorRegistry.getInstance().registerAspect(aspect);

            SimpleService service = new SimpleService("test");
            SimpleService proxy = proxyFactory.createProxy(service);

            // When
            int intResult = proxy.calculate(10, 5);
            long longResult = service.getLongValue();
            double doubleResult = service.getDoubleValue();
            boolean booleanResult = service.isActive();

            // Then
            assertEquals(15, intResult);
            assertEquals(100L, longResult);
            assertEquals(3.14, doubleResult, 0.001);
            assertTrue(booleanResult);
        }

        @Test
        @DisplayName("should handle object return types")
        void shouldHandleObjectReturnTypes() {
            // Given
            LoggingAspect aspect = new LoggingAspect();
            InterceptorRegistry.getInstance().registerAspect(aspect);

            SimpleService service = new SimpleService("test");
            SimpleService proxy = proxyFactory.createProxy(service);

            // When
            String result = proxy.getName();

            // Then
            assertEquals("test", result);
        }

        @Test
        @DisplayName("should handle methods with multiple parameters")
        void shouldHandleMethodsWithMultipleParameters() {
            // Given
            LoggingAspect aspect = new LoggingAspect();
            InterceptorRegistry.getInstance().registerAspect(aspect);

            SimpleService service = new SimpleService("test");
            SimpleService proxy = proxyFactory.createProxy(service);

            // When
            String result = proxy.concatenate("Hello", " ", "World");

            // Then
            assertEquals("Hello World", result);
        }
    }

    // ==================== Edge Case Tests ====================

    @Nested
    @DisplayName("Edge Case Tests")
    class EdgeCaseTests {

        @Test
        @DisplayName("should handle null parameters")
        void shouldHandleNullParameters() {
            // Given
            LoggingAspect aspect = new LoggingAspect();
            InterceptorRegistry.getInstance().registerAspect(aspect);

            SimpleService service = new SimpleService("test");
            SimpleService proxy = proxyFactory.createProxy(service);

            // When
            String result = proxy.greet(null);

            // Then
            assertEquals("Hello null", result);
        }

        @Test
        @DisplayName("should handle methods that return null")
        void shouldHandleMethodsThatReturnNull() {
            // Given
            LoggingAspect aspect = new LoggingAspect();
            InterceptorRegistry.getInstance().registerAspect(aspect);

            SimpleService service = new SimpleService("test");
            SimpleService proxy = proxyFactory.createProxy(service);

            // When
            String result = proxy.returnsNull();

            // Then
            assertNull(result);
        }

        @Test
        @DisplayName("should handle exception in before advice")
        void shouldHandleExceptionInBeforeAdvice() {
            // Given
            ThrowingBeforeAspect aspect = new ThrowingBeforeAspect();
            InterceptorRegistry.getInstance().registerAspect(aspect);

            SimpleService service = new SimpleService("test");
            SimpleService proxy = proxyFactory.createProxy(service);

            // When/Then
            RuntimeException ex = assertThrows(RuntimeException.class, () -> {
                proxy.getName();
            });
            assertEquals("Before failed", ex.getMessage());
        }

        @Test
        @DisplayName("should handle empty interceptor chain")
        void shouldHandleEmptyInterceptorChain() {
            // Given - no aspects registered
            SimpleService service = new SimpleService("test");
            SimpleService proxy = proxyFactory.createProxy(service);

            // When
            String result = proxy.getName();

            // Then
            assertEquals("test", result);
        }
    }

    // ==================== Performance Tests ====================

    @Nested
    @DisplayName("Performance Tests")
    class PerformanceTests {

        @Test
        @DisplayName("should create proxies efficiently")
        void shouldCreateProxiesEfficiently() {
            // Given
            LoggingAspect aspect = new LoggingAspect();
            InterceptorRegistry.getInstance().registerAspect(aspect);

            // Warm-up
            for (int i = 0; i < 100; i++) {
                proxyFactory.createProxy(new SimpleService("test"));
            }

            // When - measure proxy creation time
            long startTime = System.nanoTime();
            for (int i = 0; i < 1000; i++) {
                SimpleService proxy = proxyFactory.createProxy(new SimpleService("test"));
            }
            long endTime = System.nanoTime();

            // Then - should create 1000 proxies in under 100ms
            long durationMs = (endTime - startTime) / 1_000_000;
            assertTrue(durationMs < 100, "Proxy creation took " + durationMs + "ms, expected < 100ms");
        }

        @Test
        @DisplayName("proxied method call should be faster than reflection")
        void proxiedMethodCallShouldBeFasterThanReflection() throws Exception {
            // Given
            SimpleService service = new SimpleService("test");
            SimpleService proxy = proxyFactory.createProxy(service);
            Method method = SimpleService.class.getMethod("getName");

            // Warm-up
            for (int i = 0; i < 1000; i++) {
                proxy.getName();
                method.invoke(service);
            }

            // When - measure proxied call
            long proxyStart = System.nanoTime();
            for (int i = 0; i < 10000; i++) {
                proxy.getName();
            }
            long proxyEnd = System.nanoTime();

            // When - measure reflection call
            long reflectStart = System.nanoTime();
            for (int i = 0; i < 10000; i++) {
                method.invoke(service);
            }
            long reflectEnd = System.nanoTime();

            // Then - proxied call should be faster
            long proxyDuration = proxyEnd - proxyStart;
            long reflectDuration = reflectEnd - reflectStart;
            assertTrue(proxyDuration < reflectDuration,
                "Proxied call (" + proxyDuration + "ns) should be faster than reflection (" + reflectDuration + "ns)");
        }
    }

    // ==================== Test Helper Classes ====================

    // Simple service for testing
    static class SimpleService {
        private final String name;
        private final long longValue = 100L;
        private final double doubleValue = 3.14;
        private final boolean active = true;

        SimpleService(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public void doSomething() {
            // Empty method for void testing
        }

        public int calculate(int a, int b) {
            return a + b;
        }

        public String greet(String name) {
            return "Hello " + name;
        }

        public String concatenate(String... parts) {
            StringBuilder sb = new StringBuilder();
            for (String part : parts) {
                sb.append(part);
            }
            return sb.toString();
        }

        public void throwsException(String message) {
            throw new RuntimeException(message);
        }

        public String returnsNull() {
            return null;
        }

        public long getLongValue() {
            return longValue;
        }

        public double getDoubleValue() {
            return doubleValue;
        }

        public boolean isActive() {
            return active;
        }
    }

    // Test aspects
    @Aspect
    static class CountingAspect {
        private int beforeCount = 0;
        private int afterCount = 0;

        @Around("execution(* SimpleService.*(..))")
        public Object count(InvocationContext ctx) throws Throwable {
            beforeCount++;
            try {
                return ctx.proceed();
            } finally {
                afterCount++;
            }
        }

        public int getBeforeCount() { return beforeCount; }
        public int getAfterCount() { return afterCount; }
    }

    @Aspect
    static class LoggingAspect {
        @Around("execution(* SimpleService.*(..))")
        public Object log(InvocationContext ctx) throws Throwable {
            System.out.println("Before: " + ctx.getMethodName());
            try {
                return ctx.proceed();
            } finally {
                System.out.println("After: " + ctx.getMethodName());
            }
        }
    }

    @Aspect
    static class ExceptionAspect {
        @Around("execution(* SimpleService.throwsException(..))")
        public Object handleException(InvocationContext ctx) throws Throwable {
            try {
                return ctx.proceed();
            } catch (RuntimeException e) {
                System.out.println("Caught: " + e.getMessage());
                throw e;
            }
        }
    }

    @Aspect
    static class MultiInterceptorAspect {
        private java.util.List<String> callOrder = new java.util.ArrayList<>();

        @Around("execution(* SimpleService.getName())")
        public Object first(InvocationContext ctx) throws Throwable {
            callOrder.add("first");
            return ctx.proceed();
        }

        @Around("execution(* SimpleService.getName())")
        public Object second(InvocationContext ctx) throws Throwable {
            callOrder.add("second");
            return ctx.proceed();
        }

        public java.util.List<String> getCallOrder() { return callOrder; }
    }

    @Aspect
    static class BeforeAspect {
        private boolean beforeCalled = false;

        @Before("execution(* SimpleService.getName())")
        public void before(JoinPoint jp) {
            beforeCalled = true;
        }

        public boolean isBeforeCalled() { return beforeCalled; }
    }

    @Aspect
    static class AfterReturningAspect {
        private boolean afterReturningCalled = false;
        private String lastResult = null;

        @AfterReturning(pointcut = "execution(* SimpleService.getName())", returning = "result")
        public void afterReturning(JoinPoint jp, Object result) {
            afterReturningCalled = true;
            lastResult = (String) result;
        }

        public boolean isAfterReturningCalled() { return afterReturningCalled; }
        public String getLastResult() { return lastResult; }
    }

    @Aspect
    static class AfterThrowingAspect {
        private boolean afterThrowingCalled = false;

        @AfterThrowing(pointcut = "execution(* SimpleService.throwsException(..))", throwing = "ex")
        public void afterThrowing(JoinPoint jp, Throwable ex) {
            afterThrowingCalled = true;
        }

        public boolean isAfterThrowingCalled() { return afterThrowingCalled; }
    }

    @Aspect
    static class ThrowingBeforeAspect {
        @Before("execution(* SimpleService.getName())")
        public void before(JoinPoint jp) {
            throw new RuntimeException("Before failed");
        }
    }
}
