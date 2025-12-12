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
 * Event published when the container has been started.
 *
 * <p>This event is raised after:
 * <ul>
 *   <li>{@link ContextRefreshedEvent} has been published</li>
 *   <li>All {@link io.github.yasmramos.veld.runtime.lifecycle.Lifecycle#start()} methods called</li>
 *   <li>All {@code @OnStart} methods have been invoked</li>
 * </ul>
 *
 * <p>At this point, all lifecycle beans are running and the application
 * is fully operational.
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * @Singleton
 * public class ApplicationMonitor {
 *     
 *     private Instant startTime;
 *     
 *     @Subscribe
 *     public void onContextStarted(ContextStartedEvent event) {
 *         startTime = event.getTimestamp();
 *         System.out.println("Application started at: " + startTime);
 *         System.out.println("Lifecycle beans started: " + event.getLifecycleBeanCount());
 *     }
 * }
 * }</pre>
 *
 * @author Veld Framework
 * @since 1.0.0-alpha.6
 * @see ContextRefreshedEvent
 * @see ContextStoppedEvent
 */
public class ContextStartedEvent extends ContextEvent {
    
    private final int lifecycleBeanCount;
    
    /**
     * Creates a new context started event.
     *
     * @param source the source of the event
     * @param lifecycleBeanCount number of lifecycle beans started
     */
    public ContextStartedEvent(Object source, int lifecycleBeanCount) {
        super(source);
        this.lifecycleBeanCount = lifecycleBeanCount;
    }
    
    /**
     * Returns the number of lifecycle beans that were started.
     *
     * @return the lifecycle bean count
     */
    public int getLifecycleBeanCount() {
        return lifecycleBeanCount;
    }
    
    @Override
    public String toString() {
        return "ContextStartedEvent{" +
                "lifecycleBeanCount=" + lifecycleBeanCount +
                ", timestamp=" + getTimestamp() +
                '}';
    }
}
