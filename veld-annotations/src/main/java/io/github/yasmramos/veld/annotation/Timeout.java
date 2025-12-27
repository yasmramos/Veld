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

import java.lang.annotation.*;
import java.util.concurrent.TimeUnit;

/**
 * Marks a method with a timeout constraint.
 *
 * <p>If the method execution exceeds the specified timeout, a TimeoutException
 * is thrown or a fallback method is called.
 *
 * <p>Example usage:
 * <pre>{@code
 * @Component
 * public class ExternalService {
 *     
 *     @Timeout(value = 5000)
 *     public Response callApi(Request request) {
 *         return httpClient.execute(request);
 *     }
 *     
 *     @Timeout(value = 30, unit = TimeUnit.SECONDS, fallbackMethod = "defaultData")
 *     public Data fetchData(String id) {
 *         return slowService.get(id);
 *     }
 * }
 * }</pre>
 *
 * @author Veld Framework Team
 * @since 1.1.0
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Timeout {
    
    /**
     * Timeout duration.
     *
     * @return timeout value, default 1000
     */
    long value() default 1000;
    
    /**
     * Time unit for the timeout value.
     *
     * @return time unit, default MILLISECONDS
     */
    TimeUnit unit() default TimeUnit.MILLISECONDS;
    
    /**
     * Fallback method name to call on timeout.
     * Must have same signature as the annotated method.
     *
     * @return fallback method name
     */
    String fallbackMethod() default "";
    
    /**
     * Whether to interrupt the thread on timeout.
     *
     * @return true to interrupt, default true
     */
    boolean cancelOnTimeout() default true;
}
