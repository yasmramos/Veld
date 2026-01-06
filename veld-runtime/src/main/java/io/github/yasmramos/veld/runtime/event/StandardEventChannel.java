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

import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicLong;

/**
 * High-performance event channel implementation with object-less event support.
 *
 * <p>This implementation eliminates object allocation overhead for high-frequency
 * events by using integer event IDs instead of Event objects. Subscribers receive
 * only the payload directly.</p>
 *
 * <p><b>Performance Optimizations:</b></p>
 * <ul>
 *   <li>No event object allocation on publish</li>
 *   <li>Cardinality-based dispatch (specialized paths for 0-4 listeners)</li>
 *   <li>Priority-ordered listener invocation</li>
 *   <li>Async listener support with dedicated executor</li>
 * </ul>
 *
 * @author Veld Framework Team
 * @since 1.0.0
 */
public class StandardEventChannel implements EventChannel {

    private final String channelName;
    private ExecutorService asyncExecutor;
    private final AtomicLong publishedCount;
    private final AtomicLong deliveredCount;

    // Optimized storage using int keys for direct lookup
    private volatile ListenerEntry[] listenersById;
    private int maxRegisteredId;
    private volatile boolean shuttingDown;

    private static final int INITIAL_CAPACITY = 64;
    private static final int MAX_CAPACITY = 65536;

    /**
     * Entry holding listener information with cardinality and priority metadata.
     */
    private static class ListenerEntry {
        ObjectLessEventBus.ObjectLessListener[] listeners;
        int count;

        ListenerEntry(ObjectLessEventBus.ObjectLessListener[] listeners) {
            this.listeners = listeners;
            this.count = listeners.length;
        }
    }

    /**
     * Creates a new StandardEventChannel.
     *
     * @param channelName the name for debugging purposes
     * @param asyncExecutor the executor for async listener invocation
     */
    public StandardEventChannel(String channelName, ExecutorService asyncExecutor) {
        this.channelName = channelName;
        this.asyncExecutor = asyncExecutor;
        this.listenersById = new ListenerEntry[INITIAL_CAPACITY];
        this.maxRegisteredId = -1;
        this.publishedCount = new AtomicLong(0);
        this.deliveredCount = new AtomicLong(0);
        this.shuttingDown = false;
    }

    @Override
    public int publish(int eventId, Object payload) {
        if (eventId < 0 || eventId >= listenersById.length) {
            return 0;
        }

        ListenerEntry entry = listenersById[eventId];
        if (entry == null || entry.count == 0) {
            return 0;
        }

        publishedCount.incrementAndGet();
        int delivered = dispatchOptimized(eventId, payload, entry.listeners, entry.count);
        deliveredCount.addAndGet(delivered);
        return delivered;
    }

    @Override
    public CompletableFuture<Integer> publishAsync(int eventId, Object payload) {
        if (eventId < 0 || eventId >= listenersById.length || shuttingDown) {
            return CompletableFuture.completedFuture(0);
        }

        return CompletableFuture.supplyAsync(
                () -> publish(eventId, payload),
                asyncExecutor
        );
    }

    @Override
    public void register(int eventId, ObjectLessEventBus.ObjectLessListener listener) {
        register(eventId, listener, 0);
    }

    @Override
    public void register(int eventId, ObjectLessEventBus.ObjectLessListener listener, int priority) {
        if (eventId < 0) {
            throw new IllegalArgumentException("Event ID cannot be negative");
        }
        if (listener == null) {
            throw new IllegalArgumentException("Listener cannot be null");
        }

        // Expand array if needed
        if (eventId >= listenersById.length) {
            int newCapacity = Math.min(eventId * 2, MAX_CAPACITY);
            if (eventId >= newCapacity) {
                throw new IllegalStateException("Event ID exceeds maximum capacity: " + eventId);
            }
            listenersById = Arrays.copyOf(listenersById, newCapacity);
        }

        ListenerEntry existing = listenersById[eventId];
        ObjectLessEventBus.ObjectLessListener[] newListeners;

        if (existing == null) {
            newListeners = new ObjectLessEventBus.ObjectLessListener[]{listener};
        } else {
            // Add with priority sorting
            newListeners = Arrays.copyOf(existing.listeners, existing.count + 1);
            int insertPos = existing.count;

            // Insert in priority order (higher priority first)
            for (int i = 0; i < existing.count; i++) {
                if (listener.getPriority() > existing.listeners[i].getPriority()) {
                    insertPos = i;
                    break;
                }
            }

            // Shift elements and insert
            System.arraycopy(newListeners, insertPos, newListeners, insertPos + 1,
                    existing.count - insertPos);
            newListeners[insertPos] = listener;
        }

        listenersById[eventId] = new ListenerEntry(newListeners);
        if (eventId > maxRegisteredId) {
            maxRegisteredId = eventId;
        }
    }

    @Override
    public void unregister(int eventId, ObjectLessEventBus.ObjectLessListener listener) {
        if (eventId < 0 || eventId >= listenersById.length) {
            return;
        }

        ListenerEntry existing = listenersById[eventId];
        if (existing == null) {
            return;
        }

        int removePos = -1;
        for (int i = 0; i < existing.count; i++) {
            if (existing.listeners[i] == listener) {
                removePos = i;
                break;
            }
        }

        if (removePos < 0) {
            return;
        }

        if (existing.count == 1) {
            listenersById[eventId] = null;
        } else {
            ObjectLessEventBus.ObjectLessListener[] newListeners = new ObjectLessEventBus.ObjectLessListener[existing.count - 1];
            System.arraycopy(existing.listeners, 0, newListeners, 0, removePos);
            System.arraycopy(existing.listeners, removePos + 1, newListeners, removePos,
                    existing.count - removePos - 1);
            listenersById[eventId] = new ListenerEntry(newListeners);
        }
    }

    @Override
    public void clear() {
        shuttingDown = false;
        Arrays.fill(listenersById, null);
        maxRegisteredId = -1;
    }

    /**
     * Updates the async executor reference. Used for testing to reset the EventBus.
     *
     * @param newExecutor the new executor to use
     */
    void updateExecutor(ExecutorService newExecutor) {
        this.asyncExecutor = newExecutor;
    }

    @Override
    public int getListenerCount() {
        int total = 0;
        for (int i = 0; i <= maxRegisteredId && i < listenersById.length; i++) {
            ListenerEntry entry = listenersById[i];
            if (entry != null) {
                total += entry.count;
            }
        }
        return total;
    }

    /**
     * Optimized dispatch based on listener count.
     * Uses specialized code paths for different cardinalities.
     */
    private int dispatchOptimized(int eventId, Object payload, ObjectLessEventBus.ObjectLessListener[] listeners, int count) {
        int deliveryCount = 0;

        switch (count) {
            case 0:
                break;

            case 1:
                deliveryCount = dispatchSingle(eventId, payload, listeners[0]);
                break;

            case 2:
                deliveryCount = dispatchTwo(eventId, payload, listeners[0], listeners[1]);
                break;

            case 3:
                deliveryCount = dispatchThree(eventId, payload, listeners[0], listeners[1], listeners[2]);
                break;

            case 4:
                deliveryCount = dispatchFour(eventId, payload, listeners[0], listeners[1], listeners[2], listeners[3]);
                break;

            default:
                deliveryCount = dispatchMultiple(eventId, payload, listeners, count);
                break;
        }

        return deliveryCount;
    }

    private int dispatchSingle(int eventId, Object payload, ObjectLessEventBus.ObjectLessListener listener) {
        if (listener.isAsync()) {
            asyncExecutor.submit(() -> listener.onEvent(payload));
            return 0;
        }
        listener.onEvent(payload);
        return 1;
    }

    private int dispatchTwo(int eventId, Object payload, ObjectLessEventBus.ObjectLessListener l1, ObjectLessEventBus.ObjectLessListener l2) {
        int count = 0;

        if (l1.isAsync()) {
            asyncExecutor.submit(() -> l1.onEvent(payload));
        } else {
            l1.onEvent(payload);
            count++;
        }

        if (l2.isAsync()) {
            asyncExecutor.submit(() -> l2.onEvent(payload));
        } else {
            l2.onEvent(payload);
            count++;
        }

        return count;
    }

    private int dispatchThree(int eventId, Object payload, ObjectLessEventBus.ObjectLessListener l1,
                              ObjectLessEventBus.ObjectLessListener l2, ObjectLessEventBus.ObjectLessListener l3) {
        int count = 0;

        if (l1.isAsync()) {
            asyncExecutor.submit(() -> l1.onEvent(payload));
        } else {
            l1.onEvent(payload);
            count++;
        }

        if (l2.isAsync()) {
            asyncExecutor.submit(() -> l2.onEvent(payload));
        } else {
            l2.onEvent(payload);
            count++;
        }

        if (l3.isAsync()) {
            asyncExecutor.submit(() -> l3.onEvent(payload));
        } else {
            l3.onEvent(payload);
            count++;
        }

        return count;
    }

    private int dispatchFour(int eventId, Object payload, ObjectLessEventBus.ObjectLessListener l1,
                             ObjectLessEventBus.ObjectLessListener l2, ObjectLessEventBus.ObjectLessListener l3, ObjectLessEventBus.ObjectLessListener l4) {
        int count = 0;

        if (l1.isAsync()) {
            asyncExecutor.submit(() -> l1.onEvent(payload));
        } else {
            l1.onEvent(payload);
            count++;
        }

        if (l2.isAsync()) {
            asyncExecutor.submit(() -> l2.onEvent(payload));
        } else {
            l2.onEvent(payload);
            count++;
        }

        if (l3.isAsync()) {
            asyncExecutor.submit(() -> l3.onEvent(payload));
        } else {
            l3.onEvent(payload);
            count++;
        }

        if (l4.isAsync()) {
            asyncExecutor.submit(() -> l4.onEvent(payload));
        } else {
            l4.onEvent(payload);
            count++;
        }

        return count;
    }

    private int dispatchMultiple(int eventId, Object payload, ObjectLessEventBus.ObjectLessListener[] listeners, int count) {
        int deliveryCount = 0;
        for (int i = 0; i < count; i++) {
            ObjectLessEventBus.ObjectLessListener listener = listeners[i];
            if (listener.isAsync()) {
                asyncExecutor.submit(() -> listener.onEvent(payload));
            } else {
                listener.onEvent(payload);
                deliveryCount++;
            }
        }
        return deliveryCount;
    }

    /**
     * Returns the channel name for debugging.
     */
    public String getChannelName() {
        return channelName;
    }

    /**
     * Returns statistics about this channel.
     */
    public String getStatistics() {
        return String.format(
                "%s Channel Statistics:\n" +
                "  - Total Listeners: %d\n" +
                "  - Events Published: %d\n" +
                "  - Events Delivered: %d",
                channelName,
                getListenerCount(),
                publishedCount.get(),
                deliveredCount.get()
        );
    }
}
