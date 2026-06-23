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
 * Validates that a string matches a regular expression pattern.
 *
 * <p>Example usage:
 * <pre>{@code
 * public class User {
 *     @Pattern(regexp = "^[A-Za-z0-9_]+$", message = "Username can only contain letters, numbers, and underscores")
 *     private String username;
 *     
 *     @Pattern(regexp = "^\\+?[0-9]{10,15}$")
 *     private String phoneNumber;
 * }
 * }</pre>
 *
 * @author Veld Framework Team
 * @since 1.1.0
 */
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Pattern {
    
    /**
     * Regular expression pattern to match.
     */
    String regexp();
    
    /**
     * Regex flags (from java.util.regex.Pattern).
     */
    int flags() default 0;
    
    String message() default "must match pattern '{regexp}'";
    
    Class<?>[] groups() default {};
}
