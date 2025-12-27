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
 * Validates that the size of an element is within specified bounds.
 *
 * <p>Applies to: String, Collection, Map, Array.
 *
 * <p>Example usage:
 * <pre>{@code
 * public class User {
 *     @Size(min = 2, max = 50, message = "Name must be between 2 and 50 characters")
 *     private String name;
 *     
 *     @Size(max = 10)
 *     private List<String> tags;
 * }
 * }</pre>
 *
 * @author Veld Framework Team
 * @since 1.1.0
 */
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Size {
    
    int min() default 0;
    
    int max() default Integer.MAX_VALUE;
    
    String message() default "size must be between {min} and {max}";
    
    Class<?>[] groups() default {};
}
