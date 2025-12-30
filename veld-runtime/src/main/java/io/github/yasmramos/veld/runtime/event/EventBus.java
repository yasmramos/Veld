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

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Central event bus for publishing and subscribing to events.
 *
 * <p>The EventBus enables loose coupling between components by allowing
 * them to communicate through events without direct dependencies.
 *
 * <h2>Features</h2>
 * <ul>
 *   <li>Synchronous and asynchronous event delivery</li>
 *   <li>Priority-based subscriber ordering</li>
 *   <li>Filter expressions for conditional subscription</li>
 *   <li>Automatic discovery of @Subscribe methods</li>
 *   <li>Thread-safe event publication</li>
 * </ul>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // Get the EventBus instance
 * EventBus bus = EventBus.getInstance();
 *
 * // Register a subscriber object
 * bus.register(mySubscriber);
 *
 * // Publish an event
 * bus.publish(new OrderCreatedEvent(this, "ORD-123", 99.99));
 *
 * // Async publish
 * bus.publishAsync(new NotificationEvent("Hello!"));
 * }</pre>
 *
 * @author Veld Framework Team
 * @since 1.0.0
 * @see Event
 * @see Subscribe
 * @see EventSubscriber
 */
public class EventBus {

    private static final EventBus INSTANCE = new EventBus();

    private final Map<Class<?>, List<EventSubscriber>> subscribersByType;
    private ExecutorService asyncExecutor;
    private final AtomicLong publishedCount;
    private final AtomicLong deliveredCount;
    private volatile boolean shuttingDown;

    /**
     * Private constructor for singleton pattern.
     */
    private EventBus() {
        this.subscribersByType = new ConcurrentHashMap<>();
        this.asyncExecutor = Executors.newCachedThreadPool(r -> {
            Thread t = new Thread(r, "EventBus-Async-Worker");
            t.setDaemon(true);
            return t;
        });
        this.publishedCount = new AtomicLong(0);
        this.deliveredCount = new AtomicLong(0);
        this.shuttingDown = false;
    }

    /**
     * Returns the singleton EventBus instance.
     *
     * @return the EventBus instance
     */
    public static EventBus getInstance() {
        return INSTANCE;
    }

    /**
     * Registers an object as an event subscriber.
     *
     * <p>This method scans the object for methods annotated with @Subscribe
     * and registers them as event handlers.
     *
     * @param subscriber the subscriber object to register
     * @throws IllegalArgumentException if the subscriber is null
     */
    public void register(Object subscriber) {
        if (subscriber == null) {
            throw new IllegalArgumentException("Subscriber cannot be null");
        }

        Class<?> clazz = subscriber.getClass();
        int registeredCount = 0;

        for (Method method : clazz.getDeclaredMethods()) {
            Subscribe annotation = method.getAnnotation(Subscribe.class);
            if (annotation == null) {
                continue;
            }

            // Validate method signature
            Class<?>[] paramTypes = method.getParameterTypes();
            if (paramTypes.length != 1) {
                throw new IllegalArgumentException(
                        "Method " + method.getName() + " must have exactly one parameter");
            }

            Class<?> eventType = paramTypes[0];
            if (!Event.class.isAssignableFrom(eventType)) {
                throw new IllegalArgumentException(
                        "Parameter of " + method.getName() + " must extend Event");
            }

            // Create subscriber
            EventSubscriber eventSubscriber = new EventSubscriber(
                    subscriber,
                    method,
                    eventType,
                    annotation.async(),
                    annotation.priority(),
                    annotation.filter(),
                    annotation.catchExceptions()
            );

            // Add to subscribers list
            subscribersByType.computeIfAbsent(eventType, k -> new CopyOnWriteArrayList<>())
                    .add(eventSubscriber);

            registeredCount++;
            System.out.println("[EventBus] Registered: " + eventSubscriber);
        }

        // Sort subscribers by priority
        for (List<EventSubscriber> subscribers : subscribersByType.values()) {
            Collections.sort(subscribers);
        }

        if (registeredCount > 0) {
            System.out.println("[EventBus] Registered " + registeredCount + 
                    " handler(s) from " + clazz.getSimpleName());
        }
    }

    /**
     * Registers a specific event subscriber.
     *
     * @param subscriber the subscriber to register
     */
    public void register(EventSubscriber subscriber) {
        if (subscriber == null) {
            throw new IllegalArgumentException("Subscriber cannot be null");
        }

        List<EventSubscriber> subscribers = subscribersByType.computeIfAbsent(
                subscriber.getEventType(), k -> new CopyOnWriteArrayList<>());
        subscribers.add(subscriber);
        Collections.sort(subscribers);

        System.out.println("[EventBus] Registered: " + subscriber);
    }

    /**
     * Unregisters an object from receiving events.
     *
     * @param subscriber the subscriber object to unregister
     */
    public void unregister(Object subscriber) {
        if (subscriber == null) {
            return;
        }

        for (List<EventSubscriber> subscribers : subscribersByType.values()) {
            subscribers.removeIf(s -> s.getTarget() == subscriber);
        }

        System.out.println("[EventBus] Unregistered: " + subscriber.getClass().getSimpleName());
    }

    /**
     * Publishes an event to all matching subscribers synchronously.
     *
     * <p>Subscribers marked as async will still be invoked asynchronously,
     * but the method waits for synchronous subscribers to complete.
     *
     * @param event the event to publish
     * @return the number of subscribers that received the event
     */
    public int publish(Event event) {
        if (event == null || shuttingDown) {
            return 0;
        }

        publishedCount.incrementAndGet();
        int deliveryCount = 0;

        // Find all matching subscribers (including supertypes)
        List<EventSubscriber> matchingSubscribers = findSubscribers(event);

        for (EventSubscriber subscriber : matchingSubscribers) {
            // Check filter
            if (subscriber.hasFilter()) {
                if (!EventFilter.evaluate(subscriber.getFilter(), event)) {
                    continue;
                }
            }

            // Check if event was cancelled
            if (event.isCancelled()) {
                break;
            }

            // Deliver event
            if (subscriber.isAsync()) {
                deliverAsync(subscriber, event);
            } else {
                deliverSync(subscriber, event);
            }
            deliveryCount++;
        }

        deliveredCount.addAndGet(deliveryCount);
        return deliveryCount;
    }

    /**
     * Publishes an event asynchronously.
     *
     * <p>This method returns immediately and the event is delivered
     * in a background thread.
     *
     * @param event the event to publish
     * @return a CompletableFuture that completes when all subscribers have been notified
     */
    public CompletableFuture<Integer> publishAsync(Event event) {
        if (event == null || shuttingDown) {
            return CompletableFuture.completedFuture(0);
        }

        return CompletableFuture.supplyAsync(() -> publish(event), asyncExecutor);
    }

    /**
     * Finds all subscribers that can handle the given event.
     * Supports polymorphic event delivery - subscribers for parent classes
     * will receive events from child classes.
     */
    private List<EventSubscriber> findSubscribers(Event event) {
        List<EventSubscriber> result = new ArrayList<>();
        Class<?> eventClass = event.getClass();

        // Check all registered event types for polymorphic matching
        for (Map.Entry<Class<?>, List<EventSubscriber>> entry : subscribersByType.entrySet()) {
            Class<?> registeredType = entry.getKey();
            if (registeredType.isAssignableFrom(eventClass)) {
                result.addAll(entry.getValue());
            }
        }

        // Sort by priority
        Collections.sort(result);
        return result;
    }

    /**
     * Delivers an event to a subscriber synchronously.
     */
    private void deliverSync(EventSubscriber subscriber, Event event) {
        try {
            subscriber.invoke(event);
        } catch (Exception e) {
            handleException(subscriber, event, e);
        }
    }

    /**
     * Delivers an event to a subscriber asynchronously.
     */
    private void deliverAsync(EventSubscriber subscriber, Event event) {
        asyncExecutor.submit(() -> {
            try {
                subscriber.invoke(event);
            } catch (Exception e) {
                handleException(subscriber, event, e);
            }
        });
    }

    /**
     * Handles exceptions during event delivery.
     */
    private void handleException(EventSubscriber subscriber, Event event, Exception e) {
        if (subscriber.isCatchExceptions()) {
            System.err.println("[EventBus] Exception in subscriber " + 
                    subscriber.getMethod().getName() + ": " + e.getMessage());
            if (e.getCause() != null) {
                e.getCause().printStackTrace();
            }
        } else {
            if (e instanceof RuntimeException) {
                throw (RuntimeException) e;
            }
            throw new RuntimeException("Event handler exception", e);
        }
    }

    /**
     * Returns the total number of events published.
     *
     * @return the published event count
     */
    public long getPublishedCount() {
        return publishedCount.get();
    }

    /**
     * Returns the total number of event deliveries.
     *
     * @return the delivered event count
     */
    public long getDeliveredCount() {
        return deliveredCount.get();
    }

    /**
     * Returns the number of registered subscribers.
     *
     * @return the subscriber count
     */
    public int getSubscriberCount() {
        return subscribersByType.values().stream()
                .mapToInt(List::size)
                .sum();
    }

    /**
     * Returns a list of all registered event types.
     *
     * @return list of event type classes
     */
    public List<Class<?>> getRegisteredEventTypes() {
        return new ArrayList<>(subscribersByType.keySet());
    }

    /**
     * Clears all registered subscribers.
     */
    public void clear() {
        subscribersByType.clear();
        publishedCount.set(0);
        deliveredCount.set(0);
        System.out.println("[EventBus] Cleared all subscribers");
    }

    /**
     * Shuts down the EventBus executor.
     *
     * <p>After shutdown, async events will not be delivered.
     */
    public void shutdown() {
        shuttingDown = true;
        asyncExecutor.shutdown();
        try {
            if (!asyncExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                asyncExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            asyncExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        System.out.println("[EventBus] Shutdown complete");
    }

    /**
     * Resets the EventBus state for testing purposes.
     *
     * <p>This method clears all subscribers, resets counters, clears the
     * shutdown flag, and creates a new async executor. It should only be
     * used in tests to ensure a clean state between test runs.
     */
    void resetForTesting() {
        shuttingDown = false;
        // Shutdown old executor if it exists
        if (asyncExecutor != null && !asyncExecutor.isShutdown()) {
            asyncExecutor.shutdownNow();
        }
        // Create new executor
        asyncExecutor = Executors.newCachedThreadPool(r -> {
            Thread t = new Thread(r, "EventBus-Async-Worker");
            t.setDaemon(true);
            return t;
        });
        clear();
    }

    /**
     * Returns statistics about the EventBus.
     *
     * @return a string with EventBus statistics
     */
    public String getStatistics() {
        return String.format(
                "EventBus Statistics:\n" +
                "  - Registered Event Types: %d\n" +
                "  - Total Subscribers: %d\n" +
                "  - Events Published: %d\n" +
                "  - Events Delivered: %d",
                subscribersByType.size(),
                getSubscriberCount(),
                publishedCount.get(),
                deliveredCount.get()
        );
    }
}
