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

import java.lang.reflect.Method;
import java.util.Map;

/**
 * Context for method interception providing access to invocation details.
 *
 * <p>This interface is used by around advice and interceptors to access
 * and control the method invocation. It allows modifying arguments,
 * proceeding with the invocation, and storing context data.</p>
 *
 * <h2>Zero Reflection Design</h2>
 * <p>This interface provides two sets of methods:</p>
 * <ul>
 *   <li><b>Reflection-based:</b> {@link #getMethod()} - kept for backward compatibility</li>
 *   <li><b>Zero-reflection:</b> {@link #getMethodName()}, {@link #returnsVoid()}, etc. -
 *       preferred for new code and native image compatibility</li>
 * </ul>
 *
 * <h2>Usage Example</h2>
 *
 * <pre>{@code
 * @AroundInvoke
 * public Object intercept(InvocationContext ctx) throws Throwable {
 *     // Access method info (zero-reflection way preferred)
 *     String methodName = ctx.getMethodName();
 *     Class<?> declaringClass = ctx.getDeclaringClass();
 *     Object[] args = ctx.getParameters();
 *
 *     // Modify arguments if needed
 *     ctx.setParameters(modifiedArgs);
 *
 *     // Store context data
 *     ctx.getContextData().put("startTime", System.nanoTime());
 *
 *     // Proceed with invocation
 *     Object result = ctx.proceed();
 *
 *     // Access context data
 *     long startTime = (Long) ctx.getContextData().get("startTime");
 *
 *     return result;
 * }
 * }</pre>
 *
 * @author Veld Framework Team
 * @since 1.0.0
 * @see io.github.yasmramos.veld.annotation.AroundInvoke
 * @see io.github.yasmramos.veld.annotation.Around
 */
public interface InvocationContext extends JoinPoint {

    /**
     * Proceeds with the method invocation.
     *
     * <p>If there are more interceptors in the chain, the next interceptor
     * is called. Otherwise, the target method is invoked.</p>
     *
     * @return the result of the method invocation
     * @throws Throwable if the method or any interceptor throws an exception
     */
    Object proceed() throws Throwable;

    /**
     * Returns the parameters that will be passed to the method.
     *
     * @return array of parameters
     */
    Object[] getParameters();

    /**
     * Sets the parameters to be passed to the method.
     *
     * <p>This allows interceptors to modify the arguments before
     * the method is invoked.</p>
     *
     * @param params the new parameters
     * @throws IllegalArgumentException if params is null or has wrong length
     */
    void setParameters(Object[] params);

    /**
     * Returns a mutable map for storing context data.
     *
     * <p>This map can be used to pass data between interceptors
     * in the chain.</p>
     *
     * @return the context data map
     */
    Map<String, Object> getContextData();

    /**
     * Returns the timer instance if available.
     *
     * @return the timer, or null if not in a timer context
     */
    Object getTimer();

    /**
     * Returns the interceptor instance that is currently executing.
     *
     * @return the current interceptor
     */
    Object getInterceptor();

    // ==================== Zero-Reflection Methods ====================

    /**
     * Returns the name of the method being invoked.
     *
     * <p>This is the zero-reflection alternative to {@link #getMethod()}.getName().
     * Use this method for better native image compatibility.</p>
     *
     * @return the method name
     */
    String getMethodName();

    /**
     * Returns whether the method returns void.
     *
     * <p>This is the zero-reflection alternative to checking
     * {@link #getMethod()}.getReturnType() == void.class.
     * Use this method for better native image compatibility.</p>
     *
     * @return true if the method returns void
     */
    boolean returnsVoid();

    /**
     * Returns the parameter types as class objects.
     *
     * <p>This is the zero-reflection alternative to
     * {@link #getMethod()}.getParameterTypes().
     * Use this method for better native image compatibility.</p>
     *
     * @return array of parameter types, or empty array if no parameters
     */
    Class<?>[] getParameterTypes();

    /**
     * Returns the return type class.
     *
     * <p>This is the zero-reflection alternative to
     * {@link #getMethod()}.getReturnType().
     * Use this method for better native image compatibility.</p>
     *
     * @return the return type class, or void.class if the method returns void
     */
    Class<?> getReturnType();

    /**
     * Returns the declaring class name.
     *
     * <p>This is the zero-reflection alternative to
     * {@link #getMethod()}.getDeclaringClass().getName().
     * Use this method for better native image compatibility.</p>
     *
     * @return the fully qualified declaring class name
     */
    String getDeclaringClassName();

    /**
     * Checks if the invoked method has the specified annotation.
     *
     * <p>This is the zero-reflection alternative to
     * {@link #getMethod()}.isAnnotationPresent(annotationClass).
     * Use this method for better native image compatibility.</p>
     *
     * @param annotationClass the annotation class to check
     * @return true if the method has the annotation
     */
    default boolean hasAnnotation(Class<?> annotationClass) {
        return false; // Default implementation for backward compatibility
    }

    /**
     * Gets the annotation of the specified type from the invoked method.
     *
     * <p>This is the zero-reflection alternative to
     * {@link #getMethod()}.getAnnotation(annotationClass).
     * Use this method for better native image compatibility.</p>
     *
     * @param <A> the annotation type
     * @param annotationClass the annotation class to retrieve
     * @return the annotation instance, or null if not present
     */
    default <A extends java.lang.annotation.Annotation> A getAnnotation(Class<A> annotationClass) {
        return null; // Default implementation for backward compatibility
    }

    /**
     * Checks if a parameter at the specified index has the given annotation.
     *
     * <p>This is the zero-reflection alternative to
     * {@link java.lang.reflect.Parameter}.isAnnotationPresent(annotationClass).
     * Use this method for better native image compatibility.</p>
     *
     * @param paramIndex the parameter index (0-based)
     * @param annotationClass the annotation class to check
     * @return true if the parameter has the annotation
     */
    default boolean hasParameterAnnotation(int paramIndex, Class<?> annotationClass) {
        return false; // Default implementation for backward compatibility
    }
}
