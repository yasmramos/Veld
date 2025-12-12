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
 * Interface for beans that have a lifecycle with start/stop semantics.
 *
 * <p>Beans implementing this interface can be started and stopped by the
 * container. The container will call {@link #start()} when the context
 * starts and {@link #stop()} when the context stops.
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * @Singleton
 * public class MessageProcessor implements Lifecycle {
 *     
 *     private volatile boolean running = false;
 *     
 *     @Override
 *     public void start() {
 *         running = true;
 *         // Start processing messages
 *     }
 *     
 *     @Override
 *     public void stop() {
 *         running = false;
 *         // Stop processing messages
 *     }
 *     
 *     @Override
 *     public boolean isRunning() {
 *         return running;
 *     }
 * }
 * }</pre>
 *
 * @author Veld Framework
 * @since 1.0.0-alpha.6
 * @see SmartLifecycle
 */
public interface Lifecycle {
    
    /**
     * Start this component.
     *
     * <p>Should not throw an exception if the component is already running.
     * Called by the container when the context starts.
     */
    void start();
    
    /**
     * Stop this component.
     *
     * <p>Should not throw an exception if the component is not running.
     * Called by the container when the context stops.
     */
    void stop();
    
    /**
     * Check if this component is currently running.
     *
     * @return {@code true} if the component is running, {@code false} otherwise
     */
    boolean isRunning();
}
