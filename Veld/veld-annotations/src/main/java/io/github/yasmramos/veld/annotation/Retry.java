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

/**
 * Marks a method for automatic retry on failure.
 *
 * <p>When applied to a method, the method will be automatically retried
 * if it throws an exception, with configurable backoff strategy.
 *
 * <p>Example usage:
 * <pre>{@code
 * @Component
 * public class ExternalApiClient {
 *     
 *     @Retry(maxAttempts = 3, delay = 1000)
 *     public Response callApi(Request request) {
 *         return httpClient.execute(request);
 *     }
 *     
 *     @Retry(maxAttempts = 5, delay = 500, multiplier = 2.0, include = {IOException.class})
 *     public Data fetchData(String id) {
 *         return remoteService.get(id);
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
public @interface Retry {
    
    /**
     * Maximum number of attempts (including the initial call).
     *
     * @return max attempts, default 3
     */
    int maxAttempts() default 3;
    
    /**
     * Initial delay between retries in milliseconds.
     *
     * @return delay in milliseconds, default 1000
     */
    long delay() default 1000;
    
    /**
     * Multiplier for exponential backoff.
     * Each retry delay = previous delay * multiplier.
     *
     * @return multiplier, default 1.0 (no exponential backoff)
     */
    double multiplier() default 1.0;
    
    /**
     * Maximum delay between retries in milliseconds.
     * Used to cap exponential backoff.
     *
     * @return max delay, default 30000 (30 seconds)
     */
    long maxDelay() default 30000;
    
    /**
     * Exception types that should trigger a retry.
     * If empty, all exceptions trigger retry.
     *
     * @return exception classes to retry on
     */
    Class<? extends Throwable>[] include() default {};
    
    /**
     * Exception types that should NOT trigger a retry.
     *
     * @return exception classes to exclude from retry
     */
    Class<? extends Throwable>[] exclude() default {};
}
