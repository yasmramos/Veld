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
package io.github.yasmramos.veld.aop.interceptor;

import io.github.yasmramos.veld.aop.InvocationContext;
import io.github.yasmramos.veld.aop.MethodInvocation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link TimingInterceptor}.
 */
@DisplayName("TimingInterceptor")
class TimingInterceptorTest {

    private TimingInterceptor interceptor;
    private PrintStream originalOut;
    private ByteArrayOutputStream outputCapture;

    @BeforeEach
    void setUp() {
        interceptor = new TimingInterceptor();
        TimingInterceptor.clearStatistics();
        
        // Capture stdout
        originalOut = System.out;
        outputCapture = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outputCapture));
    }

    @AfterEach
    void tearDown() {
        System.setOut(originalOut);
        TimingInterceptor.clearStatistics();
    }

    @Nested
    @DisplayName("MethodStats")
    class MethodStatsTests {

        @Test
        @DisplayName("should record invocation count")
        void shouldRecordInvocationCount() {
            TimingInterceptor.MethodStats stats = 
                    new TimingInterceptor.MethodStats("testMethod");
            
            stats.record(1000);
            stats.record(2000);
            stats.record(3000);
            
            assertEquals(3, stats.getInvocationCount());
        }

        @Test
        @DisplayName("should calculate total time")
        void shouldCalculateTotalTime() {
            TimingInterceptor.MethodStats stats = 
                    new TimingInterceptor.MethodStats("testMethod");
            
            stats.record(1000);
            stats.record(2000);
            stats.record(3000);
            
            assertEquals(6000, stats.getTotalTimeNanos());
        }

        @Test
        @DisplayName("should track minimum time")
        void shouldTrackMinimumTime() {
            TimingInterceptor.MethodStats stats = 
                    new TimingInterceptor.MethodStats("testMethod");
            
            stats.record(3000);
            stats.record(1000);
            stats.record(2000);
            
            assertEquals(1000, stats.getMinTimeNanos());
        }

        @Test
        @DisplayName("should track maximum time")
        void shouldTrackMaximumTime() {
            TimingInterceptor.MethodStats stats = 
                    new TimingInterceptor.MethodStats("testMethod");
            
            stats.record(1000);
            stats.record(3000);
            stats.record(2000);
            
            assertEquals(3000, stats.getMaxTimeNanos());
        }

        @Test
        @DisplayName("should calculate average time")
        void shouldCalculateAverageTime() {
            TimingInterceptor.MethodStats stats = 
                    new TimingInterceptor.MethodStats("testMethod");
            
            stats.record(1000);
            stats.record(2000);
            stats.record(3000);
            
            assertEquals(2000.0, stats.getAverageTimeNanos(), 0.001);
        }

        @Test
        @DisplayName("should return zero average for no invocations")
        void shouldReturnZeroAverageForNoInvocations() {
            TimingInterceptor.MethodStats stats = 
                    new TimingInterceptor.MethodStats("testMethod");
            
            assertEquals(0.0, stats.getAverageTimeNanos());
        }

        @Test
        @DisplayName("should return zero min for no invocations")
        void shouldReturnZeroMinForNoInvocations() {
            TimingInterceptor.MethodStats stats = 
                    new TimingInterceptor.MethodStats("testMethod");
            
            assertEquals(0, stats.getMinTimeNanos());
        }

        @Test
        @DisplayName("should return method name")
        void shouldReturnMethodName() {
            TimingInterceptor.MethodStats stats = 
                    new TimingInterceptor.MethodStats("myMethod");
            
            assertEquals("myMethod", stats.getMethodName());
        }

        @Test
        @DisplayName("should produce readable toString")
        void shouldProduceReadableToString() {
            TimingInterceptor.MethodStats stats = 
                    new TimingInterceptor.MethodStats("testMethod");
            
            stats.record(1_000_000); // 1ms
            stats.record(2_000_000); // 2ms
            
            String str = stats.toString();
            
            assertTrue(str.contains("testMethod"));
            assertTrue(str.contains("count=2"));
            assertTrue(str.contains("avg="));
            assertTrue(str.contains("min="));
            assertTrue(str.contains("max="));
        }
    }

    @Nested
    @DisplayName("Timing Measurement")
    class TimingMeasurementTests {

        @Test
        @DisplayName("should measure method execution time")
        void shouldMeasureMethodExecutionTime() throws Throwable {
            Method method = TestService.class.getMethod("fastMethod");
            TestService target = new TestService();
            
            MethodInvocation invocation = new MethodInvocation(target, method, 
                    new Object[]{}, new ArrayList<>());
            
            // Intercept
            Object result = interceptor.measureTime(invocation);
            
            assertEquals("fast", result);
            
            // Check output
            String output = outputCapture.toString();
            assertTrue(output.contains("[TIMING]"));
            assertTrue(output.contains("fastMethod"));
        }

        @Test
        @DisplayName("should record statistics for method")
        void shouldRecordStatisticsForMethod() throws Throwable {
            Method method = TestService.class.getMethod("fastMethod");
            TestService target = new TestService();
            
            MethodInvocation invocation = new MethodInvocation(target, method, 
                    new Object[]{}, new ArrayList<>());
            
            interceptor.measureTime(invocation);
            interceptor.measureTime(invocation);
            
            Map<String, TimingInterceptor.MethodStats> stats = 
                    TimingInterceptor.getStatistics();
            
            assertTrue(stats.containsKey("TestService.fastMethod"));
            assertEquals(2, stats.get("TestService.fastMethod").getInvocationCount());
        }
    }

    @Nested
    @DisplayName("Static Methods")
    class StaticMethodsTests {

        @Test
        @DisplayName("should get all statistics")
        void shouldGetAllStatistics() throws Throwable {
            Method method1 = TestService.class.getMethod("fastMethod");
            Method method2 = TestService.class.getMethod("anotherMethod");
            TestService target = new TestService();
            
            MethodInvocation invocation1 = new MethodInvocation(target, method1, 
                    new Object[]{}, new ArrayList<>());
            MethodInvocation invocation2 = new MethodInvocation(target, method2, 
                    new Object[]{}, new ArrayList<>());
            
            interceptor.measureTime(invocation1);
            interceptor.measureTime(invocation2);
            
            Map<String, TimingInterceptor.MethodStats> stats = 
                    TimingInterceptor.getStatistics();
            
            assertEquals(2, stats.size());
        }

        @Test
        @DisplayName("should clear statistics")
        void shouldClearStatistics() throws Throwable {
            Method method = TestService.class.getMethod("fastMethod");
            TestService target = new TestService();
            
            MethodInvocation invocation = new MethodInvocation(target, method, 
                    new Object[]{}, new ArrayList<>());
            
            interceptor.measureTime(invocation);
            assertFalse(TimingInterceptor.getStatistics().isEmpty());
            
            TimingInterceptor.clearStatistics();
            
            assertTrue(TimingInterceptor.getStatistics().isEmpty());
        }

        @Test
        @DisplayName("should print statistics")
        void shouldPrintStatistics() throws Throwable {
            Method method = TestService.class.getMethod("fastMethod");
            TestService target = new TestService();
            
            MethodInvocation invocation = new MethodInvocation(target, method, 
                    new Object[]{}, new ArrayList<>());
            
            interceptor.measureTime(invocation);
            TimingInterceptor.printStatistics();
            
            String output = outputCapture.toString();
            assertTrue(output.contains("Method Timing Statistics"));
            assertTrue(output.contains("TestService.fastMethod"));
        }
    }

    @Nested
    @DisplayName("Exception Handling")
    class ExceptionHandlingTests {

        @Test
        @DisplayName("should still record time when method throws")
        void shouldStillRecordTimeWhenMethodThrows() throws Exception {
            Method method = TestService.class.getMethod("throwingMethod");
            TestService target = new TestService();
            
            MethodInvocation invocation = new MethodInvocation(target, method, 
                    new Object[]{}, new ArrayList<>());
            
            assertThrows(RuntimeException.class, () -> interceptor.measureTime(invocation));
            
            // Statistics should still be recorded
            Map<String, TimingInterceptor.MethodStats> stats = 
                    TimingInterceptor.getStatistics();
            assertTrue(stats.containsKey("TestService.throwingMethod"));
            assertEquals(1, stats.get("TestService.throwingMethod").getInvocationCount());
        }
    }

    // Test service class
    public static class TestService {
        @Timed
        public String fastMethod() {
            return "fast";
        }

        @Timed
        public String anotherMethod() {
            return "another";
        }

        @Timed
        public void throwingMethod() {
            throw new RuntimeException("Test exception");
        }
    }
}
