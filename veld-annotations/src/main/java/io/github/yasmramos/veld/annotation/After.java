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
 * Marks a method as after advice that executes after the target method.
 *
 * <p>After advice runs after the join point completes, regardless of
 * whether it completed normally or threw an exception (like finally).
 *
 * <h2>Advice Types</h2>
 *
 * <p>Use the {@link #type()} attribute to specify when to run:
 * <ul>
 *   <li>{@code FINALLY} (default) - always runs after the method</li>
 *   <li>{@code RETURNING} - only runs if method completed normally</li>
 *   <li>{@code THROWING} - only runs if method threw an exception</li>
 * </ul>
 *
 * <h2>Usage Examples</h2>
 *
 * <pre>{@code
 * @Aspect
 * public class ResourceAspect {
 *
 *     @After("execution(* com.example.dao.*.*(..))")
 *     public void closeResources(JoinPoint jp) {
 *         System.out.println("Method completed: " + jp.getMethodName());
 *     }
 *
 *     @After(value = "execution(* com.example.service.*.*(..))", type = AfterType.RETURNING)
 *     public void onSuccess(JoinPoint jp, Object result) {
 *         audit.logSuccess(jp.getMethodName(), result);
 *     }
 *
 *     @After(value = "execution(* com.example.service.*.*(..))", type = AfterType.THROWING)
 *     public void onError(JoinPoint jp, Throwable error) {
 *         audit.logError(jp.getMethodName(), error);
 *     }
 * }
 * }</pre>
 *
 * @author Veld Framework Team
 * @since 1.0.0
 * @see Aspect
 * @see Around
 * @see Before
 * @see AfterType
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface After {

    /**
     * The pointcut expression defining which join points this advice applies to.
     *
     * @return the pointcut expression
     */
    String value();

    /**
     * When this after advice should execute.
     *
     * @return the after advice type
     */
    AfterType type() default AfterType.FINALLY;
}
