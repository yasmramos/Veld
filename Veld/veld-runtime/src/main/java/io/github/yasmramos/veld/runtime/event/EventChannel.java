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
package io.github.yasmramos.veld.runtime.event;

import java.util.concurrent.CompletableFuture;

/**
 * Lightweight event channel interface for high-performance event dispatch.
 *
 * <p>This interface supports object-less events using integer event IDs,
 * eliminating the overhead of creating event objects for high-frequency events.</p>
 *
 * <p><b>Object-less Event Pattern:</b></p>
 * <pre>{@code
 * // Instead of: bus.publish(new HighFrequencyEvent(data));
 * // Use: bus.publish(EVENT_ID, payload);
 * }</pre>
 *
 * @author Veld Framework Team
 * @since 1.0.0
 * @see ObjectLessEventBus
 */
public interface EventChannel {

    /**
     * Publishes an event with the given ID and payload to all subscribers.
     *
     * <p>This is the object-less event pattern, eliminating the need to
     * create event objects for high-frequency events.</p>
     *
     * @param eventId the unique identifier for this event type
     * @param payload the data to publish with the event
     * @return the number of subscribers that received the event
     */
    int publish(int eventId, Object payload);

    /**
     * Publishes an event with the given ID and payload asynchronously.
     *
     * @param eventId the unique identifier for this event type
     * @param payload the data to publish with the event
     * @return a CompletableFuture containing the delivery count
     */
    CompletableFuture<Integer> publishAsync(int eventId, Object payload);

    /**
     * Registers a listener for object-less events.
     *
     * @param eventId the event ID to listen for
     * @param listener the listener to invoke when the event is published
     */
    void register(int eventId, ObjectLessEventBus.ObjectLessListener listener);

    /**
     * Registers a listener for object-less events with priority.
     *
     * @param eventId the event ID to listen for
     * @param listener the listener to invoke when the event is published
     * @param priority the priority of this listener (higher = called first)
     */
    void register(int eventId, ObjectLessEventBus.ObjectLessListener listener, int priority);

    /**
     * Unregisters a listener for object-less events.
     *
     * @param eventId the event ID to stop listening for
     * @param listener the listener to remove
     */
    void unregister(int eventId, ObjectLessEventBus.ObjectLessListener listener);

    /**
     * Unregisters all listeners from this channel.
     */
    void clear();

    /**
     * Returns the number of registered listeners.
     *
     * @return the listener count
     */
    int getListenerCount();
}
