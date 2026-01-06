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

import io.github.yasmramos.veld.runtime.event.Event;

/**
 * Base class for all container context lifecycle events.
 *
 * <p>Context events are published to the EventBus during the container
 * lifecycle. Components can subscribe to these events using the
 * {@code @Subscribe} annotation.
 *
 * <p>Extends {@link Event} to integrate with the Veld EventBus system.
 *
 * <h2>Event Types</h2>
 * <ul>
 *   <li>{@link ContextRefreshedEvent} - Container initialization complete</li>
 *   <li>{@link ContextStartedEvent} - Container started</li>
 *   <li>{@link ContextStoppedEvent} - Container stopped</li>
 *   <li>{@link ContextClosedEvent} - Container closed</li>
 * </ul>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * @Singleton
 * public class LifecycleListener {
 *     
 *     @Subscribe
 *     public void onContextRefreshed(ContextRefreshedEvent event) {
 *         System.out.println("Container ready at: " + event.getTimestamp());
 *     }
 *     
 *     @Subscribe
 *     public void onContextClosed(ContextClosedEvent event) {
 *         System.out.println("Container closed, uptime: " + 
 *             Duration.between(startTime, event.getTimestamp()));
 *     }
 * }
 * }</pre>
 *
 * @author Veld Framework
 * @since 1.0.0-alpha.6
 * @see Event
 */
public abstract class ContextEvent extends Event {
    
    /**
     * Creates a new context event.
     *
     * @param source the source of the event (typically the container)
     */
    protected ContextEvent(Object source) {
        super(source);
    }
    
    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
                "source=" + (getSource() != null ? getSource().getClass().getSimpleName() : "null") +
                ", timestamp=" + getTimestamp() +
                '}';
    }
}
