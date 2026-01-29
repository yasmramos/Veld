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
 * Marks a method that triggers cache eviction.
 *
 * <p>When the annotated method is called, specified cache entries are removed.
 *
 * <p>Example usage:
 * <pre>{@code
 * @Component
 * public class UserService {
 *     
 *     @CacheEvict("users")
 *     public void deleteUser(Long id) {
 *         userRepository.deleteById(id);
 *     }
 *     
 *     @CacheEvict(value = "users", allEntries = true)
 *     public void clearUserCache() {
 *         // Clears entire cache
 *     }
 *     
 *     @CacheEvict(value = "users", key = "#user.id", beforeInvocation = true)
 *     public void updateUser(User user) {
 *         userRepository.save(user);
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
public @interface CacheEvict {
    
    /**
     * Cache name(s) to evict from.
     *
     * @return cache names
     */
    String[] value() default {};
    
    /**
     * SpEL expression for computing the cache key to evict.
     *
     * @return key expression
     */
    String key() default "";
    
    /**
     * SpEL expression for conditional eviction.
     *
     * @return condition expression
     */
    String condition() default "";
    
    /**
     * Whether to evict all entries in the cache.
     *
     * @return true to evict all, default false
     */
    boolean allEntries() default false;
    
    /**
     * Whether to evict before method invocation.
     * Default is after successful invocation.
     *
     * @return true to evict before, default false
     */
    boolean beforeInvocation() default false;
}
