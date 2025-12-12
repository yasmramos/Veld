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

import java.time.Instant;
import java.util.UUID;

/**
 * Base class for all events in the Veld EventBus system.
 *
 * <p>All custom events should extend this class to be publishable
 * through the EventBus. The base class provides common metadata
 * such as event ID, timestamp, and source information.
 *
 * <h2>Creating Custom Events</h2>
 *
 * <pre>{@code
 * public class OrderCreatedEvent extends Event {
 *     private final String orderId;
 *     private final double amount;
 *
 *     public OrderCreatedEvent(Object source, String orderId, double amount) {
 *         super(source);
 *         this.orderId = orderId;
 *         this.amount = amount;
 *     }
 *
 *     public String getOrderId() { return orderId; }
 *     public double getAmount() { return amount; }
 * }
 * }</pre>
 *
 * @author Veld Framework Team
 * @since 1.0.0-alpha.5
 * @see EventBus
 * @see io.github.yasmramos.veld.annotation.Subscribe
 */
public abstract class Event {

    private final String eventId;
    private final Instant timestamp;
    private final Object source;
    private boolean cancelled;
    private boolean consumed;

    /**
     * Creates a new event with the specified source.
     *
     * @param source the object that originated this event (can be null)
     */
    protected Event(Object source) {
        this.eventId = UUID.randomUUID().toString();
        this.timestamp = Instant.now();
        this.source = source;
        this.cancelled = false;
        this.consumed = false;
    }

    /**
     * Creates a new event without a specific source.
     */
    protected Event() {
        this(null);
    }

    /**
     * Returns the unique identifier of this event.
     *
     * @return the event ID
     */
    public String getEventId() {
        return eventId;
    }

    /**
     * Returns the timestamp when this event was created.
     *
     * @return the creation timestamp
     */
    public Instant getTimestamp() {
        return timestamp;
    }

    /**
     * Returns the source object that originated this event.
     *
     * @return the source object, or null if not specified
     */
    public Object getSource() {
        return source;
    }

    /**
     * Returns whether this event has been cancelled.
     *
     * <p>Cancelled events may be skipped by some subscribers
     * depending on their configuration.
     *
     * @return true if cancelled, false otherwise
     */
    public boolean isCancelled() {
        return cancelled;
    }

    /**
     * Cancels this event, potentially preventing further processing.
     */
    public void cancel() {
        this.cancelled = true;
    }

    /**
     * Returns whether this event has been consumed.
     *
     * <p>Consumed events indicate that a subscriber has fully
     * handled the event and no further processing is needed.
     *
     * @return true if consumed, false otherwise
     */
    public boolean isConsumed() {
        return consumed;
    }

    /**
     * Marks this event as consumed.
     */
    public void consume() {
        this.consumed = true;
    }

    /**
     * Returns the simple name of this event's class.
     *
     * @return the event type name
     */
    public String getEventType() {
        return getClass().getSimpleName();
    }

    @Override
    public String toString() {
        return String.format("%s[id=%s, timestamp=%s, source=%s]",
                getEventType(), eventId, timestamp,
                source != null ? source.getClass().getSimpleName() : "null");
    }
}
