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
 * Marks a method whose result should be cached.
 *
 * <p>When a cached method is called, the cache is checked first. If a cached
 * value exists, it is returned without executing the method. Otherwise, the
 * method is executed and the result is cached.
 *
 * <p>Example usage:
 * <pre>{@code
 * @Component
 * public class UserService {
 *     
 *     @Cacheable("users")
 *     public User findById(Long id) {
 *         return userRepository.findById(id);
 *     }
 *     
 *     @Cacheable(value = "users", key = "#username", ttl = 3600)
 *     public User findByUsername(String username) {
 *         return userRepository.findByUsername(username);
 *     }
 *     
 *     @Cacheable(value = "users", condition = "#id > 0", unless = "#result == null")
 *     public User findUser(Long id) {
 *         return userRepository.findById(id);
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
public @interface Cacheable {
    
    /**
     * Cache name(s) to use.
     *
     * @return cache names
     */
    String[] value() default {};
    
    /**
     * SpEL expression for computing the cache key.
     * Default uses all method parameters.
     *
     * @return key expression
     */
    String key() default "";
    
    /**
     * SpEL expression for conditional caching.
     * If false, method is executed without caching.
     *
     * @return condition expression
     */
    String condition() default "";
    
    /**
     * SpEL expression to veto caching.
     * If true, result is not cached (but may be retrieved from cache).
     *
     * @return unless expression
     */
    String unless() default "";
    
    /**
     * Time-to-live in seconds. 0 means no expiration.
     *
     * @return TTL in seconds, default 0
     */
    long ttl() default 0;
    
    /**
     * Whether to cache null values.
     *
     * @return true to cache nulls, default false
     */
    boolean cacheNull() default false;
    
    /**
     * Whether to synchronize cache access.
     * Prevents cache stampede for expensive operations.
     *
     * @return true to synchronize, default false
     */
    boolean sync() default false;
}
