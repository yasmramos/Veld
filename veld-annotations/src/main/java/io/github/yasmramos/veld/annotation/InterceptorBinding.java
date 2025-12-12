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
 * Meta-annotation for creating interceptor binding annotations.
 *
 * <p>Interceptor bindings link interceptors to their target methods or classes.
 * An annotation marked with {@code @InterceptorBinding} can be applied to
 * both an interceptor class and target methods/classes.
 *
 * <h2>Creating an Interceptor Binding</h2>
 *
 * <pre>{@code
 * @InterceptorBinding
 * @Retention(RetentionPolicy.RUNTIME)
 * @Target({ElementType.METHOD, ElementType.TYPE})
 * public @interface Logged {
 *     // Optional attributes
 *     LogLevel level() default LogLevel.INFO;
 * }
 * }</pre>
 *
 * <h2>Using the Binding</h2>
 *
 * <pre>{@code
 * // Define the interceptor
 * @Interceptor
 * @Logged
 * public class LoggingInterceptor {
 *     @AroundInvoke
 *     public Object log(InvocationContext ctx) throws Throwable {
 *         // ...
 *     }
 * }
 *
 * // Apply to a method
 * @Component
 * public class OrderService {
 *     @Logged(level = LogLevel.DEBUG)
 *     public Order createOrder(OrderRequest request) {
 *         // ...
 *     }
 * }
 * }</pre>
 *
 * @author Veld Framework Team
 * @since 1.0.0-alpha.5
 * @see Interceptor
 * @see AroundInvoke
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.ANNOTATION_TYPE)
public @interface InterceptorBinding {
}
