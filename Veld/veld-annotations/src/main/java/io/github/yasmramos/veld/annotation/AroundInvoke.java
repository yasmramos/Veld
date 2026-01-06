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
 * Marks a method in an {@link Interceptor} as the interception method.
 *
 * <p>The annotated method will be invoked around the target method execution.
 * It must have the following signature:
 *
 * <pre>{@code
 * @AroundInvoke
 * public Object intercept(InvocationContext ctx) throws Throwable {
 *     // Before logic
 *     Object result = ctx.proceed();  // Call target method
 *     // After logic
 *     return result;
 * }
 * }</pre>
 *
 * <h2>Usage Example</h2>
 *
 * <pre>{@code
 * @Interceptor
 * @Timed
 * public class TimingInterceptor {
 *
 *     @AroundInvoke
 *     public Object measureTime(InvocationContext ctx) throws Throwable {
 *         long start = System.nanoTime();
 *         try {
 *             return ctx.proceed();
 *         } finally {
 *             long elapsed = System.nanoTime() - start;
 *             System.out.printf("%s took %d ns%n",
 *                 ctx.getMethod().getName(), elapsed);
 *         }
 *     }
 * }
 * }</pre>
 *
 * @author Veld Framework Team
 * @since 1.0.0
 * @see Interceptor
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface AroundInvoke {
}
