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

/**
 * Interface for method interceptors.
 *
 * <p>Interceptors intercept method invocations and can perform actions
 * before and after the method execution, modify parameters, or even
 * prevent the method from being called.
 *
 * <h2>Usage Example</h2>
 *
 * <pre>{@code
 * public class LoggingInterceptor implements MethodInterceptor {
 *     @Override
 *     public Object invoke(InvocationContext ctx) throws Throwable {
 *         System.out.println("Before: " + ctx.getMethod().getName());
 *         try {
 *             Object result = ctx.proceed();
 *             System.out.println("After: " + ctx.getMethod().getName());
 *             return result;
 *         } catch (Throwable t) {
 *             System.out.println("Exception: " + t.getMessage());
 *             throw t;
 *         }
 *     }
 * }
 * }</pre>
 *
 * @author Veld Framework Team
 * @since 1.0.0
 * @see InvocationContext
 */
@FunctionalInterface
public interface MethodInterceptor {

    /**
     * Intercepts a method invocation.
     *
     * <p>Implementations should call {@link InvocationContext#proceed()}
     * to continue the interceptor chain and eventually invoke the target method.
     *
     * @param ctx the invocation context
     * @return the result of the method invocation
     * @throws Throwable if the method or interceptor throws an exception
     */
    Object invoke(InvocationContext ctx) throws Throwable;
}
