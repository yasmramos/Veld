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
 * Interceptor binding for method execution timing.
 *
 * <p>When applied to a method, the execution time is measured and logged.
 *
 * <h2>Usage</h2>
 *
 * <pre>{@code
 * @Component
 * public class DataService {
 *
 *     @Timed
 *     public List<Data> fetchData() {
 *         // Execution time will be measured
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
public @interface Timed {

    /**
     * Time unit for reporting.
     */
    enum Unit {
        NANOSECONDS, MICROSECONDS, MILLISECONDS, SECONDS
    }

    /**
     * The time unit to use for reporting.
     *
     * @return the time unit
     */
    Unit unit() default Unit.MILLISECONDS;

    /**
     * Threshold in the specified unit above which to log a warning.
     * Set to 0 to disable threshold warnings.
     *
     * @return the warning threshold
     */
    long warnThreshold() default 0;
}
