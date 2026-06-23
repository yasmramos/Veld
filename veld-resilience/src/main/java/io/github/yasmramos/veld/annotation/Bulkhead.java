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
 * Marks a method to be protected by a bulkhead pattern.
 *
 * <p>The bulkhead pattern isolates resources to prevent failures in one
 * part of the system from cascading to others by limiting concurrent executions.
 *
 * <p>Types:
 * <ul>
 *   <li>SEMAPHORE - Limits concurrent calls using a semaphore</li>
 *   <li>THREADPOOL - Isolates calls in a separate thread pool</li>
 * </ul>
 *
 * <p>Example usage:
 * <pre>{@code
 * @Component
 * public class ResourceService {
 *     
 *     @Bulkhead(maxConcurrent = 10)
 *     public Result processRequest(Request request) {
 *         return heavyProcessing(request);
 *     }
 *     
 *     @Bulkhead(maxConcurrent = 5, maxWait = 1000, type = BulkheadType.THREADPOOL)
 *     public Data fetchData(String id) {
 *         return externalService.get(id);
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
public @interface Bulkhead {
    
    /**
     * Name of the bulkhead instance.
     * Methods with the same name share the same bulkhead.
     *
     * @return bulkhead name, default uses method name
     */
    String name() default "";
    
    /**
     * Maximum number of concurrent calls allowed.
     *
     * @return max concurrent calls, default 10
     */
    int maxConcurrent() default 10;
    
    /**
     * Maximum time in milliseconds to wait for permission.
     *
     * @return max wait time, default 0 (no waiting)
     */
    long maxWait() default 0;
    
    /**
     * Bulkhead type: SEMAPHORE or THREADPOOL.
     *
     * @return bulkhead type, default SEMAPHORE
     */
    Type type() default Type.SEMAPHORE;
    
    /**
     * Core thread pool size (only for THREADPOOL type).
     *
     * @return core pool size, default 5
     */
    int coreSize() default 5;
    
    /**
     * Maximum thread pool size (only for THREADPOOL type).
     *
     * @return max pool size, default 10
     */
    int maxSize() default 10;
    
    /**
     * Queue capacity for thread pool (only for THREADPOOL type).
     *
     * @return queue capacity, default 100
     */
    int queueCapacity() default 100;
    
    /**
     * Fallback method name when bulkhead rejects the call.
     *
     * @return fallback method name
     */
    String fallbackMethod() default "";
    
    /**
     * Bulkhead type enumeration.
     */
    enum Type {
        SEMAPHORE,
        THREADPOOL
    }
}
