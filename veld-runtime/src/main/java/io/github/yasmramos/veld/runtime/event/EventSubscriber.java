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

import java.lang.reflect.Method;
import java.util.Objects;

/**
 * Represents a subscriber to events in the EventBus.
 *
 * <p>Each subscriber wraps a target object and a method that should
 * be invoked when matching events are published.
 *
 * @author Veld Framework Team
 * @since 1.0.0
 */
public class EventSubscriber implements Comparable<EventSubscriber> {

    private final Object target;
    private final Method method;
    private final Class<?> eventType;
    private final boolean async;
    private final int priority;
    private final String filter;
    private final boolean catchExceptions;

    /**
     * Creates a new event subscriber.
     *
     * @param target          the object containing the handler method
     * @param method          the method to invoke on events
     * @param eventType       the type of events this subscriber handles
     * @param async           whether to invoke asynchronously
     * @param priority        the subscriber priority
     * @param filter          the filter expression (empty for no filter)
     * @param catchExceptions whether to catch and log exceptions
     */
    public EventSubscriber(Object target, Method method, Class<?> eventType,
                           boolean async, int priority, String filter,
                           boolean catchExceptions) {
        this.target = Objects.requireNonNull(target, "target cannot be null");
        this.method = Objects.requireNonNull(method, "method cannot be null");
        this.eventType = Objects.requireNonNull(eventType, "eventType cannot be null");
        this.async = async;
        this.priority = priority;
        this.filter = filter != null ? filter : "";
        this.catchExceptions = catchExceptions;

        // Ensure method is accessible
        if (!method.canAccess(target)) {
            method.setAccessible(true);
        }
    }

    /**
     * Returns the target object containing the handler method.
     *
     * @return the target object
     */
    public Object getTarget() {
        return target;
    }

    /**
     * Returns the handler method.
     *
     * @return the method
     */
    public Method getMethod() {
        return method;
    }

    /**
     * Returns the event type this subscriber handles.
     *
     * @return the event type class
     */
    public Class<?> getEventType() {
        return eventType;
    }

    /**
     * Returns whether this subscriber should be invoked asynchronously.
     *
     * @return true for async invocation
     */
    public boolean isAsync() {
        return async;
    }

    /**
     * Returns the priority of this subscriber.
     *
     * @return the priority value
     */
    public int getPriority() {
        return priority;
    }

    /**
     * Returns the filter expression.
     *
     * @return the filter expression, or empty string if none
     */
    public String getFilter() {
        return filter;
    }

    /**
     * Returns whether this subscriber has a filter.
     *
     * @return true if a filter is defined
     */
    public boolean hasFilter() {
        return !filter.isEmpty();
    }

    /**
     * Returns whether exceptions should be caught.
     *
     * @return true if exceptions are caught and logged
     */
    public boolean isCatchExceptions() {
        return catchExceptions;
    }

    /**
     * Checks if this subscriber can handle the given event type.
     *
     * @param event the event to check
     * @return true if this subscriber can handle the event
     */
    public boolean canHandle(Event event) {
        return eventType.isAssignableFrom(event.getClass());
    }

    /**
     * Invokes the handler method with the given event.
     *
     * @param event the event to deliver
     * @throws Exception if the handler throws an exception
     */
    public void invoke(Event event) throws Exception {
        method.invoke(target, event);
    }

    @Override
    public int compareTo(EventSubscriber other) {
        // Higher priority first
        return Integer.compare(other.priority, this.priority);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EventSubscriber that = (EventSubscriber) o;
        return Objects.equals(target, that.target) &&
                Objects.equals(method, that.method);
    }

    @Override
    public int hashCode() {
        return Objects.hash(target, method);
    }

    @Override
    public String toString() {
        return String.format("EventSubscriber[%s.%s(%s), priority=%d, async=%s]",
                target.getClass().getSimpleName(),
                method.getName(),
                eventType.getSimpleName(),
                priority,
                async);
    }
}
