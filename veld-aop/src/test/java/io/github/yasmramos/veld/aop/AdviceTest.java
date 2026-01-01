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

import io.github.yasmramos.veld.annotation.AfterType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link Advice}.
 */
@DisplayName("Advice")
class AdviceTest {

    private TestAspect aspect;
    private Method aroundMethod;
    private Method beforeMethod;
    private Method afterMethod;

    @BeforeEach
    void setUp() throws Exception {
        aspect = new TestAspect();
        aroundMethod = TestAspect.class.getMethod("aroundAdvice", InvocationContext.class);
        beforeMethod = TestAspect.class.getMethod("beforeAdvice", JoinPoint.class);
        afterMethod = TestAspect.class.getMethod("afterAdvice", JoinPoint.class);
    }

    @Nested
    @DisplayName("Factory Methods")
    class FactoryMethodsTests {

        @Test
        @DisplayName("should create around advice")
        void shouldCreateAroundAdvice() {
            Advice advice = Advice.around("execution(* *(..))", aspect, aroundMethod, 0);
            
            assertEquals(Advice.Type.AROUND, advice.getType());
            assertSame(aspect, advice.getAspectInstance());
            assertSame(aroundMethod, advice.getAdviceMethod());
            assertEquals(0, advice.getOrder());
        }

        @Test
        @DisplayName("should create before advice")
        void shouldCreateBeforeAdvice() {
            Advice advice = Advice.before("execution(* *(..))", aspect, beforeMethod, 1);
            
            assertEquals(Advice.Type.BEFORE, advice.getType());
            assertSame(aspect, advice.getAspectInstance());
            assertSame(beforeMethod, advice.getAdviceMethod());
            assertEquals(1, advice.getOrder());
        }

        @Test
        @DisplayName("should create after returning advice")
        void shouldCreateAfterReturningAdvice() {
            Advice advice = Advice.after("execution(* *(..))", AfterType.RETURNING, 
                    aspect, afterMethod, 2);
            
            assertEquals(Advice.Type.AFTER_RETURNING, advice.getType());
        }

        @Test
        @DisplayName("should create after throwing advice")
        void shouldCreateAfterThrowingAdvice() {
            Advice advice = Advice.after("execution(* *(..))", AfterType.THROWING, 
                    aspect, afterMethod, 2);
            
            assertEquals(Advice.Type.AFTER_THROWING, advice.getType());
        }

        @Test
        @DisplayName("should create after finally advice")
        void shouldCreateAfterFinallyAdvice() {
            Advice advice = Advice.after("execution(* *(..))", AfterType.FINALLY, 
                    aspect, afterMethod, 2);
            
            assertEquals(Advice.Type.AFTER_FINALLY, advice.getType());
        }
    }

    @Nested
    @DisplayName("Pointcut Matching")
    class PointcutMatchingTests {

        @Test
        @DisplayName("should match method based on pointcut")
        void shouldMatchMethodBasedOnPointcut() throws Exception {
            Advice advice = Advice.around("@annotation(TestMarker)", 
                    aspect, aroundMethod, 0);
            
            Method matchingMethod = TestTarget.class.getMethod("markedMethod");
            Method nonMatchingMethod = TestTarget.class.getMethod("calculate", int.class);
            
            assertTrue(advice.matches(matchingMethod));
            assertFalse(advice.matches(nonMatchingMethod));
        }

        @Test
        @DisplayName("should check if could match class")
        void shouldCheckIfCouldMatchClass() {
            Advice advice = Advice.around("@annotation(TestMarker)", 
                    aspect, aroundMethod, 0);
            
            // couldMatch returns true for @annotation (conservative)
            assertTrue(advice.couldMatch(TestTarget.class));
        }

        @Test
        @DisplayName("should return pointcut")
        void shouldReturnPointcut() {
            Advice advice = Advice.around("execution(* *(..))", aspect, aroundMethod, 0);
            
            assertNotNull(advice.getPointcut());
            assertEquals("execution(* *(..))", advice.getPointcut().getExpression());
        }
    }

    @Nested
    @DisplayName("Invocation")
    class InvocationTests {

        @Test
        @DisplayName("should invoke advice method")
        void shouldInvokeAdviceMethod() throws Throwable {
            Advice advice = Advice.around("execution(* *(..))", aspect, aroundMethod, 0);
            
            // Create a mock context
            InvocationContext mockContext = new MockInvocationContext();
            
            Object result = advice.invoke(mockContext);
            
            assertEquals("around-result", result);
            assertTrue(aspect.wasAroundCalled());
        }

        @Test
        @DisplayName("should propagate exceptions from advice")
        void shouldPropagateExceptionsFromAdvice() throws Exception {
            Method throwingMethod = TestAspect.class.getMethod("throwingAdvice", 
                    InvocationContext.class);
            Advice advice = Advice.around("execution(* *(..))", aspect, throwingMethod, 0);
            
            InvocationContext mockContext = new MockInvocationContext();
            
            RuntimeException ex = assertThrows(RuntimeException.class, 
                    () -> advice.invoke(mockContext));
            assertEquals("Test exception from advice", ex.getMessage());
        }
    }

    @Nested
    @DisplayName("Ordering")
    class OrderingTests {

        @Test
        @DisplayName("should compare by order")
        void shouldCompareByOrder() {
            Advice advice1 = Advice.around("execution(* *(..))", aspect, aroundMethod, 1);
            Advice advice2 = Advice.around("execution(* *(..))", aspect, aroundMethod, 2);
            Advice advice3 = Advice.around("execution(* *(..))", aspect, aroundMethod, 0);
            
            assertTrue(advice3.compareTo(advice1) < 0);
            assertTrue(advice1.compareTo(advice2) < 0);
            assertEquals(0, advice1.compareTo(advice1));
        }

        @Test
        @DisplayName("should return order value")
        void shouldReturnOrderValue() {
            Advice advice = Advice.around("execution(* *(..))", aspect, aroundMethod, 42);
            
            assertEquals(42, advice.getOrder());
        }
    }

    @Nested
    @DisplayName("Type Enum")
    class TypeEnumTests {

        @Test
        @DisplayName("should have all advice types")
        void shouldHaveAllAdviceTypes() {
            Advice.Type[] types = Advice.Type.values();
            
            assertEquals(5, types.length);
            assertNotNull(Advice.Type.valueOf("AROUND"));
            assertNotNull(Advice.Type.valueOf("BEFORE"));
            assertNotNull(Advice.Type.valueOf("AFTER_RETURNING"));
            assertNotNull(Advice.Type.valueOf("AFTER_THROWING"));
            assertNotNull(Advice.Type.valueOf("AFTER_FINALLY"));
        }
    }

    @Nested
    @DisplayName("ToString")
    class ToStringTests {

        @Test
        @DisplayName("should produce readable toString")
        void shouldProduceReadableToString() {
            Advice advice = Advice.around("execution(* *(..))", aspect, aroundMethod, 5);
            
            String str = advice.toString();
            
            assertTrue(str.contains("Advice"));
            assertTrue(str.contains("AROUND"));
            assertTrue(str.contains("aroundAdvice"));
            assertTrue(str.contains("5"));
        }
    }

    // Test aspect
    public static class TestAspect {
        private boolean aroundCalled = false;

        public Object aroundAdvice(InvocationContext ctx) throws Throwable {
            aroundCalled = true;
            return "around-result";
        }

        public void beforeAdvice(JoinPoint jp) {
            // Before logic
        }

        public void afterAdvice(JoinPoint jp) {
            // After logic
        }

        public Object throwingAdvice(InvocationContext ctx) throws Throwable {
            throw new RuntimeException("Test exception from advice");
        }

        public boolean wasAroundCalled() {
            return aroundCalled;
        }
    }

    // Test marker annotation
    @java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
    @java.lang.annotation.Target(java.lang.annotation.ElementType.METHOD)
    @interface TestMarker {}

    // Test target
    public static class TestTarget {
        public void doSomething() {}
        public int calculate(int value) { return value * 2; }
        
        @TestMarker
        public void markedMethod() {}
    }

    // Mock invocation context
    public static class MockInvocationContext implements InvocationContext {
        @Override
        public Object proceed() throws Throwable {
            return "proceeded";
        }

        @Override
        public Object[] getParameters() {
            return new Object[0];
        }

        @Override
        public void setParameters(Object[] params) {}

        @Override
        public java.util.Map<String, Object> getContextData() {
            return new java.util.HashMap<>();
        }

        @Override
        public Object getTimer() {
            return null;
        }

        @Override
        public Object getInterceptor() {
            return null;
        }

        @Override
        public Object getTarget() {
            return null;
        }

        @Override
        public Method getMethod() {
            return null;
        }

        @Override
        public String getMethodName() {
            return "mockMethod";
        }

        @Override
        public boolean returnsVoid() {
            return true;
        }

        @Override
        public Class<?>[] getParameterTypes() {
            return new Class<?>[0];
        }

        @Override
        public Class<?> getReturnType() {
            return void.class;
        }

        @Override
        public Object[] getArgs() {
            return new Object[0];
        }

        @Override
        public Class<?> getDeclaringClass() {
            return MockInvocationContext.class;
        }

        @Override
        public String getDeclaringClassName() {
            return "io.github.yasmramos.veld.aop.AdviceTest";
        }

        @Override
        public String getSignature() {
            return "void MockInvocationContext.mockMethod()";
        }

        @Override
        public String toShortString() {
            return "MockInvocationContext.mockMethod()";
        }

        @Override
        public String toLongString() {
            return "void MockInvocationContext.mockMethod() with args []";
        }
    }
}
