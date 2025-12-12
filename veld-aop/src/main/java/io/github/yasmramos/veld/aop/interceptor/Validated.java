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
package io.github.yasmramos.veld.aop.interceptor;

import io.github.yasmramos.veld.annotation.InterceptorBinding;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Interceptor binding for method argument validation.
 *
 * <p>When applied to a method, arguments are validated before execution.
 *
 * <h2>Usage</h2>
 *
 * <pre>{@code
 * @Component
 * public class UserService {
 *
 *     @Validated
 *     public User createUser(@NotNull String name, @NotNull String email) {
 *         // Arguments will be validated
 *     }
 * }
 * }</pre>
 *
 * @author Veld Framework Team
 * @since 1.0.0-alpha.5
 */
@InterceptorBinding
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface Validated {

    /**
     * Whether to throw an exception on validation failure.
     *
     * @return true to throw exception (default), false to log warning
     */
    boolean strict() default true;

    /**
     * Message to include in the exception.
     *
     * @return custom message, or empty for default
     */
    String message() default "";
}
