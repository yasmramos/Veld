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
 * Marks a method or class as requiring authentication.
 *
 * <p>The annotated element can only be accessed by authenticated users
 * with the specified roles.
 *
 * <p>Example usage:
 * <pre>{@code
 * @Component
 * @Secured  // All methods require authentication
 * public class AdminService {
 *     
 *     @Secured({"ROLE_ADMIN"})
 *     public void deleteUser(Long id) {
 *         userRepository.deleteById(id);
 *     }
 *     
 *     @Secured({"ROLE_ADMIN", "ROLE_MANAGER"})
 *     public List<User> getAllUsers() {
 *         return userRepository.findAll();
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
public @interface Secured {
    
    /**
     * Required roles for access.
     * If empty, only authentication is required.
     *
     * @return required roles
     */
    String[] value() default {};
}
