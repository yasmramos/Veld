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
 * Marks a method for caching its return value.
 *
 * <p>When applied to a method, the result will be cached based on the method
 * arguments. Subsequent calls with the same arguments will return the cached
 * value without executing the method.
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
 *     @Cacheable(value = "users", ttl = 300000)
 *     public List<User> findAll() {
 *         return userRepository.findAll();
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
     * The name of the cache to use.
     *
     * @return the cache name
     */
    String value() default "default";
    
    /**
     * Custom cache key SpEL expression.
     * If empty, the key is generated from method arguments.
     *
     * @return the key expression
     */
    String key() default "";
    
    /**
     * Time to live in milliseconds.
     * A value of 0 means no expiration.
     *
     * @return TTL in milliseconds
     */
    long ttl() default 0;
    
    /**
     * Maximum number of entries in the cache.
     * A value of 0 means unlimited.
     *
     * @return max size
     */
    int maxSize() default 1000;
    
    /**
     * Condition SpEL expression to determine if caching should occur.
     * If empty, always cache.
     *
     * @return the condition expression
     */
    String condition() default "";
    
    /**
     * Unless SpEL expression to veto caching.
     * If the expression evaluates to true, the result is not cached.
     *
     * @return the unless expression
     */
    String unless() default "";
}
