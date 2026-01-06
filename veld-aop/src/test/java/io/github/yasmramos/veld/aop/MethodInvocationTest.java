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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link MethodInvocation}.
 */
@DisplayName("MethodInvocation")
class MethodInvocationTest {

    private TestTarget target;
    private Method greetMethod;
    private Method addMethod;
    private Method voidMethod;

    @BeforeEach
    void setUp() throws Exception {
        target = new TestTarget();
        greetMethod = TestTarget.class.getMethod("greet", String.class);
        addMethod = TestTarget.class.getMethod("add", int.class, int.class);
        voidMethod = TestTarget.class.getMethod("doSomething");
    }

    @Nested
    @DisplayName("Basic Properties")
    class BasicPropertiesTests {

        @Test
        @DisplayName("should return target object")
        void shouldReturnTarget() {
            MethodInvocation invocation = new MethodInvocation(target, greetMethod, 
                    new Object[]{"World"}, new ArrayList<>());
            
            assertSame(target, invocation.getTarget());
        }

        @Test
        @DisplayName("should return method")
        void shouldReturnMethod() {
            MethodInvocation invocation = new MethodInvocation(target, greetMethod, 
                    new Object[]{"World"}, new ArrayList<>());
            
            assertSame(greetMethod, invocation.getMethod());
        }

        @Test
        @DisplayName("should return method name")
        void shouldReturnMethodName() {
            MethodInvocation invocation = new MethodInvocation(target, greetMethod, 
                    new Object[]{"World"}, new ArrayList<>());
            
            assertEquals("greet", invocation.getMethodName());
        }

        @Test
        @DisplayName("should return declaring class")
        void shouldReturnDeclaringClass() {
            MethodInvocation invocation = new MethodInvocation(target, greetMethod, 
                    new Object[]{"World"}, new ArrayList<>());
            
            assertEquals(TestTarget.class, invocation.getDeclaringClass());
        }

        @Test
        @DisplayName("should return parameters as copy")
        void shouldReturnParametersAsCopy() {
            Object[] params = new Object[]{"World"};
            MethodInvocation invocation = new MethodInvocation(target, greetMethod, 
                    params, new ArrayList<>());
            
            Object[] returned = invocation.getParameters();
            assertArrayEquals(params, returned);
            assertNotSame(params, returned);
        }

        @Test
        @DisplayName("should return args as copy")
        void shouldReturnArgsAsCopy() {
            Object[] params = new Object[]{"World"};
            MethodInvocation invocation = new MethodInvocation(target, greetMethod, 
                    params, new ArrayList<>());
            
            Object[] returned = invocation.getArgs();
            assertArrayEquals(params, returned);
            assertNotSame(params, returned);
        }

        @Test
        @DisplayName("should handle null parameters")
        void shouldHandleNullParameters() {
            MethodInvocation invocation = new MethodInvocation(target, voidMethod, 
                    null, new ArrayList<>());
            
            assertNotNull(invocation.getParameters());
            assertEquals(0, invocation.getParameters().length);
        }
    }

    @Nested
    @DisplayName("Parameter Modification")
    class ParameterModificationTests {

        @Test
        @DisplayName("should allow setting parameters")
        void shouldAllowSettingParameters() {
            MethodInvocation invocation = new MethodInvocation(target, greetMethod, 
                    new Object[]{"World"}, new ArrayList<>());
            
            invocation.setParameters(new Object[]{"Universe"});
            
            assertArrayEquals(new Object[]{"Universe"}, invocation.getParameters());
        }

        @Test
        @DisplayName("should reject null parameters")
        void shouldRejectNullParameters() {
            MethodInvocation invocation = new MethodInvocation(target, greetMethod, 
                    new Object[]{"World"}, new ArrayList<>());
            
            assertThrows(IllegalArgumentException.class, 
                    () -> invocation.setParameters(null));
        }

        @Test
        @DisplayName("should reject wrong parameter count")
        void shouldRejectWrongParameterCount() {
            MethodInvocation invocation = new MethodInvocation(target, greetMethod, 
                    new Object[]{"World"}, new ArrayList<>());
            
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, 
                    () -> invocation.setParameters(new Object[]{"a", "b"}));
            
            assertTrue(ex.getMessage().contains("Expected 1 parameters"));
        }
    }

    @Nested
    @DisplayName("Context Data")
    class ContextDataTests {

        @Test
        @DisplayName("should provide mutable context data map")
        void shouldProvideMutableContextDataMap() {
            MethodInvocation invocation = new MethodInvocation(target, greetMethod, 
                    new Object[]{"World"}, new ArrayList<>());
            
            Map<String, Object> contextData = invocation.getContextData();
            assertNotNull(contextData);
            
            contextData.put("key", "value");
            assertEquals("value", invocation.getContextData().get("key"));
        }

        @Test
        @DisplayName("should return null for timer")
        void shouldReturnNullForTimer() {
            MethodInvocation invocation = new MethodInvocation(target, greetMethod, 
                    new Object[]{"World"}, new ArrayList<>());
            
            assertNull(invocation.getTimer());
        }
    }

    @Nested
    @DisplayName("Method Invocation")
    class MethodInvocationTests {

        @Test
        @DisplayName("should invoke target method without interceptors")
        void shouldInvokeTargetMethodWithoutInterceptors() throws Throwable {
            MethodInvocation invocation = new MethodInvocation(target, greetMethod, 
                    new Object[]{"World"}, new ArrayList<>());
            
            Object result = invocation.proceed();
            
            assertEquals("Hello, World!", result);
        }

        @Test
        @DisplayName("should invoke method with multiple parameters")
        void shouldInvokeMethodWithMultipleParameters() throws Throwable {
            MethodInvocation invocation = new MethodInvocation(target, addMethod, 
                    new Object[]{5, 3}, new ArrayList<>());
            
            Object result = invocation.proceed();
            
            assertEquals(8, result);
        }

        @Test
        @DisplayName("should invoke void method")
        void shouldInvokeVoidMethod() throws Throwable {
            MethodInvocation invocation = new MethodInvocation(target, voidMethod, 
                    new Object[]{}, new ArrayList<>());
            
            Object result = invocation.proceed();
            
            assertNull(result);
            assertTrue(target.wasDoSomethingCalled());
        }

        @Test
        @DisplayName("should propagate exceptions")
        void shouldPropagateExceptions() throws Exception {
            Method throwingMethod = TestTarget.class.getMethod("throwException");
            MethodInvocation invocation = new MethodInvocation(target, throwingMethod, 
                    new Object[]{}, new ArrayList<>());
            
            RuntimeException ex = assertThrows(RuntimeException.class, invocation::proceed);
            assertEquals("Test exception", ex.getMessage());
        }
    }

    @Nested
    @DisplayName("Interceptor Chain")
    class InterceptorChainTests {

        @Test
        @DisplayName("should execute single interceptor")
        void shouldExecuteSingleInterceptor() throws Throwable {
            List<String> calls = new ArrayList<>();
            
            MethodInterceptor interceptor = ctx -> {
                calls.add("before");
                Object result = ctx.proceed();
                calls.add("after");
                return result;
            };
            
            MethodInvocation invocation = new MethodInvocation(target, greetMethod, 
                    new Object[]{"World"}, Arrays.asList(interceptor));
            
            Object result = invocation.proceed();
            
            assertEquals("Hello, World!", result);
            assertEquals(Arrays.asList("before", "after"), calls);
        }

        @Test
        @DisplayName("should execute multiple interceptors in order")
        void shouldExecuteMultipleInterceptorsInOrder() throws Throwable {
            List<String> calls = new ArrayList<>();
            
            MethodInterceptor interceptor1 = ctx -> {
                calls.add("1-before");
                Object result = ctx.proceed();
                calls.add("1-after");
                return result;
            };
            
            MethodInterceptor interceptor2 = ctx -> {
                calls.add("2-before");
                Object result = ctx.proceed();
                calls.add("2-after");
                return result;
            };
            
            MethodInvocation invocation = new MethodInvocation(target, greetMethod, 
                    new Object[]{"World"}, Arrays.asList(interceptor1, interceptor2));
            
            Object result = invocation.proceed();
            
            assertEquals("Hello, World!", result);
            assertEquals(Arrays.asList("1-before", "2-before", "2-after", "1-after"), calls);
        }

        @Test
        @DisplayName("should allow interceptor to modify result")
        void shouldAllowInterceptorToModifyResult() throws Throwable {
            MethodInterceptor interceptor = ctx -> {
                Object result = ctx.proceed();
                return result + " (modified)";
            };
            
            MethodInvocation invocation = new MethodInvocation(target, greetMethod, 
                    new Object[]{"World"}, Arrays.asList(interceptor));
            
            Object result = invocation.proceed();
            
            assertEquals("Hello, World! (modified)", result);
        }

        @Test
        @DisplayName("should allow interceptor to modify parameters")
        void shouldAllowInterceptorToModifyParameters() throws Throwable {
            MethodInterceptor interceptor = ctx -> {
                ctx.setParameters(new Object[]{"Modified"});
                return ctx.proceed();
            };
            
            MethodInvocation invocation = new MethodInvocation(target, greetMethod, 
                    new Object[]{"World"}, Arrays.asList(interceptor));
            
            Object result = invocation.proceed();
            
            assertEquals("Hello, Modified!", result);
        }

        @Test
        @DisplayName("should track current interceptor")
        void shouldTrackCurrentInterceptor() throws Throwable {
            final Object[] capturedInterceptor = new Object[1];
            
            MethodInterceptor interceptor = ctx -> {
                capturedInterceptor[0] = ctx.getInterceptor();
                return ctx.proceed();
            };
            
            MethodInvocation invocation = new MethodInvocation(target, greetMethod, 
                    new Object[]{"World"}, Arrays.asList(interceptor));
            
            invocation.proceed();
            
            assertSame(interceptor, capturedInterceptor[0]);
        }
    }

    @Nested
    @DisplayName("Signature Methods")
    class SignatureMethodsTests {

        @Test
        @DisplayName("should build correct signature")
        void shouldBuildCorrectSignature() {
            MethodInvocation invocation = new MethodInvocation(target, greetMethod, 
                    new Object[]{"World"}, new ArrayList<>());
            
            String signature = invocation.getSignature();
            
            assertTrue(signature.contains("String"));
            assertTrue(signature.contains("TestTarget"));
            assertTrue(signature.contains("greet"));
        }

        @Test
        @DisplayName("should build signature with multiple parameters")
        void shouldBuildSignatureWithMultipleParameters() {
            MethodInvocation invocation = new MethodInvocation(target, addMethod, 
                    new Object[]{5, 3}, new ArrayList<>());
            
            String signature = invocation.getSignature();
            
            assertTrue(signature.contains("int"));
            assertTrue(signature.contains("add"));
        }

        @Test
        @DisplayName("should produce short string")
        void shouldProduceShortString() {
            MethodInvocation invocation = new MethodInvocation(target, greetMethod, 
                    new Object[]{"World"}, new ArrayList<>());
            
            String shortStr = invocation.toShortString();
            
            assertEquals("TestTarget.greet()", shortStr);
        }

        @Test
        @DisplayName("should produce long string with args")
        void shouldProduceLongStringWithArgs() {
            MethodInvocation invocation = new MethodInvocation(target, greetMethod, 
                    new Object[]{"World"}, new ArrayList<>());
            
            String longStr = invocation.toLongString();
            
            assertTrue(longStr.contains("greet"));
            assertTrue(longStr.contains("World"));
        }

        @Test
        @DisplayName("should produce toString")
        void shouldProduceToString() {
            MethodInvocation invocation = new MethodInvocation(target, greetMethod, 
                    new Object[]{"World"}, new ArrayList<>());
            
            String str = invocation.toString();
            
            assertTrue(str.contains("MethodInvocation"));
            assertTrue(str.contains("TestTarget.greet()"));
        }
    }

    // Test target class
    public static class TestTarget {
        private boolean doSomethingCalled = false;

        public String greet(String name) {
            return "Hello, " + name + "!";
        }

        public int add(int a, int b) {
            return a + b;
        }

        public void doSomething() {
            doSomethingCalled = true;
        }

        public void throwException() {
            throw new RuntimeException("Test exception");
        }

        public boolean wasDoSomethingCalled() {
            return doSomethingCalled;
        }
    }
}
