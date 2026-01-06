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
package io.github.yasmramos.veld.runtime.lifecycle.event;

/**
 * Event published when the container has been stopped.
 *
 * <p>This event is raised when:
 * <ul>
 *   <li>All {@link io.github.yasmramos.veld.runtime.lifecycle.Lifecycle#stop()} methods called</li>
 *   <li>All {@code @OnStop} methods have been invoked</li>
 * </ul>
 *
 * <p>At this point, all lifecycle beans have been stopped but the container
 * is still available. The container can be restarted by calling {@code start()}.
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * @Singleton
 * public class ApplicationMonitor {
 *     
 *     @Subscribe
 *     public void onContextStopped(ContextStoppedEvent event) {
 *         System.out.println("Application stopped at: " + event.getTimestamp());
 *         System.out.println("Lifecycle beans stopped: " + event.getLifecycleBeanCount());
 *     }
 * }
 * }</pre>
 *
 * @author Veld Framework
 * @since 1.0.0-alpha.6
 * @see ContextStartedEvent
 * @see ContextClosedEvent
 */
public class ContextStoppedEvent extends ContextEvent {
    
    private final int lifecycleBeanCount;
    
    /**
     * Creates a new context stopped event.
     *
     * @param source the source of the event
     * @param lifecycleBeanCount number of lifecycle beans stopped
     */
    public ContextStoppedEvent(Object source, int lifecycleBeanCount) {
        super(source);
        this.lifecycleBeanCount = lifecycleBeanCount;
    }
    
    /**
     * Returns the number of lifecycle beans that were stopped.
     *
     * @return the lifecycle bean count
     */
    public int getLifecycleBeanCount() {
        return lifecycleBeanCount;
    }
    
    @Override
    public String toString() {
        return "ContextStoppedEvent{" +
                "lifecycleBeanCount=" + lifecycleBeanCount +
                ", timestamp=" + getTimestamp() +
                '}';
    }
}
