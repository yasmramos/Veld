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
package io.github.yasmramos.veld.aop.pointcut;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link CompositePointcut}.
 */
@DisplayName("CompositePointcut")
class CompositePointcutTest {

    @Nested
    @DisplayName("Simple Pointcuts")
    class SimplePointcutTests {

        @Test
        @DisplayName("should parse simple annotation expression")
        void shouldParseSimpleAnnotationExpression() throws Exception {
            CompositePointcut pointcut = CompositePointcut.parse("@annotation(TestMarker)");
            
            Method method = TestService.class.getMethod("markedMethod");
            assertTrue(pointcut.matches(method));
        }

        @Test
        @DisplayName("should parse within package expression")
        void shouldParseWithinPackageExpression() throws Exception {
            CompositePointcut pointcut = CompositePointcut.parse("within(io.github.yasmramos.veld.aop.pointcut.*)");
            
            Method method = TestService.class.getMethod("doSomething");
            assertTrue(pointcut.matches(method));
        }

        @Test
        @DisplayName("should return original expression")
        void shouldReturnOriginalExpression() {
            String expr = "@annotation(TestMarker)";
            CompositePointcut pointcut = CompositePointcut.parse(expr);
            
            assertEquals(expr, pointcut.getExpression());
        }

        @Test
        @DisplayName("should include expression in toString")
        void shouldIncludeExpressionInToString() {
            String expr = "@annotation(TestMarker)";
            CompositePointcut pointcut = CompositePointcut.parse(expr);
            
            String str = pointcut.toString();
            assertTrue(str.contains(expr));
            assertTrue(str.contains("CompositePointcut"));
        }
    }

    @Nested
    @DisplayName("AND Operator")
    class AndOperatorTests {

        @Test
        @DisplayName("should match when both conditions are true")
        void shouldMatchWhenBothConditionsAreTrue() throws Exception {
            CompositePointcut pointcut = CompositePointcut.parse(
                    "within(io.github.yasmramos.veld.aop.pointcut.*) && @annotation(TestMarker)");
            
            Method method = TestService.class.getMethod("markedMethod");
            assertTrue(pointcut.matches(method));
        }

        @Test
        @DisplayName("should not match when left condition is false")
        void shouldNotMatchWhenLeftConditionIsFalse() throws Exception {
            CompositePointcut pointcut = CompositePointcut.parse(
                    "within(com.nonexistent.*) && @annotation(TestMarker)");
            
            Method method = TestService.class.getMethod("markedMethod");
            assertFalse(pointcut.matches(method));
        }

        @Test
        @DisplayName("should not match when right condition is false")
        void shouldNotMatchWhenRightConditionIsFalse() throws Exception {
            CompositePointcut pointcut = CompositePointcut.parse(
                    "within(io.github.yasmramos.veld.aop.pointcut.*) && @annotation(NonExistent)");
            
            Method method = TestService.class.getMethod("doSomething");
            assertFalse(pointcut.matches(method));
        }
    }

    @Nested
    @DisplayName("OR Operator")
    class OrOperatorTests {

        @Test
        @DisplayName("should match when left condition is true")
        void shouldMatchWhenLeftConditionIsTrue() throws Exception {
            CompositePointcut pointcut = CompositePointcut.parse(
                    "@annotation(TestMarker) || @annotation(NonExistent)");
            
            Method method = TestService.class.getMethod("markedMethod");
            assertTrue(pointcut.matches(method));
        }

        @Test
        @DisplayName("should match when right condition is true")
        void shouldMatchWhenRightConditionIsTrue() throws Exception {
            CompositePointcut pointcut = CompositePointcut.parse(
                    "@annotation(NonExistent) || @annotation(TestMarker)");
            
            Method method = TestService.class.getMethod("markedMethod");
            assertTrue(pointcut.matches(method));
        }

        @Test
        @DisplayName("should not match when both conditions are false")
        void shouldNotMatchWhenBothConditionsAreFalse() throws Exception {
            CompositePointcut pointcut = CompositePointcut.parse(
                    "@annotation(NonExistent1) || @annotation(NonExistent2)");
            
            Method method = TestService.class.getMethod("doSomething");
            assertFalse(pointcut.matches(method));
        }
    }

    @Nested
    @DisplayName("NOT Operator")
    class NotOperatorTests {

        @Test
        @DisplayName("should negate matching expression")
        void shouldNegateMatchingExpression() throws Exception {
            CompositePointcut pointcut = CompositePointcut.parse(
                    "!@annotation(TestMarker)");
            
            Method markedMethod = TestService.class.getMethod("markedMethod");
            Method normalMethod = TestService.class.getMethod("doSomething");
            
            assertFalse(pointcut.matches(markedMethod));
            assertTrue(pointcut.matches(normalMethod));
        }

        @Test
        @DisplayName("should work with complex expression")
        void shouldWorkWithComplexExpression() throws Exception {
            CompositePointcut pointcut = CompositePointcut.parse(
                    "within(io.github.yasmramos.veld.aop.pointcut.*) && !@annotation(TestMarker)");
            
            Method markedMethod = TestService.class.getMethod("markedMethod");
            Method normalMethod = TestService.class.getMethod("doSomething");
            
            assertFalse(pointcut.matches(markedMethod));
            assertTrue(pointcut.matches(normalMethod));
        }
    }

    @Nested
    @DisplayName("Parentheses")
    class ParenthesesTests {

        @Test
        @DisplayName("should respect parentheses grouping")
        void shouldRespectParenthesesGrouping() throws Exception {
            CompositePointcut pointcut = CompositePointcut.parse(
                    "(@annotation(TestMarker) || @annotation(AnotherMarker)) && within(io.github.yasmramos.veld.aop.pointcut.*)");
            
            Method method = TestService.class.getMethod("markedMethod");
            assertTrue(pointcut.matches(method));
        }

        @Test
        @DisplayName("should handle nested parentheses")
        void shouldHandleNestedParentheses() throws Exception {
            CompositePointcut pointcut = CompositePointcut.parse(
                    "((@annotation(TestMarker)))");
            
            Method method = TestService.class.getMethod("markedMethod");
            assertTrue(pointcut.matches(method));
        }
    }

    @Nested
    @DisplayName("Operator Precedence")
    class OperatorPrecedenceTests {

        @Test
        @DisplayName("OR has lower precedence than AND")
        void orHasLowerPrecedenceThanAnd() throws Exception {
            // a || b && c should be parsed as a || (b && c)
            CompositePointcut pointcut = CompositePointcut.parse(
                    "@annotation(TestMarker) || @annotation(AnotherMarker) && @annotation(NonExistent)");
            
            Method method = TestService.class.getMethod("markedMethod");
            // markedMethod matches left side of OR, so should match
            assertTrue(pointcut.matches(method));
        }
    }

    @Nested
    @DisplayName("Could Match Optimization")
    class CouldMatchTests {

        @Test
        @DisplayName("should return true for simple annotation")
        void shouldReturnTrueForSimpleAnnotation() {
            CompositePointcut pointcut = CompositePointcut.parse("@annotation(TestMarker)");
            
            assertTrue(pointcut.couldMatch(TestService.class));
        }

        @Test
        @DisplayName("should check OR conditions")
        void shouldCheckOrConditions() {
            CompositePointcut pointcut = CompositePointcut.parse(
                    "within(com.nonexistent.*) || @annotation(TestMarker)");
            
            assertTrue(pointcut.couldMatch(TestService.class));
        }

        @Test
        @DisplayName("should return true for NOT (conservative)")
        void shouldReturnTrueForNot() {
            CompositePointcut pointcut = CompositePointcut.parse(
                    "!within(com.nonexistent.*)");
            
            // NOT is conservative, always returns true
            assertTrue(pointcut.couldMatch(TestService.class));
        }
    }

    // Test annotation
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    @interface TestMarker {}

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    @interface AnotherMarker {}

    // Test classes
    static class TestService {
        public void doSomething() {}
        public int getValue() { return 42; }
        
        @TestMarker
        public void markedMethod() {}
    }
}
