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
import io.github.yasmramos.veld.example.events.NotificationEvent;
import io.github.yasmramos.veld.example.events.OrderCreatedEvent;

/**
 * Handles notification events and sends emails asynchronously.
 *
 * <p>Demonstrates async event handling to prevent blocking
 * the main execution thread for I/O operations like sending emails.
 *
 * @author Veld Framework Team
 * @since 1.0.0
 */
@Component
public class NotificationHandler {

    /**
     * Sends order confirmation emails asynchronously.
     * 
     * <p>Using async=true allows the order creation flow to continue
     * without waiting for email delivery.
     *
     * @param event the order created event
     */
    @Subscribe(async = true, priority = 10)
    public void sendOrderConfirmation(OrderCreatedEvent event) {
        // Simulate email sending delay
        try {
            Thread.sleep(100); // Simulated I/O delay
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        System.out.println("  [NotificationHandler] Email sent to: " + event.getCustomerEmail());
        System.out.println("    - Subject: Order Confirmation #" + event.getOrderId());
        System.out.println("    - (Sent asynchronously in thread: " + Thread.currentThread().getName() + ")");
    }

    /**
     * Handles general notifications synchronously.
     *
     * @param event the notification event
     */
    @Subscribe
    public void onNotification(NotificationEvent event) {
        System.out.println("  [NotificationHandler] Notification received:");
        System.out.println("    - Channel: " + event.getChannel());
        System.out.println("    - Priority: " + event.getPriority());
        System.out.println("    - Message: " + event.getMessage());
    }

    /**
     * Special handler for high-priority notifications.
     * Uses filter to only process urgent notifications.
     *
     * @param event the notification event
     */
    @Subscribe(priority = 100, filter = "event.highPriority == true")
    public void onUrgentNotification(NotificationEvent event) {
        System.out.println("  [NotificationHandler] URGENT NOTIFICATION!");
        System.out.println("    - Immediate attention required: " + event.getMessage());
    }
}
