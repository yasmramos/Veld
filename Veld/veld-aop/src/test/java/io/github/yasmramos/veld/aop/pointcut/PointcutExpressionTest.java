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
 * Tests for {@link PointcutExpression}.
 */
@DisplayName("PointcutExpression")
class PointcutExpressionTest {

    @Nested
    @DisplayName("Execution Pointcuts")
    class ExecutionPointcutTests {

        @Test
        @DisplayName("should match method with full package pattern")
        void shouldMatchMethodWithFullPackagePattern() throws Exception {
            // Use double-dot wildcard to match the full signature
            PointcutExpression pointcut = PointcutExpression.parse(
                    "execution(public void io.github.yasmramos.veld..doSomething())");
            
            Method method = TestService.class.getMethod("doSomething");
            assertTrue(pointcut.matches(method));
        }

        @Test
        @DisplayName("should match with double-dot wildcard in package")
        void shouldMatchWithDoubleDotWildcardInPackage() throws Exception {
            PointcutExpression pointcut = PointcutExpression.parse(
                    "execution(public void io.github.yasmramos.veld..doSomething())");
            
            Method method = TestService.class.getMethod("doSomething");
            assertTrue(pointcut.matches(method));
        }

        @Test
        @DisplayName("should not match different method")
        void shouldNotMatchDifferentMethod() throws Exception {
            PointcutExpression pointcut = PointcutExpression.parse(
                    "execution(public void io.github.yasmramos.veld..nonExistentMethod())");
            
            Method method = TestService.class.getMethod("doSomething");
            assertFalse(pointcut.matches(method));
        }

        @Test
        @DisplayName("should return execution type")
        void shouldReturnExecutionType() {
            PointcutExpression pointcut = PointcutExpression.parse("execution(* *(..))");
            
            assertEquals(PointcutExpression.PointcutType.EXECUTION, pointcut.getType());
        }

        @Test
        @DisplayName("should return original expression")
        void shouldReturnOriginalExpression() {
            String expr = "execution(* *(..))";
            PointcutExpression pointcut = PointcutExpression.parse(expr);
            
            assertEquals(expr, pointcut.getExpression());
        }

        @Test
        @DisplayName("should match method with return type")
        void shouldMatchMethodWithReturnType() throws Exception {
            PointcutExpression pointcut = PointcutExpression.parse(
                    "execution(public int io.github.yasmramos.veld..calculate(..))");
            
            Method method = TestService.class.getMethod("calculate", int.class);
            assertTrue(pointcut.matches(method));
        }
    }

    @Nested
    @DisplayName("Within Pointcuts")
    class WithinPointcutTests {

        @Test
        @DisplayName("should match methods within package pattern")
        void shouldMatchMethodsWithinPackagePattern() throws Exception {
            PointcutExpression pointcut = PointcutExpression.parse(
                    "within(io.github.yasmramos.veld.aop.pointcut.*)");
            
            Method method = TestService.class.getMethod("doSomething");
            assertTrue(pointcut.matches(method));
        }

        @Test
        @DisplayName("should not match methods in different package")
        void shouldNotMatchMethodsInDifferentPackage() throws Exception {
            PointcutExpression pointcut = PointcutExpression.parse(
                    "within(com.other.package.*)");
            
            Method method = TestService.class.getMethod("doSomething");
            assertFalse(pointcut.matches(method));
        }

        @Test
        @DisplayName("should return within type")
        void shouldReturnWithinType() {
            PointcutExpression pointcut = PointcutExpression.parse("within(com.example.*)");
            
            assertEquals(PointcutExpression.PointcutType.WITHIN, pointcut.getType());
        }
    }

    @Nested
    @DisplayName("Annotation Pointcuts")
    class AnnotationPointcutTests {

        @Test
        @DisplayName("should match methods with annotation by simple name")
        void shouldMatchMethodsWithAnnotationBySimpleName() throws Exception {
            PointcutExpression pointcut = PointcutExpression.parse(
                    "@annotation(TestAnnotation)");
            
            Method annotatedMethod = TestService.class.getMethod("annotatedMethod");
            Method nonAnnotatedMethod = TestService.class.getMethod("doSomething");
            
            assertTrue(pointcut.matches(annotatedMethod));
            assertFalse(pointcut.matches(nonAnnotatedMethod));
        }

        @Test
        @DisplayName("should match methods with annotation by full name")
        void shouldMatchMethodsWithAnnotationByFullName() throws Exception {
            String annotationName = TestAnnotation.class.getName();
            PointcutExpression pointcut = PointcutExpression.parse(
                    "@annotation(" + annotationName + ")");
            
            Method annotatedMethod = TestService.class.getMethod("annotatedMethod");
            assertTrue(pointcut.matches(annotatedMethod));
        }

        @Test
        @DisplayName("should return annotation type")
        void shouldReturnAnnotationType() {
            PointcutExpression pointcut = PointcutExpression.parse("@annotation(Test)");
            
            assertEquals(PointcutExpression.PointcutType.ANNOTATION, pointcut.getType());
        }
    }

    @Nested
    @DisplayName("Within Annotation Pointcuts")
    class WithinAnnotationPointcutTests {

        @Test
        @DisplayName("should match methods in annotated class by simple name")
        void shouldMatchMethodsInAnnotatedClassBySimpleName() throws Exception {
            PointcutExpression pointcut = PointcutExpression.parse(
                    "@within(ClassAnnotation)");
            
            Method method = AnnotatedService.class.getMethod("serviceMethod");
            assertTrue(pointcut.matches(method));
        }

        @Test
        @DisplayName("should match methods in annotated class by full name")
        void shouldMatchMethodsInAnnotatedClassByFullName() throws Exception {
            String annotationName = ClassAnnotation.class.getName();
            PointcutExpression pointcut = PointcutExpression.parse(
                    "@within(" + annotationName + ")");
            
            Method method = AnnotatedService.class.getMethod("serviceMethod");
            assertTrue(pointcut.matches(method));
        }

        @Test
        @DisplayName("should not match methods in non-annotated class")
        void shouldNotMatchMethodsInNonAnnotatedClass() throws Exception {
            PointcutExpression pointcut = PointcutExpression.parse(
                    "@within(ClassAnnotation)");
            
            Method method = TestService.class.getMethod("doSomething");
            assertFalse(pointcut.matches(method));
        }

        @Test
        @DisplayName("should return within-annotation type")
        void shouldReturnWithinAnnotationType() {
            PointcutExpression pointcut = PointcutExpression.parse("@within(Test)");
            
            assertEquals(PointcutExpression.PointcutType.WITHIN_ANNOTATION, pointcut.getType());
        }

        @Test
        @DisplayName("should check class annotation for optimization")
        void shouldCheckClassAnnotationForOptimization() {
            PointcutExpression pointcut = PointcutExpression.parse(
                    "@within(ClassAnnotation)");
            
            assertTrue(pointcut.couldMatch(AnnotatedService.class));
            assertFalse(pointcut.couldMatch(TestService.class));
        }
    }

    @Nested
    @DisplayName("Default Behavior")
    class DefaultBehaviorTests {

        @Test
        @DisplayName("should treat unknown expression as execution pattern")
        void shouldTreatUnknownExpressionAsExecutionPattern() {
            PointcutExpression pointcut = PointcutExpression.parse("* *.doSomething(..)");
            
            assertEquals(PointcutExpression.PointcutType.EXECUTION, pointcut.getType());
        }

        @Test
        @DisplayName("should throw on invalid expression")
        void shouldThrowOnInvalidExpression() {
            assertThrows(IllegalArgumentException.class, 
                    () -> PointcutExpression.parse("execution()"));
        }

        @Test
        @DisplayName("couldMatch should return true for execution")
        void couldMatchShouldReturnTrueForExecution() {
            PointcutExpression pointcut = PointcutExpression.parse("execution(* *(..))");
            
            assertTrue(pointcut.couldMatch(TestService.class));
        }

        @Test
        @DisplayName("couldMatch should return true for annotation")
        void couldMatchShouldReturnTrueForAnnotation() {
            PointcutExpression pointcut = PointcutExpression.parse("@annotation(Test)");
            
            assertTrue(pointcut.couldMatch(TestService.class));
        }
    }

    @Nested
    @DisplayName("ToString")
    class ToStringTests {

        @Test
        @DisplayName("should include expression in toString")
        void shouldIncludeExpressionInToString() {
            String expr = "execution(* *(..))";
            PointcutExpression pointcut = PointcutExpression.parse(expr);
            
            String str = pointcut.toString();
            assertTrue(str.contains(expr));
            assertTrue(str.contains("PointcutExpression"));
        }
    }

    // Test annotations
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    @interface TestAnnotation {}

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    @interface ClassAnnotation {}

    // Test classes
    static class TestService {
        public void doSomething() {}
        public int calculate(int value) { return value * 2; }
        public String getName() { return "test"; }
        
        @TestAnnotation
        public void annotatedMethod() {}
    }

    @ClassAnnotation
    static class AnnotatedService {
        public void serviceMethod() {}
    }
}
