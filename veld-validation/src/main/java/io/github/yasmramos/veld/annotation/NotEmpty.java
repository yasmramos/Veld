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
 * Marks a field as non-empty.
 *
 * <p>The annotated element must not be null and must have a size/length greater than 0.
 * Applies to: String, Collection, Map, Array.
 *
 * <p>Example usage:
 * <pre>{@code
 * public class Order {
 *     @NotEmpty(message = "Order must have at least one item")
 *     private List<Item> items;
 *     
 *     @NotEmpty
 *     private String customerId;
 * }
 * }</pre>
 *
 * @author Veld Framework Team
 * @since 1.1.0
 */
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface NotEmpty {
    
    String message() default "must not be empty";
    
    Class<?>[] groups() default {};
}
