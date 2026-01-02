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
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

/**
 * Central event bus for publishing and subscribing to events.
 *
 * <p>This implementation provides two distinct event systems:</p>
 * <ul>
 *   <li><b>Object-Based Events ({@link ObjectEventBus}):</b> Traditional system using
 *       typed {@link Event} objects with full type safety and semantic clarity.</li>
 *   <li><b>Object-Less Events ({@link ObjectLessEventBus}):</b> High-performance system
 *       using integer event IDs with zero object allocation overhead.</li>
 * </ul>
 *
 * <p><b>Optimizations:</b></p>
 * <ul>
 *   <li>MethodHandle-based invocation for object-based events (5-6x faster than reflection)</li>
 *   <li>Specialized dispatch based on listener cardinality</li>
 *   <li>Fast-path for common cases (0, 1, 2-4 listeners)</li>
 *   <li>Array-based storage for small listener sets</li>
 *   <li>StandardEventChannel for zero-allocation object-less events</li>
 * </ul>
 *
 * <p><b>Usage Recommendations:</b></p>
 * <ul>
 *   <li>Use <b>Object-Based</b> for domain events, state changes, and when type safety is priority</li>
 *   <li>Use <b>Object-Less</b> for high-frequency events, metrics, telemetry, and performance-critical paths</li>
 * </ul>
 *
 * @author Veld Framework Team
 * @since 1.0.0
 * @see ObjectEventBus
 * @see ObjectLessEventBus
 * @see Event
 * @see Subscribe
 * @see EventSubscriber
 * @see EventChannel
 */
public class EventBus implements ObjectEventBus, ObjectLessEventBus {

    private static final EventBus INSTANCE = new EventBus();

    // Optimized subscriber storage for object-based events
    private final SubscriberIndex subscriberIndex;

    // StandardEventChannel for object-less events (zero allocation)
    private final StandardEventChannel standardChannel;

    // Specialized channels
    private final Map<String, EventChannel> specializedChannels;

    private ExecutorService asyncExecutor;
    private final AtomicLong publishedCount;
    private final AtomicLong deliveredCount;
    private volatile boolean shuttingDown;

    /**
     * Lightweight interface for fast event dispatch.
     *
     * @deprecated Use {@link ObjectLessEventBus.ObjectLessListener} for new implementations
     */
    @Deprecated
    public interface EventListener {
        void onEvent(Event event);
        default boolean isAsync() { return false; }
        default int getPriority() { return 0; }
    }

    /**
     * Optimized subscriber index with cardinality-based dispatch.
     *
     * <p>This class uses specialized arrays for different listener counts:
     * <ul>
     *   <li>0 listeners: Empty array (fastest)</li>
     *   <li>1 listener: Single-element array</li>
     *   <li>2-4 listeners: Small fixed-size arrays</li>
     *   <li>5+ listeners: CopyOnWriteArrayList</li>
     * </ul>
     */
    private class SubscriberIndex {
        // Fast path: direct listeners storage with cardinality optimization
        private final Map<Class<?>, ListenerEntry> listenersByType = new ConcurrentHashMap<>();

        /**
         * Entry holding listener information with cardinality metadata.
         */
        private static class ListenerEntry {
            final EventListener[] listeners;
            final int count;

            ListenerEntry(EventListener[] listeners) {
                this.listeners = listeners;
                this.count = listeners.length;
            }
        }

        void register(EventSubscriber subscriber) {
            registerListener(subscriber.getEventType(), new SubscriberEventListener(subscriber));
        }

        void registerListener(Class<?> eventType, EventListener listener) {
            listenersByType.compute(eventType, (key, existing) -> {
                if (existing == null) {
                    return new ListenerEntry(new EventListener[]{listener});
                }

                // Insert listener with priority sorting (higher priority first)
                EventListener[] newListeners = new EventListener[existing.count + 1];
                int insertPos = 0;

                // Find insertion position based on priority
                for (int i = 0; i < existing.count; i++) {
                    if (listener.getPriority() > existing.listeners[i].getPriority()) {
                        insertPos = i;
                        break;
                    }
                    insertPos = i + 1;
                }

                // Copy listeners before insertion point
                System.arraycopy(existing.listeners, 0, newListeners, 0, insertPos);
                // Insert new listener
                newListeners[insertPos] = listener;
                // Copy listeners after insertion point
                System.arraycopy(existing.listeners, insertPos, newListeners, insertPos + 1,
                        existing.count - insertPos);

                return new ListenerEntry(newListeners);
            });
        }

        @SuppressWarnings("unchecked")
        int publish(Event event) {
            Class<?> eventClass = event.getClass();

            // Check for listeners registered for this event class or any parent class
            // This supports event inheritance - child events are delivered to parent subscribers
            Class<?> current = eventClass;
            while (current != null && current != Event.class) {
                ListenerEntry entry = listenersByType.get(current);
                if (entry != null && entry.count > 0) {
                    return dispatchOptimized(event, entry.listeners, entry.count);
                }
                current = current.getSuperclass();
            }

            // No listeners found for this event or any parent class
            return 0;
        }

        /**
         * Optimized dispatch based on listener count.
         * Uses specialized code paths for different cardinalities.
         */
        private int dispatchOptimized(Event event, EventListener[] listeners, int count) {
            int deliveryCount = 0;

            // Specialized dispatch based on cardinality
            switch (count) {
                case 0:
                    // No listeners - fastest path
                    break;

                case 1:
                    // Single listener - no loop overhead
                    deliveryCount = dispatchSingle(event, listeners[0]);
                    break;

                case 2:
                    // Two listeners - unrolled
                    deliveryCount = dispatchTwo(event, listeners[0], listeners[1]);
                    break;

                case 3:
                    // Three listeners - unrolled
                    deliveryCount = dispatchThree(event, listeners[0], listeners[1], listeners[2]);
                    break;

                case 4:
                    // Four listeners - unrolled
                    deliveryCount = dispatchFour(event, listeners[0], listeners[1], listeners[2], listeners[3]);
                    break;

                default:
                    // Five or more listeners - loop
                    deliveryCount = dispatchMultiple(event, listeners, count);
                    break;
            }

            return deliveryCount;
        }

        private int dispatchSingle(Event event, EventListener listener) {
            if (listener.isAsync()) {
                asyncExecutor.submit(() -> listener.onEvent(event));
                return 1;
            }
            listener.onEvent(event);
            return 1;
        }

        private int dispatchTwo(Event event, EventListener l1, EventListener l2) {
            int count = 0;

            if (l1.isAsync()) {
                asyncExecutor.submit(() -> l1.onEvent(event));
                count++;
            } else {
                l1.onEvent(event);
                count++;
            }

            // Check if event was cancelled after first listener
            if (event.isCancelled()) {
                return count;
            }

            if (l2.isAsync()) {
                asyncExecutor.submit(() -> l2.onEvent(event));
                count++;
            } else {
                l2.onEvent(event);
                count++;
            }

            return count;
        }

        private int dispatchThree(Event event, EventListener l1, EventListener l2, EventListener l3) {
            int count = 0;

            if (l1.isAsync()) {
                asyncExecutor.submit(() -> l1.onEvent(event));
                count++;
            } else {
                l1.onEvent(event);
                count++;
            }

            // Check if event was cancelled
            if (event.isCancelled()) {
                return count;
            }

            if (l2.isAsync()) {
                asyncExecutor.submit(() -> l2.onEvent(event));
                count++;
            } else {
                l2.onEvent(event);
                count++;
            }

            // Check if event was cancelled
            if (event.isCancelled()) {
                return count;
            }

            if (l3.isAsync()) {
                asyncExecutor.submit(() -> l3.onEvent(event));
                count++;
            } else {
                l3.onEvent(event);
                count++;
            }

            return count;
        }

        private int dispatchFour(Event event, EventListener l1, EventListener l2,
                                  EventListener l3, EventListener l4) {
            int count = 0;

            if (l1.isAsync()) {
                asyncExecutor.submit(() -> l1.onEvent(event));
                count++;
            } else {
                l1.onEvent(event);
                count++;
            }

            // Check if event was cancelled
            if (event.isCancelled()) {
                return count;
            }

            if (l2.isAsync()) {
                asyncExecutor.submit(() -> l2.onEvent(event));
                count++;
            } else {
                l2.onEvent(event);
                count++;
            }

            // Check if event was cancelled
            if (event.isCancelled()) {
                return count;
            }

            if (l3.isAsync()) {
                asyncExecutor.submit(() -> l3.onEvent(event));
                count++;
            } else {
                l3.onEvent(event);
                count++;
            }

            // Check if event was cancelled
            if (event.isCancelled()) {
                return count;
            }

            if (l4.isAsync()) {
                asyncExecutor.submit(() -> l4.onEvent(event));
                count++;
            } else {
                l4.onEvent(event);
                count++;
            }

            return count;
        }

        private int dispatchMultiple(Event event, EventListener[] listeners, int count) {
            int deliveryCount = 0;
            for (int i = 0; i < count; i++) {
                // Check for cancellation before each listener (except the first)
                if (i > 0 && event.isCancelled()) {
                    break;
                }

                EventListener listener = listeners[i];
                if (listener.isAsync()) {
                    asyncExecutor.submit(() -> listener.onEvent(event));
                    deliveryCount++;
                } else {
                    listener.onEvent(event);
                    deliveryCount++;
                }
            }
            return deliveryCount;
        }

        void unregister(Object subscriber) {
            listenersByType.forEach((type, entry) -> {
                List<EventListener> toRemove = new ArrayList<>();
                for (EventListener listener : entry.listeners) {
                    if (listener instanceof SubscriberEventListener) {
                        Object target = ((SubscriberEventListener) listener).getTarget();
                        if (target == subscriber) {
                            toRemove.add(listener);
                        }
                    }
                }
                if (!toRemove.isEmpty()) {
                    EventListener[] newListeners = Arrays.stream(entry.listeners)
                            .filter(l -> !toRemove.contains(l))
                            .toArray(EventListener[]::new);
                    listenersByType.put(type, new ListenerEntry(newListeners));
                }
            });
        }

        void clear() {
            listenersByType.clear();
        }

        int getSubscriberCount() {
            return listenersByType.values().stream().mapToInt(e -> e.count).sum();
        }
    }

    /**
     * Wrapper to use EventSubscriber as EventListener with MethodHandle invocation.
     */
    private static class SubscriberEventListener implements EventListener {
        private final EventSubscriber subscriber;

        SubscriberEventListener(EventSubscriber subscriber) {
            this.subscriber = subscriber;
        }

        @Override
        public void onEvent(Event event) {
            try {
                subscriber.invoke(event);
            } catch (Throwable e) {
                if (subscriber.isCatchExceptions()) {
                    // Log the exception but don't propagate
                    System.err.println("[EventBus] Exception in subscriber: " + e.getMessage());
                } else {
                    throw new RuntimeException(e);
                }
            }
        }

        @Override
        public boolean isAsync() {
            return subscriber.isAsync();
        }

        @Override
        public int getPriority() {
            return subscriber.getPriority();
        }

        Object getTarget() {
            return subscriber.getTarget();
        }
    }

    /**
     * Private constructor for singleton pattern.
     */
    private EventBus() {
        this.subscriberIndex = new SubscriberIndex();
        this.asyncExecutor = createAsyncExecutor();
        this.publishedCount = new AtomicLong(0);
        this.deliveredCount = new AtomicLong(0);
        this.shuttingDown = false;

        // Initialize standard channel for object-less events
        this.standardChannel = new StandardEventChannel("Standard", asyncExecutor);

        // Initialize specialized channels map
        this.specializedChannels = new ConcurrentHashMap<>();
    }

    private ExecutorService createAsyncExecutor() {
        return Executors.newCachedThreadPool(r -> {
            Thread t = new Thread(r, "EventBus-Async-Worker");
            t.setDaemon(true);
            return t;
        });
    }

    /**
     * Returns the singleton EventBus instance.
     */
    public static EventBus getInstance() {
        return INSTANCE;
    }

    // ==================== ObjectLessEventBus Methods ====================

    /**
     * Publishes an object-less event to the standard channel.
     *
     * <p>This is the high-performance path for events that don't require
     * an Event object. Eliminates object allocation overhead entirely.</p>
     *
     * <p><b>Example:</b></p>
     * <pre>{@code
     * // Instead of: bus.publish(new HighFrequencyEvent(data));
     * // Use:
     * bus.publish(1001, data);
     * }</pre>
     *
     * @param eventId the unique identifier for this event type
     * @param payload the data to publish with the event
     * @return the number of subscribers that received the event
     */
    @Override
    public int publish(int eventId, Object payload) {
        if (shuttingDown) {
            return 0;
        }
        return standardChannel.publish(eventId, payload);
    }

    /**
     * Publishes an object-less event asynchronously.
     *
     * @param eventId the unique identifier for this event type
     * @param payload the data to publish with the event
     * @return a CompletableFuture containing the delivery count
     */
    @Override
    public CompletableFuture<Integer> publishAsync(int eventId, Object payload) {
        if (shuttingDown) {
            return CompletableFuture.completedFuture(0);
        }
        return standardChannel.publishAsync(eventId, payload);
    }

    /**
     * Registers a listener for object-less events.
     *
     * @param eventId the event ID to listen for
     * @param listener the listener to invoke when the event is published
     */
    @Override
    public void register(int eventId, ObjectLessListener listener) {
        standardChannel.register(eventId, listener);
    }

    /**
     * Registers a listener for object-less events with priority.
     *
     * @param eventId the event ID to listen for
     * @param listener the listener to invoke when the event is published
     * @param priority the priority of this listener (higher = called first)
     */
    @Override
    public void register(int eventId, ObjectLessListener listener, int priority) {
        standardChannel.register(eventId, listener, priority);
    }

    /**
     * Unregisters a listener for object-less events.
     *
     * @param eventId the event ID to stop listening for
     * @param listener the listener to remove
     */
    @Override
    public void unregister(int eventId, ObjectLessListener listener) {
        standardChannel.unregister(eventId, listener);
    }

    /**
     * Returns the total number of registered listeners.
     *
     * @return the count of all registered listeners across all channels
     */
    @Override
    public int getListenerCount() {
        int count = standardChannel.getListenerCount();
        for (EventChannel channel : specializedChannels.values()) {
            count += channel.getListenerCount();
        }
        return count;
    }

    // ==================== Specialized Channels ====================

    /**
     * Gets or creates a specialized channel for the given purpose.
     *
     * <p>Specialized channels can be used for different event domains
     * (e.g., lifecycle, metrics, tracing) to provide isolation and
     * dedicated optimization.</p>
     *
     * @param channelName the name of the specialized channel
     * @return the specialized EventChannel
     */
    @Override
    public EventChannel getChannel(String channelName) {
        return specializedChannels.computeIfAbsent(channelName,
                name -> new StandardEventChannel(name, asyncExecutor));
    }

    /**
     * Gets the standard channel for object-less events.
     *
     * @return the standard EventChannel
     */
    @Override
    public EventChannel getStandardChannel() {
        return standardChannel;
    }

    // ==================== Zero-Reflection Registration Methods ====================

    /**
     * Functional interface for typed event handlers in zero-reflection mode.
     *
     * @param <T> the event type
     */
    @FunctionalInterface
    public interface TypedEventHandler<T extends Event> {
        void handle(T event);
    }

    /**
     * Registers a typed event handler for zero-reflection operation.
     *
     * <p>This method is intended for use by generated code that creates type-safe
     * event subscriptions without using reflection.</p>
     *
     * <h2>Generated Code Pattern</h2>
     * <pre>{@code
     * bus.registerEventHandler(SimpleEvent.ID, SimpleEvent.class,
     *     (SimpleEvent event) -> typed.onSimpleEvent(event));
     * }</pre>
     *
     * @param <T> the event type
     * @param eventId the event type ID (e.g., hash of Event class name)
     * @param eventClass the event class for type-safe casting
     * @param handler the typed event handler
     */
    public <T extends Event> void registerEventHandler(int eventId, Class<T> eventClass,
                                                        TypedEventHandler<T> handler) {
        if (handler == null) {
            throw new IllegalArgumentException("Handler cannot be null");
        }
        standardChannel.register(eventId, (payload) -> {
            if (eventClass.isInstance(payload)) {
                handler.handle(eventClass.cast(payload));
            }
        });
    }

    /**
     * Registers a typed event handler with priority.
     *
     * @param <T> the event type
     * @param eventId the event type ID
     * @param eventClass the event class for type-safe casting
     * @param handler the typed event handler
     * @param priority the priority (higher = called first)
     */
    public <T extends Event> void registerEventHandler(int eventId, Class<T> eventClass,
                                                        TypedEventHandler<T> handler, int priority) {
        if (handler == null) {
            throw new IllegalArgumentException("Handler cannot be null");
        }
        standardChannel.register(eventId, (payload) -> {
            if (eventClass.isInstance(payload)) {
                handler.handle(eventClass.cast(payload));
            }
        }, priority);
    }

    /**
     * Registers an event handler using a lambda/Consumer for zero-reflection operation.
     *
     * @deprecated Use {@link #registerEventHandler(int, Class, TypedEventHandler)} instead
     * @param eventId the event type ID
     * @param handler the event handler as a Consumer
     */
    @Deprecated
    public void registerEventHandler(int eventId, java.util.function.Consumer<? super Event> handler) {
        if (handler == null) {
            throw new IllegalArgumentException("Handler cannot be null");
        }
        standardChannel.register(eventId, (payload) -> {
            Event event = payload instanceof Event ? (Event) payload : new GenericEvent(payload);
            handler.accept(event);
        });
    }

    /**
     * Registers an event handler with priority using a lambda/Consumer.
     *
     * @deprecated Use {@link #registerEventHandler(int, Class, TypedEventHandler, int)} instead
     * @param eventId the event type ID
     * @param handler the event handler as a Consumer
     * @param priority the priority (higher = called first)
     */
    @Deprecated
    public void registerEventHandler(int eventId, java.util.function.Consumer<? super Event> handler, int priority) {
        if (handler == null) {
            throw new IllegalArgumentException("Handler cannot be null");
        }
        standardChannel.register(eventId, (payload) -> {
            Event event = payload instanceof Event ? (Event) payload : new GenericEvent(payload);
            handler.accept(event);
        }, priority);
    }

    /**
     * Simple generic event wrapper for payload events.
     */
    private static class GenericEvent extends Event {
        private final Object payload;

        GenericEvent(Object payload) {
            this.payload = payload;
        }

        public Object getPayload() {
            return payload;
        }

        @Override
        public String toString() {
            return "GenericEvent{payload=" + payload + "}";
        }
    }

    // ==================== ObjectEventBus Methods (Zero-Reflection) ====================

    /**
     * Registers an object as an event subscriber using zero-reflection API.
     *
     * @deprecated Use {@link #registerEventHandler(int, Class, TypedEventHandler)} instead.
     *             This method requires reflection and is not compatible with GraalVM native-image.
     *             Please migrate to the zero-reflection API using registerEventHandler().
     */
    @Deprecated
    @Override
    public void register(Object subscriber) {
        throw new UnsupportedOperationException(
                "Object registration with @Subscribe is not supported in zero-reflection mode. " +
                "Please use registerEventHandler(int eventId, Class<T> eventClass, TypedEventHandler<T> handler) instead.");
    }

    /**
     * Registers an EventSubscriber directly.
     *
     * @param subscriber the EventSubscriber to register
     */
    public void register(EventSubscriber subscriber) {
        if (subscriber == null) {
            throw new IllegalArgumentException("EventSubscriber cannot be null");
        }
        subscriberIndex.register(subscriber);
        System.out.println("[EventBus] Registered EventSubscriber: " +
                subscriber.getEventType().getSimpleName());
    }

    /**
     * Registers a specific event listener.
     *
     * @deprecated Use {@link #register(int, ObjectLessListener)} for new implementations
     */
    @Deprecated
    @Override
    public void register(EventListener listener, Class<?> eventType) {
        subscriberIndex.registerListener(eventType, listener);
    }

    /**
     * Unregisters an object from receiving events.
     */
    @Override
    public void unregister(Object subscriber) {
        if (subscriber == null) {
            return;
        }
        subscriberIndex.unregister(subscriber);
        System.out.println("[EventBus] Unregistered: " + subscriber.getClass().getSimpleName());
    }

    /**
     * Publishes an event to all matching subscribers synchronously.
     *
     * @param event the Event object to publish
     * @return the number of subscribers that received the event
     */
    @Override
    public int publish(Event event) {
        if (event == null || shuttingDown) {
            return 0;
        }

        publishedCount.incrementAndGet();
        int delivered = subscriberIndex.publish(event);
        deliveredCount.addAndGet(delivered);
        return delivered;
    }

    /**
     * Publishes an event asynchronously.
     *
     * @param event the Event object to publish
     * @return a CompletableFuture containing the delivery count
     */
    @Override
    public CompletableFuture<Integer> publishAsync(Event event) {
        if (event == null || shuttingDown) {
            return CompletableFuture.completedFuture(0);
        }

        return CompletableFuture.supplyAsync(() -> publish(event), asyncExecutor);
    }

    // ==================== Statistics and Management ====================

    /**
     * Returns the total number of events published.
     */
    public long getPublishedCount() {
        return publishedCount.get();
    }

    /**
     * Returns the total number of event deliveries.
     */
    public long getDeliveredCount() {
        return deliveredCount.get();
    }

    /**
     * Returns the number of registered subscribers (object-based).
     */
    @Override
    public int getSubscriberCount() {
        return subscriberIndex.getSubscriberCount();
    }

    /**
     * Returns the total number of registered listeners across all channels.
     */
    public int getTotalListenerCount() {
        int count = subscriberIndex.getSubscriberCount();
        count += standardChannel.getListenerCount();
        for (EventChannel channel : specializedChannels.values()) {
            count += channel.getListenerCount();
        }
        return count;
    }

    /**
     * Clears all registered subscribers and listeners.
     */
    @Override
    public void clear() {
        shuttingDown = false;
        subscriberIndex.clear();
        standardChannel.clear();
        for (EventChannel channel : specializedChannels.values()) {
            channel.clear();
        }
        publishedCount.set(0);
        deliveredCount.set(0);
        System.out.println("[EventBus] Cleared all subscribers and listeners");
    }

    /**
     * Shuts down the EventBus executor.
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
     */
    void resetForTesting() {
        shuttingDown = false;
        if (asyncExecutor != null && !asyncExecutor.isShutdown()) {
            asyncExecutor.shutdownNow();
        }
        asyncExecutor = createAsyncExecutor();

        // Update executor reference in standard channel
        standardChannel.updateExecutor(asyncExecutor);

        // Update executor reference in specialized channels
        for (EventChannel channel : specializedChannels.values()) {
            if (channel instanceof StandardEventChannel) {
                ((StandardEventChannel) channel).updateExecutor(asyncExecutor);
            }
        }

        clear();
    }

    /**
     * Returns a list of all registered event types.
     */
    @Override
    public List<Class<?>> getRegisteredEventTypes() {
        return new ArrayList<>(subscriberIndex.listenersByType.keySet());
    }

    /**
     * Returns statistics about the EventBus.
     */
    @Override
    public String getStatistics() {
        return String.format(
                "EventBus Statistics:\n" +
                "  - Object-Based Subscribers: %d\n" +
                "  - Object-Less Listeners: %d\n" +
                "  - Specialized Channels: %d\n" +
                "  - Total Events Published: %d\n" +
                "  - Total Events Delivered: %d",
                getSubscriberCount(),
                standardChannel.getListenerCount(),
                specializedChannels.size(),
                publishedCount.get(),
                deliveredCount.get()
        );
    }
}
