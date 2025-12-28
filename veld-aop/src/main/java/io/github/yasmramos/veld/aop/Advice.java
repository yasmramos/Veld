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
import io.github.yasmramos.veld.aop.pointcut.CompositePointcut;

import java.lang.reflect.Method;

/**
 * Represents an advice (around, before, after) in an aspect.
 *
 * @author Veld Framework Team
 * @since 1.0.0
 */
public class Advice implements Comparable<Advice> {

    /**
     * Types of advice.
     */
    public enum Type {
        AROUND,
        BEFORE,
        AFTER_RETURNING,
        AFTER_THROWING,
        AFTER_FINALLY
    }

    private final Type type;
    private final CompositePointcut pointcut;
    private final Object aspectInstance;
    private final Method adviceMethod;
    private final int order;

    /**
     * Creates a new advice.
     *
     * @param type           the advice type
     * @param pointcut       the pointcut expression
     * @param aspectInstance the aspect instance
     * @param adviceMethod   the advice method
     * @param order          the order (lower = higher priority)
     */
    public Advice(Type type, CompositePointcut pointcut, Object aspectInstance,
                  Method adviceMethod, int order) {
        this.type = type;
        this.pointcut = pointcut;
        this.aspectInstance = aspectInstance;
        this.adviceMethod = adviceMethod;
        this.order = order;
        this.adviceMethod.setAccessible(true);
    }

    /**
     * Creates an advice from an Around annotation.
     */
    public static Advice around(String expression, Object aspect, Method method, int order) {
        return new Advice(Type.AROUND, CompositePointcut.parse(expression),
                aspect, method, order);
    }

    /**
     * Creates an advice from a Before annotation.
     */
    public static Advice before(String expression, Object aspect, Method method, int order) {
        return new Advice(Type.BEFORE, CompositePointcut.parse(expression),
                aspect, method, order);
    }

    /**
     * Creates an advice from an After annotation.
     */
    public static Advice after(String expression, AfterType afterType, Object aspect,
                               Method method, int order) {
        Type type;
        switch (afterType) {
            case RETURNING:
                type = Type.AFTER_RETURNING;
                break;
            case THROWING:
                type = Type.AFTER_THROWING;
                break;
            default:
                type = Type.AFTER_FINALLY;
        }
        return new Advice(type, CompositePointcut.parse(expression), aspect, method, order);
    }

    /**
     * Tests if this advice applies to a method.
     *
     * @param method the method to test
     * @return true if this advice should be applied
     */
    public boolean matches(Method method) {
        return pointcut.matches(method);
    }

    /**
     * Tests if this advice could apply to any method in a class.
     *
     * @param clazz the class to test
     * @return true if this advice could apply
     */
    public boolean couldMatch(Class<?> clazz) {
        return pointcut.couldMatch(clazz);
    }

    /**
     * Invokes the advice method.
     *
     * @param args the arguments to pass
     * @return the result of the advice
     * @throws Throwable if the advice throws an exception
     */
    public Object invoke(Object... args) throws Throwable {
        try {
            return adviceMethod.invoke(aspectInstance, args);
        } catch (java.lang.reflect.InvocationTargetException e) {
            throw e.getCause();
        }
    }

    public Type getType() {
        return type;
    }

    public CompositePointcut getPointcut() {
        return pointcut;
    }

    public Object getAspectInstance() {
        return aspectInstance;
    }

    public Method getAdviceMethod() {
        return adviceMethod;
    }

    public int getOrder() {
        return order;
    }

    @Override
    public int compareTo(Advice other) {
        return Integer.compare(this.order, other.order);
    }

    @Override
    public String toString() {
        return "Advice{" +
                "type=" + type +
                ", method=" + adviceMethod.getDeclaringClass().getSimpleName() +
                "." + adviceMethod.getName() +
                ", order=" + order +
                '}';
    }
}
