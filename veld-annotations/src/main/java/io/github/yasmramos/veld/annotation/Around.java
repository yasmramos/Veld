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
 * Marks a method as around advice that wraps the target method execution.
 *
 * <p>Around advice is the most powerful type of advice. It can control
 * whether the target method is executed, modify arguments, catch exceptions,
 * and modify the return value.
 *
 * <h2>Pointcut Expression Syntax</h2>
 *
 * <p>The pointcut expression supports the following patterns:
 * <ul>
 *   <li>{@code execution(modifiers? returnType package.class.method(params))} - method execution</li>
 *   <li>{@code within(package..*)} - all methods within a package</li>
 *   <li>{@code @annotation(com.example.MyAnnotation)} - methods with annotation</li>
 * </ul>
 *
 * <h2>Wildcards</h2>
 * <ul>
 *   <li>{@code *} - matches any single element</li>
 *   <li>{@code ..} - matches any number of elements (packages or parameters)</li>
 * </ul>
 *
 * <h2>Usage Examples</h2>
 *
 * <pre>{@code
 * @Aspect
 * public class TransactionAspect {
 *
 *     // All methods in service package
 *     @Around("execution(* com.example.service.*.*(..))")
 *     public Object transactional(InvocationContext ctx) throws Throwable {
 *         Transaction tx = beginTransaction();
 *         try {
 *             Object result = ctx.proceed();
 *             tx.commit();
 *             return result;
 *         } catch (Exception e) {
 *             tx.rollback();
 *             throw e;
 *         }
 *     }
 *
 *     // Methods annotated with @Cacheable
 *     @Around("@annotation(com.example.Cacheable)")
 *     public Object cache(InvocationContext ctx) throws Throwable {
 *         String key = generateKey(ctx);
 *         Object cached = cache.get(key);
 *         if (cached != null) return cached;
 *         Object result = ctx.proceed();
 *         cache.put(key, result);
 *         return result;
 *     }
 * }
 * }</pre>
 *
 * @author Veld Framework Team
 * @since 1.0.0
 * @see Aspect
 * @see Before
 * @see After
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Around {

    /**
     * The pointcut expression defining which join points this advice applies to.
     *
     * @return the pointcut expression
     */
    String value();
}
