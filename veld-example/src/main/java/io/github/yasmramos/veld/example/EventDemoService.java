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
package io.github.yasmramos.veld.example;

import io.github.yasmramos.veld.annotation.Component;
import io.github.yasmramos.veld.example.events.NotificationEvent;
import io.github.yasmramos.veld.example.events.OrderCancelledEvent;
import io.github.yasmramos.veld.example.events.OrderCreatedEvent;
import io.github.yasmramos.veld.runtime.event.EventBus;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Service for demonstrating EventBus functionality.
 *
 * <p>Publishes events to communicate with other parts of 
 * the application without tight coupling.
 *
 * @author Veld Framework Team
 * @since 1.0.0
 */
@Component
public class EventDemoService {

    private final AtomicInteger orderCounter = new AtomicInteger(1000);

    /**
     * Creates a new order and publishes an OrderCreatedEvent.
     *
     * @param amount          the order amount
     * @param customerEmail   the customer's email
     * @param items           the ordered items
     * @param shippingAddress the shipping address
     * @return the created order ID
     */
    public String createOrder(double amount, String customerEmail,
                              List<String> items, String shippingAddress) {
        // Generate order ID
        String orderId = "ORD-" + orderCounter.incrementAndGet();

        System.out.println("  [EventDemoService] Creating order: " + orderId);

        // Publish order created event
        OrderCreatedEvent event = new OrderCreatedEvent(
                this,
                orderId,
                amount,
                customerEmail,
                items,
                shippingAddress
        );

        int subscribers = EventBus.getInstance().publish(event);
        System.out.println("  [EventDemoService] Event delivered to " + subscribers + " subscriber(s)");

        return orderId;
    }

    /**
     * Cancels an order and publishes an OrderCancelledEvent.
     *
     * @param orderId       the order ID to cancel
     * @param amount        the order amount (for refund)
     * @param customerEmail the customer's email
     * @param reason        the cancellation reason
     * @param issueRefund   whether to issue a refund
     */
    public void cancelOrder(String orderId, double amount, String customerEmail,
                            String reason, boolean issueRefund) {
        System.out.println("  [EventDemoService] Cancelling order: " + orderId);

        OrderCancelledEvent event = new OrderCancelledEvent(
                this,
                orderId,
                amount,
                customerEmail,
                reason,
                issueRefund
        );

        EventBus.getInstance().publish(event);
    }

    /**
     * Sends a notification through the event bus.
     *
     * @param message  the notification message
     * @param priority the notification priority
     * @param channel  the notification channel
     */
    public void sendNotification(String message, NotificationEvent.Priority priority, String channel) {
        NotificationEvent event = new NotificationEvent(this, message, priority, channel);
        EventBus.getInstance().publish(event);
    }
}
