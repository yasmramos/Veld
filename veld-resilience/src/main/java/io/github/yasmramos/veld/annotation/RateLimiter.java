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
 * Marks a method for rate limiting.
 *
 * <p>When applied to a method, calls will be limited to a maximum rate.
 * Excess calls will either block, throw an exception, or return a fallback.
 *
 * <p>Example usage:
 * <pre>{@code
 * @Component
 * public class ApiService {
 *     
 *     @RateLimiter(permits = 10, period = 1000)
 *     public Response callExternalApi(Request request) {
 *         return httpClient.execute(request);
 *     }
 *     
 *     @RateLimiter(permits = 100, period = 60000, blocking = false)
 *     public Data getData(String id) {
 *         return repository.findById(id);
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
public @interface RateLimiter {
    
    /**
     * Maximum number of permits per period.
     *
     * @return permits per period, default 10
     */
    int permits() default 10;
    
    /**
     * Time period in milliseconds.
     *
     * @return period in milliseconds, default 1000 (1 second)
     */
    long period() default 1000;
    
    /**
     * Whether to block when rate limit is exceeded.
     * If false, throws RateLimitExceededException.
     *
     * @return true to block, false to throw
     */
    boolean blocking() default true;
    
    /**
     * Maximum wait time in milliseconds when blocking.
     * 0 means wait indefinitely.
     *
     * @return max wait time, default 5000 (5 seconds)
     */
    long timeout() default 5000;
    
    /**
     * Key for rate limiter grouping.
     * Methods with the same key share the same rate limit.
     *
     * @return limiter key, default uses method name
     */
    String key() default "";
}
