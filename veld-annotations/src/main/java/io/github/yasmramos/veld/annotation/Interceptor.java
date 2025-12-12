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
package io.github.yasmramos.veld.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a class as a method interceptor (CDI-style).
 *
 * <p>Interceptors provide a simple way to implement cross-cutting concerns
 * using the interceptor pattern. Unlike aspects, interceptors are bound
 * to target methods through interceptor bindings.
 *
 * <h2>Creating an Interceptor</h2>
 *
 * <pre>{@code
 * @Interceptor
 * @Logged  // Interceptor binding annotation
 * public class LoggingInterceptor {
 *
 *     @AroundInvoke
 *     public Object logMethod(InvocationContext ctx) throws Throwable {
 *         System.out.println("Entering: " + ctx.getMethod().getName());
 *         try {
 *             return ctx.proceed();
 *         } finally {
 *             System.out.println("Exiting: " + ctx.getMethod().getName());
 *         }
 *     }
 * }
 * }</pre>
 *
 * <h2>Using the Interceptor</h2>
 *
 * <pre>{@code
 * @Component
 * public class MyService {
 *
 *     @Logged  // Apply the interceptor binding
 *     public void doSomething() {
 *         // Method will be intercepted
 *     }
 * }
 * }</pre>
 *
 * @author Veld Framework Team
 * @since 1.0.0-alpha.5
 * @see AroundInvoke
 * @see InterceptorBinding
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Interceptor {

    /**
     * The priority of this interceptor.
     *
     * <p>Lower values indicate higher priority (executed first in the chain).
     * Default is {@code 1000}.
     *
     * @return the priority value
     */
    int priority() default 1000;
}
