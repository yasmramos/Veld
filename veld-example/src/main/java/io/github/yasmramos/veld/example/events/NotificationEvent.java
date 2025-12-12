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
package io.github.yasmramos.veld.example.events;

import io.github.yasmramos.veld.runtime.event.Event;

/**
 * A simple notification event for demonstration purposes.
 *
 * <p>Can be used for general-purpose notifications within
 * the application.
 *
 * @author Veld Framework Team
 * @since 1.0.0-alpha.5
 */
public class NotificationEvent extends Event {

    /**
     * Notification priority levels.
     */
    public enum Priority {
        LOW, NORMAL, HIGH, URGENT
    }

    private final String message;
    private final Priority priority;
    private final String channel;

    /**
     * Creates a notification event with default priority.
     *
     * @param source  the source of the notification
     * @param message the notification message
     */
    public NotificationEvent(Object source, String message) {
        this(source, message, Priority.NORMAL, "default");
    }

    /**
     * Creates a notification event with specified priority.
     *
     * @param source   the source of the notification
     * @param message  the notification message
     * @param priority the priority level
     * @param channel  the notification channel
     */
    public NotificationEvent(Object source, String message, Priority priority, String channel) {
        super(source);
        this.message = message;
        this.priority = priority;
        this.channel = channel;
    }

    /**
     * Returns the notification message.
     *
     * @return the message
     */
    public String getMessage() {
        return message;
    }

    /**
     * Returns the notification priority.
     *
     * @return the priority
     */
    public Priority getPriority() {
        return priority;
    }

    /**
     * Returns the notification channel.
     *
     * @return the channel
     */
    public String getChannel() {
        return channel;
    }

    /**
     * Checks if this is a high priority notification.
     *
     * @return true if priority is HIGH or URGENT
     */
    public boolean isHighPriority() {
        return priority == Priority.HIGH || priority == Priority.URGENT;
    }

    @Override
    public String toString() {
        return String.format("NotificationEvent[message='%s', priority=%s, channel=%s]",
                message, priority, channel);
    }
}
