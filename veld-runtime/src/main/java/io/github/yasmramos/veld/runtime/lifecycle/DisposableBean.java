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
 * Interface for beans that need to release resources when the container
 * is destroyed.
 *
 * <p>The {@link #destroy()} method is called when the container is closing.
 * This is an alternative to using {@code @PreDestroy} annotation.
 *
 * <h2>Execution Order</h2>
 * <ol>
 *   <li>Container shutdown initiated</li>
 *   <li>{@code @OnStop} methods</li>
 *   <li>{@code @PreDestroy} methods</li>
 *   <li>{@code destroy()} (this interface)</li>
 *   <li>Container closed</li>
 * </ol>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * @Singleton
 * public class ConnectionPool implements DisposableBean {
 *     
 *     private final List<Connection> connections = new ArrayList<>();
 *     
 *     @Override
 *     public void destroy() throws Exception {
 *         for (Connection conn : connections) {
 *             try {
 *                 conn.close();
 *             } catch (SQLException e) {
 *                 // Log but continue closing others
 *             }
 *         }
 *         connections.clear();
 *     }
 * }
 * }</pre>
 *
 * <h2>Best Practices</h2>
 * <ul>
 *   <li>Always release resources (connections, files, threads)</li>
 *   <li>Log exceptions but don't throw - allow other beans to destroy</li>
 *   <li>Clear collections to help garbage collection</li>
 *   <li>Set references to null if holding large objects</li>
 * </ul>
 *
 * @author Veld Framework
 * @since 1.0.0-alpha.6
 * @see InitializingBean
 * @see io.github.yasmramos.veld.annotation.PreDestroy
 */
public interface DisposableBean {
    
    /**
     * Called by the container when it is being destroyed.
     *
     * <p>This method should release any resources that the bean is holding.
     *
     * @throws Exception if destruction fails (will be logged but not propagated)
     */
    void destroy() throws Exception;
}
