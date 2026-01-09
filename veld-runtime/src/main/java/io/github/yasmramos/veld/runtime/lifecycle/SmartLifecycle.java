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
 * Extended {@link Lifecycle} interface for beans that require fine-grained
 * control over startup and shutdown order.
 *
 * <p>The {@link #getPhase()} method determines the order in which beans
 * are started and stopped. Lower phase values start first and stop last.
 *
 * <h2>Phase Values</h2>
 * <ul>
 *   <li>{@code Integer.MIN_VALUE}: First to start, last to stop</li>
 *   <li>{@code 0}: Default phase</li>
 *   <li>{@code Integer.MAX_VALUE}: Last to start, first to stop</li>
 * </ul>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * @Singleton
 * public class DatabaseConnection implements SmartLifecycle {
 *     
 *     private boolean running = false;
 *     private Connection connection;
 *     
 *     @Override
 *     public void start() {
 *         connection = DriverManager.getConnection(url);
 *         running = true;
 *     }
 *     
 *     @Override
 *     public void stop() {
 *         connection.close();
 *         running = false;
 *     }
 *     
 *     @Override
 *     public boolean isRunning() {
 *         return running;
 *     }
 *     
 *     @Override
 *     public int getPhase() {
 *         // Start early (low value), stop late
 *         return -1000;
 *     }
 *     
 *     @Override
 *     public boolean isAutoStartup() {
 *         return true; // Start automatically with context
 *     }
 * }
 * 
 * @Singleton
 * public class UserRepository implements SmartLifecycle {
 *     
 *     @Override
 *     public int getPhase() {
 *         // Start after database (-1000), stop before
 *         return 0;
 *     }
 *     
 *     // ... other methods
 * }
 * }</pre>
 *
 * <h2>Async Stop</h2>
 * <p>The {@link #stop(Runnable)} method allows for asynchronous shutdown.
 * The callback must be invoked when shutdown is complete.
 *
 * @author Veld Framework
 * @since 1.0.0-alpha.6
 * @see Lifecycle
 * @see Phased
 */
public interface SmartLifecycle extends Lifecycle, Phased {
    
    /**
     * Default phase value for SmartLifecycle beans.
     */
    int DEFAULT_PHASE = 0;
    
    /**
     * Returns whether this Lifecycle component should be started
     * automatically by the container when the context is refreshed.
     *
     * <p>A value of {@code false} indicates that the component is
     * intended to be started manually.
     *
     * @return {@code true} if auto-startup is enabled (default)
     */
    default boolean isAutoStartup() {
        return true;
    }
    
    /**
     * Indicates the phase in which this lifecycle component should be
     * started and stopped.
     *
     * <p>During startup, components with lower phase values are started
     * first. During shutdown, components with higher phase values are
     * stopped first.
     *
     * @return the phase value (default is {@value #DEFAULT_PHASE})
     */
    @Override
    default int getPhase() {
        return DEFAULT_PHASE;
    }
    
    /**
     * Stop this component with a callback for asynchronous shutdown.
     *
     * <p>The default implementation delegates to {@link #stop()} and
     * then invokes the callback.
     *
     * @param callback the callback to invoke when shutdown is complete
     */
    default void stop(Runnable callback) {
        stop();
        callback.run();
    }
}
