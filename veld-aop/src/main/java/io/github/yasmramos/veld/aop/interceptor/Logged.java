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
 * Interceptor binding for method logging.
 *
 * <p>When applied to a method or class, causes the method invocation
 * to be logged (entry, exit, arguments, return value).
 *
 * <h2>Usage</h2>
 *
 * <pre>{@code
 * @Component
 * public class OrderService {
 *
 *     @Logged
 *     public Order createOrder(OrderRequest request) {
 *         // Method entry/exit will be logged
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
public @interface Logged {

    /**
     * Log level for the messages.
     */
    enum Level {
        TRACE, DEBUG, INFO, WARN, ERROR
    }

    /**
     * The log level to use.
     *
     * @return the log level
     */
    Level level() default Level.INFO;

    /**
     * Whether to log method arguments.
     *
     * @return true to log arguments
     */
    boolean logArgs() default true;

    /**
     * Whether to log the return value.
     *
     * @return true to log return value
     */
    boolean logResult() default true;

    /**
     * Whether to log execution time.
     *
     * @return true to log execution time
     */
    boolean logTime() default false;
}
