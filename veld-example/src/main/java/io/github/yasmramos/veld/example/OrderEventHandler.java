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
import io.github.yasmramos.veld.annotation.Subscribe;
import io.github.yasmramos.veld.example.events.OrderCancelledEvent;
import io.github.yasmramos.veld.example.events.OrderCreatedEvent;
import io.github.yasmramos.veld.example.events.OrderEvent;

/**
 * Handles order-related events.
 *
 * <p>Demonstrates various @Subscribe features including:
 * <ul>
 *   <li>Subscribing to specific event types</li>
 *   <li>Subscribing to base event types (polymorphism)</li>
 *   <li>Priority-based handling</li>
 *   <li>Filter expressions</li>
 * </ul>
 *
 * @author Veld Framework Team
 * @since 1.0.0-alpha.5
 */
@Component
public class OrderEventHandler {

    /**
     * Handles all order created events with high priority.
     * This runs first due to higher priority.
     *
     * @param event the order created event
     */
    @Subscribe(priority = 100)
    public void onOrderCreated(OrderCreatedEvent event) {
        System.out.println("  [OrderEventHandler] New order created!");
        System.out.println("    - Order ID: " + event.getOrderId());
        System.out.println("    - Amount: $" + String.format("%.2f", event.getAmount()));
        System.out.println("    - Items: " + event.getItems());
        System.out.println("    - Ship to: " + event.getShippingAddress());
    }

    /**
     * Special handler for VIP orders (amount > 1000).
     * Uses filter expression to only process high-value orders.
     *
     * @param event the order created event
     */
    @Subscribe(priority = 50, filter = "event.amount > 1000")
    public void onVIPOrderCreated(OrderCreatedEvent event) {
        System.out.println("  [OrderEventHandler] VIP ORDER DETECTED!");
        System.out.println("    - VIP Customer: " + event.getCustomerEmail());
        System.out.println("    - High-value order requires special handling");
    }

    /**
     * Handles order cancellation events.
     *
     * @param event the order cancelled event
     */
    @Subscribe
    public void onOrderCancelled(OrderCancelledEvent event) {
        System.out.println("  [OrderEventHandler] Order cancelled");
        System.out.println("    - Order ID: " + event.getOrderId());
        System.out.println("    - Reason: " + event.getReason());
        System.out.println("    - Refund issued: " + (event.isRefundIssued() ? "Yes" : "No"));
    }

    /**
     * Catches ALL order events (base type subscription).
     * Lower priority so it runs after specific handlers.
     *
     * @param event any order event
     */
    @Subscribe(priority = -100)
    public void onAnyOrderEvent(OrderEvent event) {
        System.out.println("  [OrderEventHandler] Order event logged: " + event.getEventType() + 
                " (ID: " + event.getOrderId() + ")");
    }
}
