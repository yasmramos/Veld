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
package io.github.yasmramos.veld.runtime.lifecycle;

/**
 * Interface for beans that need to perform initialization after all
 * properties have been set.
 *
 * <p>The {@link #afterPropertiesSet()} method is called after the container
 * has set all bean properties (dependencies). This is an alternative to
 * using {@code @PostConstruct} annotation.
 *
 * <h2>Execution Order</h2>
 * <ol>
 *   <li>Bean instantiation</li>
 *   <li>Dependency injection</li>
 *   <li>{@code afterPropertiesSet()} (this interface)</li>
 *   <li>{@code @PostConstruct} methods</li>
 * </ol>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * @Singleton
 * public class DataSourceConfig implements InitializingBean {
 *     
 *     @Inject
 *     private ConfigService config;
 *     
 *     private DataSource dataSource;
 *     
 *     @Override
 *     public void afterPropertiesSet() throws Exception {
 *         // ConfigService is guaranteed to be injected
 *         String url = config.get("db.url");
 *         String user = config.get("db.user");
 *         String password = config.get("db.password");
 *         
 *         dataSource = createDataSource(url, user, password);
 *     }
 *     
 *     public DataSource getDataSource() {
 *         return dataSource;
 *     }
 * }
 * }</pre>
 *
 * <h2>When to Use</h2>
 * <ul>
 *   <li>Complex initialization logic after injection</li>
 *   <li>Validation of injected dependencies</li>
 *   <li>Setting up derived properties</li>
 *   <li>Programmatic alternative to {@code @PostConstruct}</li>
 * </ul>
 *
 * @author Veld Framework
 * @since 1.0.0-alpha.6
 * @see DisposableBean
 * @see io.github.yasmramos.veld.annotation.PostConstruct
 */
public interface InitializingBean {
    
    /**
     * Called by the container after all properties have been set.
     *
     * <p>This method allows the bean to perform initialization that
     * requires all injected dependencies to be available.
     *
     * @throws Exception if initialization fails
     */
    void afterPropertiesSet() throws Exception;
}
