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
 * Marks a parameter or field for cascading validation.
 *
 * <p>When applied, the validation framework will recursively validate
 * the annotated object's constraints.
 *
 * <p>Example usage:
 * <pre>{@code
 * @Component
 * public class UserService {
 *     
 *     public void createUser(@Valid UserDTO user) {
 *         // user is validated before method execution
 *     }
 * }
 * 
 * public class UserDTO {
 *     @NotNull
 *     private String name;
 *     
 *     @Valid
 *     private AddressDTO address; // Cascading validation
 * }
 * }</pre>
 *
 * @author Veld Framework Team
 * @since 1.1.0
 */
@Target({ElementType.PARAMETER, ElementType.FIELD, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Valid {
    
    /**
     * Validation groups to apply.
     *
     * @return validation groups
     */
    Class<?>[] groups() default {};
}
