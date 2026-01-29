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
 * Marks a method that always updates the cache.
 *
 * <p>Unlike @Cacheable, the method is always executed and the result is
 * put into the cache. Useful for cache population or update operations.
 *
 * <p>Example usage:
 * <pre>{@code
 * @Component
 * public class UserService {
 *     
 *     @CachePut(value = "users", key = "#user.id")
 *     public User updateUser(User user) {
 *         return userRepository.save(user);
 *     }
 *     
 *     @CachePut(value = "users", key = "#result.id")
 *     public User createUser(String name, String email) {
 *         User user = new User(name, email);
 *         return userRepository.save(user);
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
public @interface CachePut {
    
    /**
     * Cache name(s) to update.
     *
     * @return cache names
     */
    String[] value() default {};
    
    /**
     * SpEL expression for computing the cache key.
     *
     * @return key expression
     */
    String key() default "";
    
    /**
     * SpEL expression for conditional caching.
     *
     * @return condition expression
     */
    String condition() default "";
    
    /**
     * SpEL expression to veto caching.
     *
     * @return unless expression
     */
    String unless() default "";
    
    /**
     * Time-to-live in seconds. 0 means no expiration.
     *
     * @return TTL in seconds
     */
    long ttl() default 0;
}
