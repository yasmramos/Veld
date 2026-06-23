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
 * Validates that a string is a valid email address.
 *
 * <p>Example usage:
 * <pre>{@code
 * public class User {
 *     @Email(message = "Please provide a valid email address")
 *     private String email;
 *     
 *     @Email(regexp = ".*@company\\.com", message = "Must be a company email")
 *     private String workEmail;
 * }
 * }</pre>
 *
 * @author Veld Framework Team
 * @since 1.1.0
 */
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Email {
    
    String message() default "must be a valid email address";
    
    /**
     * Optional regex pattern for additional validation.
     */
    String regexp() default ".*";
    
    Class<?>[] groups() default {};
}
