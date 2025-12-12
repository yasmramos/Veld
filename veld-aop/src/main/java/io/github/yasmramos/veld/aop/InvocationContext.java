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
 * proceeding with the invocation, and storing context data.
 *
 * <h2>Usage Example</h2>
 *
 * <pre>{@code
 * @AroundInvoke
 * public Object intercept(InvocationContext ctx) throws Throwable {
 *     // Access method info
 *     Method method = ctx.getMethod();
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
 * @since 1.0.0-alpha.5
 * @see io.github.yasmramos.veld.annotation.AroundInvoke
 * @see io.github.yasmramos.veld.annotation.Around
 */
public interface InvocationContext extends JoinPoint {

    /**
     * Proceeds with the method invocation.
     *
     * <p>If there are more interceptors in the chain, the next interceptor
     * is called. Otherwise, the target method is invoked.
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
     * the method is invoked.
     *
     * @param params the new parameters
     * @throws IllegalArgumentException if params is null or has wrong length
     */
    void setParameters(Object[] params);

    /**
     * Returns a mutable map for storing context data.
     *
     * <p>This map can be used to pass data between interceptors
     * in the chain.
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
}
