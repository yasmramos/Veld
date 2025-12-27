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
 * Specifies the list of roles permitted to access the annotated method or class.
 *
 * <p>JSR-250 compatible annotation for role-based access control.
 *
 * <p>Example usage:
 * <pre>{@code
 * @Component
 * public class UserService {
 *     
 *     @RolesAllowed("ADMIN")
 *     public void deleteUser(Long id) {
 *         userRepository.deleteById(id);
 *     }
 *     
 *     @RolesAllowed({"ADMIN", "MANAGER"})
 *     public void updateUser(User user) {
 *         userRepository.save(user);
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
public @interface RolesAllowed {
    
    /**
     * List of roles that are permitted access.
     *
     * @return allowed roles
     */
    String[] value();
}
