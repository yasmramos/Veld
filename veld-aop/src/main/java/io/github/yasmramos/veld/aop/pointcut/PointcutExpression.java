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

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.regex.Pattern;

/**
 * Represents and evaluates a pointcut expression.
 *
 * <p>Supports the following pointcut designators:
 * <ul>
 *   <li>{@code execution(modifiers? returnType declaringType.method(params))} - method execution</li>
 *   <li>{@code within(type)} - all methods within a type</li>
 *   <li>{@code @annotation(annotationType)} - methods with annotation</li>
 *   <li>{@code @within(annotationType)} - types with annotation</li>
 * </ul>
 *
 * <h2>Wildcards</h2>
 * <ul>
 *   <li>{@code *} - matches any single element (one package level, any class name, any method name)</li>
 *   <li>{@code ..} - matches any number of elements (any sub-packages, any parameters)</li>
 * </ul>
 *
 * @author Veld Framework Team
 * @since 1.0.0-alpha.5
 */
public class PointcutExpression {

    private final String expression;
    private final PointcutType type;
    private final Pattern pattern;
    private final String annotationClass;

    /**
     * Pointcut designator types.
     */
    public enum PointcutType {
        EXECUTION,
        WITHIN,
        ANNOTATION,
        WITHIN_ANNOTATION
    }

    /**
     * Parses a pointcut expression.
     *
     * @param expression the expression string
     * @return the parsed pointcut expression
     */
    public static PointcutExpression parse(String expression) {
        return new PointcutExpression(expression);
    }

    private PointcutExpression(String expression) {
        this.expression = expression.trim();
        
        if (this.expression.startsWith("execution(")) {
            this.type = PointcutType.EXECUTION;
            String pattern = extractContent("execution");
            this.pattern = compileExecutionPattern(pattern);
            this.annotationClass = null;
        } else if (this.expression.startsWith("within(")) {
            this.type = PointcutType.WITHIN;
            String pattern = extractContent("within");
            this.pattern = compileWithinPattern(pattern);
            this.annotationClass = null;
        } else if (this.expression.startsWith("@annotation(")) {
            this.type = PointcutType.ANNOTATION;
            this.annotationClass = extractContent("@annotation");
            this.pattern = null;
        } else if (this.expression.startsWith("@within(")) {
            this.type = PointcutType.WITHIN_ANNOTATION;
            this.annotationClass = extractContent("@within");
            this.pattern = null;
        } else {
            // Default: treat as execution pattern
            this.type = PointcutType.EXECUTION;
            this.pattern = compileExecutionPattern(this.expression);
            this.annotationClass = null;
        }
    }

    private String extractContent(String prefix) {
        int start = expression.indexOf('(') + 1;
        int end = expression.lastIndexOf(')');
        if (end <= start) {
            throw new IllegalArgumentException("Invalid expression: " + expression);
        }
        return expression.substring(start, end).trim();
    }

    /**
     * Compiles an execution pattern into a regex.
     *
     * <p>Pattern format: [modifiers] returnType declaringType.methodName(paramTypes)
     * <p>Examples:
     * <ul>
     *   <li>{@code * com.example..*.*(..)}</li>
     *   <li>{@code public * com.example.service.*Service.*(..)}</li>
     *   <li>{@code * *(..)}</li>
     * </ul>
     */
    private Pattern compileExecutionPattern(String patternStr) {
        // Normalize whitespace
        patternStr = patternStr.trim().replaceAll("\\s+", " ");
        
        // Convert wildcards to regex
        String regex = patternStr
                // Escape special regex characters except * and .
                .replace("(", "\\(")
                .replace(")", "\\)")
                .replace("[", "\\[")
                .replace("]", "\\]")
                .replace("$", "\\$")
                // Handle .. (any sub-packages or any params)
                .replace("..", "@@DOTDOT@@")
                // Handle * (any single element)
                .replace("*", "[^.()]*")
                // Restore ..
                .replace("@@DOTDOT@@", ".*")
                // Handle spaces (optional modifiers)
                .replace(" ", "\\s+");
        
        return Pattern.compile("^" + regex + "$");
    }

    private Pattern compileWithinPattern(String patternStr) {
        String regex = patternStr
                // First, handle .. before escaping dots
                .replace("..", "@@DOTDOT@@")
                // Escape special regex characters
                .replace(".", "\\.")
                .replace("$", "\\$")
                // Handle * (any class name, including inner classes with $)
                .replace("*", "[^.]*")
                // Restore ..
                .replace("@@DOTDOT@@", ".*");
        
        return Pattern.compile("^" + regex);
    }

    /**
     * Tests if this pointcut matches a method.
     *
     * @param method the method to test
     * @return true if the pointcut matches
     */
    public boolean matches(Method method) {
        switch (type) {
            case EXECUTION:
                return matchesExecution(method);
            case WITHIN:
                return matchesWithin(method);
            case ANNOTATION:
                return matchesAnnotation(method);
            case WITHIN_ANNOTATION:
                return matchesWithinAnnotation(method);
            default:
                return false;
        }
    }

    private boolean matchesExecution(Method method) {
        String signature = buildSignature(method);
        return pattern.matcher(signature).matches();
    }

    private String buildSignature(Method method) {
        StringBuilder sb = new StringBuilder();
        
        // Add modifiers
        int mods = method.getModifiers();
        if (Modifier.isPublic(mods)) sb.append("public ");
        else if (Modifier.isProtected(mods)) sb.append("protected ");
        else if (Modifier.isPrivate(mods)) sb.append("private ");
        
        // Add return type
        sb.append(method.getReturnType().getName()).append(" ");
        
        // Add declaring class and method name
        sb.append(method.getDeclaringClass().getName());
        sb.append(".").append(method.getName());
        
        // Add parameters
        sb.append("(");
        Class<?>[] paramTypes = method.getParameterTypes();
        for (int i = 0; i < paramTypes.length; i++) {
            if (i > 0) sb.append(", ");
            sb.append(paramTypes[i].getName());
        }
        sb.append(")");
        
        return sb.toString();
    }

    private boolean matchesWithin(Method method) {
        String className = method.getDeclaringClass().getName();
        return pattern.matcher(className).matches();
    }

    private boolean matchesAnnotation(Method method) {
        for (Annotation ann : method.getAnnotations()) {
            if (ann.annotationType().getName().equals(annotationClass) ||
                ann.annotationType().getSimpleName().equals(annotationClass)) {
                return true;
            }
        }
        return false;
    }

    private boolean matchesWithinAnnotation(Method method) {
        Class<?> declaringClass = method.getDeclaringClass();
        for (Annotation ann : declaringClass.getAnnotations()) {
            if (ann.annotationType().getName().equals(annotationClass) ||
                ann.annotationType().getSimpleName().equals(annotationClass)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Tests if this pointcut matches a class (for optimization).
     *
     * @param clazz the class to test
     * @return true if any method in the class could match
     */
    public boolean couldMatch(Class<?> clazz) {
        switch (type) {
            case WITHIN:
                return pattern.matcher(clazz.getName()).matches();
            case WITHIN_ANNOTATION:
                for (Annotation ann : clazz.getAnnotations()) {
                    if (ann.annotationType().getName().equals(annotationClass) ||
                        ann.annotationType().getSimpleName().equals(annotationClass)) {
                        return true;
                    }
                }
                return false;
            default:
                return true; // Need to check individual methods
        }
    }

    /**
     * Returns the original expression.
     *
     * @return the expression string
     */
    public String getExpression() {
        return expression;
    }

    /**
     * Returns the pointcut type.
     *
     * @return the type
     */
    public PointcutType getType() {
        return type;
    }

    @Override
    public String toString() {
        return "PointcutExpression{" + expression + "}";
    }
}
