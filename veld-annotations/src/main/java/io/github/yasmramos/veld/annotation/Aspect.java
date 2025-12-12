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
 * Marks a class as an Aspect containing cross-cutting concerns.
 *
 * <p>Aspects are special components that define pointcuts and advice
 * for intercepting method executions across the application.
 *
 * <h2>Usage Example</h2>
 *
 * <pre>{@code
 * @Aspect
 * public class LoggingAspect {
 *
 *     @Around("execution(* com.example.service.*.*(..))")
 *     public Object logMethodCall(InvocationContext ctx) throws Throwable {
 *         System.out.println("Before: " + ctx.getMethod().getName());
 *         Object result = ctx.proceed();
 *         System.out.println("After: " + ctx.getMethod().getName());
 *         return result;
 *     }
 * }
 * }</pre>
 *
 * <h2>Aspect Ordering</h2>
 *
 * <p>When multiple aspects apply to the same join point, the {@link #order()}
 * attribute determines the execution order. Lower values have higher priority.
 *
 * @author Veld Framework Team
 * @since 1.0.0-alpha.5
 * @see Around
 * @see Before
 * @see After
 * @see Pointcut
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Aspect {

    /**
     * Optional name for this aspect.
     *
     * <p>If not specified, the simple class name will be used.
     *
     * @return the aspect name
     */
    String value() default "";

    /**
     * The order of this aspect when multiple aspects apply.
     *
     * <p>Lower values indicate higher priority (executed first for
     * before advice, last for after advice).
     *
     * <p>Default is {@code Integer.MAX_VALUE} (lowest priority).
     *
     * @return the order value
     */
    int order() default Integer.MAX_VALUE;
}
