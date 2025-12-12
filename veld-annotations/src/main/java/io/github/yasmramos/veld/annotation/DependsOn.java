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
 * Specifies explicit initialization dependencies between beans.
 *
 * <p>While Veld automatically handles dependencies based on injection points,
 * there are cases where a bean depends on another bean being fully initialized
 * without having a direct injection reference. This annotation allows you to
 * declare such implicit dependencies.
 *
 * <h2>Use Cases</h2>
 * <ul>
 *   <li>Static initializers that depend on other beans</li>
 *   <li>Event-based dependencies where no direct injection exists</li>
 *   <li>Initialization order requirements for non-injected collaborators</li>
 *   <li>Database schema initialization before repository beans</li>
 * </ul>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // DatabaseMigrator must be initialized before any repository
 * @Singleton
 * public class DatabaseMigrator {
 *     @PostConstruct
 *     public void migrate() {
 *         // Run database migrations
 *     }
 * }
 * 
 * // UserRepository depends on migrations being complete
 * @Singleton
 * @DependsOn("databaseMigrator")
 * public class UserRepository {
 *     // Safe to use database - migrations are complete
 * }
 * 
 * // Multiple dependencies
 * @Singleton
 * @DependsOn({"cacheManager", "configService"})
 * public class ApplicationService {
 *     // Both CacheManager and ConfigService are initialized first
 * }
 * }</pre>
 *
 * <h2>Bean Naming</h2>
 * <p>Bean names follow these conventions:
 * <ul>
 *   <li>Simple class name with first letter lowercase: {@code UserService} -> {@code "userService"}</li>
 *   <li>Custom name via {@code @Named("customName")}</li>
 *   <li>Full qualified class name as fallback</li>
 * </ul>
 *
 * @author Veld Framework
 * @since 1.0.0-alpha.6
 * @see PostConstruct
 * @see PostInitialize
 */
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface DependsOn {
    
    /**
     * Names of beans that this bean depends on.
     * All specified beans will be initialized before this bean.
     *
     * @return array of bean names
     */
    String[] value();
}
