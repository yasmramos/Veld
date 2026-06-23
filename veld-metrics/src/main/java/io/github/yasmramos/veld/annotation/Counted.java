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
 * Counts method invocations as a metric.
 *
 * <p>The counter is incremented each time the method is called.
 *
 * <p>Example usage:
 * <pre>{@code
 * @Component
 * public class UserService {
 *     
 *     @Counted("users.created")
 *     public User createUser(UserRequest request) {
 *         return userRepository.save(new User(request));
 *     }
 *     
 *     @Counted(value = "users.login", description = "Number of login attempts",
 *              recordFailuresOnly = false)
 *     public void login(String username, String password) {
 *         // Login logic
 *     }
 * }
 * }</pre>
 *
 * @author Veld Framework Team
 * @since 1.1.0
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Counted {
    
    /**
     * Metric name.
     *
     * @return metric name
     */
    String value() default "";
    
    /**
     * Description of the metric.
     *
     * @return description
     */
    String description() default "";
    
    /**
     * Additional tags for the metric.
     *
     * @return extra tags
     */
    String[] extraTags() default {};
    
    /**
     * Whether to only record failures (exceptions).
     *
     * @return true to record only failures
     */
    boolean recordFailuresOnly() default false;
}
