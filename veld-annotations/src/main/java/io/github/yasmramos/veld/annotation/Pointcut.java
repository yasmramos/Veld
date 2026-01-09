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
 * Defines a reusable pointcut expression.
 *
 * <p>Pointcuts can be referenced by their method name in other advice
 * annotations, allowing for reuse and composition.
 *
 * <h2>Expression Syntax</h2>
 *
 * <p>Pointcut expressions support:
 * <ul>
 *   <li>{@code execution(modifiers? returnType declaringType.method(params))} - method execution</li>
 *   <li>{@code within(type)} - all join points within a type</li>
 *   <li>{@code @annotation(annotationType)} - methods with annotation</li>
 *   <li>{@code @within(annotationType)} - types with annotation</li>
 *   <li>{@code args(types)} - methods with matching argument types</li>
 * </ul>
 *
 * <h2>Wildcards</h2>
 * <ul>
 *   <li>{@code *} - matches any single element</li>
 *   <li>{@code ..} - matches zero or more elements</li>
 *   <li>{@code +} - matches subtypes</li>
 * </ul>
 *
 * <h2>Logical Operators</h2>
 * <ul>
 *   <li>{@code &&} - AND</li>
 *   <li>{@code ||} - OR</li>
 *   <li>{@code !} - NOT</li>
 * </ul>
 *
 * <h2>Usage Examples</h2>
 *
 * <pre>{@code
 * @Aspect
 * public class MyAspect {
 *
 *     // Define reusable pointcuts
 *     @Pointcut("execution(* com.example.service.*.*(..))")
 *     public void serviceMethods() {}
 *
 *     @Pointcut("execution(* com.example.dao.*.*(..))")
 *     public void daoMethods() {}
 *
 *     @Pointcut("serviceMethods() || daoMethods()")
 *     public void businessMethods() {}
 *
 *     // Use the pointcut
 *     @Around("businessMethods()")
 *     public Object logBusinessMethod(InvocationContext ctx) throws Throwable {
 *         // ...
 *     }
 * }
 * }</pre>
 *
 * @author Veld Framework Team
 * @since 1.0.0
 * @see Aspect
 * @see Around
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Pointcut {

    /**
     * The pointcut expression.
     *
     * @return the pointcut expression
     */
    String value();
}
