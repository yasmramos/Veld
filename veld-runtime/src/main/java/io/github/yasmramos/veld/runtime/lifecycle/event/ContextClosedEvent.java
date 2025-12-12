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

import java.time.Duration;

/**
 * Event published when the container is being closed.
 *
 * <p>This is the final lifecycle event, raised when:
 * <ul>
 *   <li>Context has been stopped</li>
 *   <li>{@code @PreDestroy} methods have been called</li>
 *   <li>{@link io.github.yasmramos.veld.runtime.lifecycle.DisposableBean#destroy()} methods called</li>
 * </ul>
 *
 * <p>After this event, the container is no longer usable. This is the
 * last chance to perform any cleanup or logging.
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
 *     }
 *     
 *     @Subscribe
 *     public void onContextClosed(ContextClosedEvent event) {
 *         Duration uptime = Duration.between(startTime, event.getTimestamp());
 *         System.out.println("Application closed after: " + uptime);
 *         System.out.println("Total uptime: " + event.getUptime());
 *     }
 * }
 * }</pre>
 *
 * @author Veld Framework
 * @since 1.0.0-alpha.6
 * @see ContextStoppedEvent
 */
public class ContextClosedEvent extends ContextEvent {
    
    private final Duration uptime;
    private final int destroyedBeanCount;
    
    /**
     * Creates a new context closed event.
     *
     * @param source the source of the event
     * @param uptime the total uptime of the container
     * @param destroyedBeanCount number of beans destroyed
     */
    public ContextClosedEvent(Object source, Duration uptime, int destroyedBeanCount) {
        super(source);
        this.uptime = uptime;
        this.destroyedBeanCount = destroyedBeanCount;
    }
    
    /**
     * Returns the total uptime of the container.
     *
     * @return the uptime duration
     */
    public Duration getUptime() {
        return uptime;
    }
    
    /**
     * Returns the number of beans that were destroyed.
     *
     * @return the destroyed bean count
     */
    public int getDestroyedBeanCount() {
        return destroyedBeanCount;
    }
    
    @Override
    public String toString() {
        return "ContextClosedEvent{" +
                "uptime=" + uptime +
                ", destroyedBeanCount=" + destroyedBeanCount +
                ", timestamp=" + getTimestamp() +
                '}';
    }
}
