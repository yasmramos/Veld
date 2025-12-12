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

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method to be called after ALL beans in the container have been
 * fully initialized.
 *
 * <p>Unlike {@link PostConstruct} which is called immediately after a bean's
 * dependencies are injected, {@code @PostInitialize} is called only after
 * the entire container initialization is complete. This is useful for
 * operations that require all beans to be available.
 *
 * <h2>Execution Order</h2>
 * <ol>
 *   <li>Bean construction</li>
 *   <li>Dependency injection</li>
 *   <li>{@code @PostConstruct} methods</li>
 *   <li>Container finishes initializing ALL beans</li>
 *   <li>{@code @PostInitialize} methods (this annotation)</li>
 *   <li>Container ready for use</li>
 * </ol>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * @Singleton
 * public class CacheWarmer {
 *     
 *     @Inject
 *     private UserRepository userRepository;
 *     
 *     @Inject
 *     private ProductRepository productRepository;
 *     
 *     @PostInitialize
 *     public void warmCaches() {
 *         // All repositories are guaranteed to be ready
 *         userRepository.preloadCache();
 *         productRepository.preloadCache();
 *     }
 * }
 * }</pre>
 *
 * <h2>Method Requirements</h2>
 * <ul>
 *   <li>Must be public or package-private</li>
 *   <li>Must have no parameters</li>
 *   <li>Return type is typically void (return value is ignored)</li>
 *   <li>May throw exceptions</li>
 * </ul>
 *
 * @author Veld Framework
 * @since 1.0.0-alpha.6
 * @see PostConstruct
 * @see OnStart
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface PostInitialize {
    
    /**
     * Execution order among multiple {@code @PostInitialize} methods.
     * Lower values execute first.
     *
     * @return the order value (default 0)
     */
    int order() default 0;
}
