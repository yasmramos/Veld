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

import io.github.yasmramos.veld.annotation.Subscribe;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Interface for the traditional object-based event bus system.
 *
 * <p>This interface defines the contract for publishing and subscribing to
 * typed Event objects. Each event is a concrete class extending {@link Event},
 * and subscribers register handler methods annotated with {@link Subscribe}.</p>
 *
 * <p><b>Characteristics:</b></p>
 * <ul>
 *   <li>Type-safe event dispatch based on Event class hierarchy</li>
 *   <li>Supports event filtering via method parameters</li>
 *   <li>Supports priority and async execution per subscriber</li>
 *   <li>Higher memory allocation due to Event object creation</li>
 *   <li>Use when type safety and semantic clarity are priorities</li>
 * </ul>
 *
 * <p><b>Example Usage:</b></p>
 * <pre>{@code
 * ObjectEventBus bus = EventBus.getInstance();
 *
 * // Define an event
 * class UserCreatedEvent extends Event {
 *     private final String userId;
 *     private final String email;
 *
 *     public UserCreatedEvent(String userId, String email) {
 *         this.userId = userId;
 *         this.email = email;
 *     }
 *
 *     public String getUserId() { return userId; }
 *     public String getEmail() { return email; }
 * }
 *
 * // Define a subscriber
 * class UserHandler {
 *     @Subscribe
 *     public void onUserCreated(UserCreatedEvent event) {
 *         System.out.println("User created: " + event.getUserId());
 *     }
 * }
 *
 * // Register and publish
 * bus.register(new UserHandler());
 * bus.publish(new UserCreatedEvent("123", "user@example.com"));
 * }</pre>
 *
 * @author Veld Framework Team
 * @since 1.0.0
 * @see Event
 * @see Subscribe
 * @see EventBus
 */
public interface ObjectEventBus {

    /**
     * Registers an object as an event subscriber.
     *
     * <p>All methods annotated with {@link Subscribe} in the given object
     * will be registered as event handlers.</p>
     *
     * @param subscriber the object containing event handler methods
     * @throws IllegalArgumentException if subscriber is null or has invalid handler methods
     */
    void register(Object subscriber);

    /**
     * Registers a specific event listener for a given event type.
     *
     * @param listener the event listener to register
     * @param eventType the type of event to listen for
     * @deprecated Use {@link ObjectLessEventBus#register(int, ObjectLessEventBus.ObjectLessListener)} instead
     */
    @Deprecated
    void register(EventBus.EventListener listener, Class<?> eventType);

    /**
     * Unregisters an object from receiving events.
     *
     * <p>All handler methods registered from this subscriber will be removed.</p>
     *
     * @param subscriber the subscriber to unregister
     */
    void unregister(Object subscriber);

    /**
     * Publishes an event to all matching subscribers synchronously.
     *
     * <p>The event will be delivered to all registered handlers whose
     * subscribed event type matches the runtime type of the event.</p>
     *
     * @param event the Event object to publish
     * @return the number of subscribers that received the event
     */
    int publish(Event event);

    /**
     * Publishes an event to all matching subscribers asynchronously.
     *
     * <p>The event will be published in a separate thread from the
     * internal executor pool.</p>
     *
     * @param event the Event object to publish
     * @return a CompletableFuture containing the delivery count
     */
    CompletableFuture<Integer> publishAsync(Event event);

    /**
     * Returns the number of registered subscribers.
     *
     * @return the count of registered subscribers
     */
    int getSubscriberCount();

    /**
     * Returns a list of all registered event types.
     *
     * @return list of event class types that have registered subscribers
     */
    List<Class<?>> getRegisteredEventTypes();

    /**
     * Clears all registered subscribers.
     */
    void clear();

    /**
     * Returns statistics about the object-based event system.
     *
     * @return a string containing statistics
     */
    String getStatistics();
}
