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
 * Base event class for all order-related events.
 *
 * <p>Contains common order information like order ID and amount
 * that is shared across all order event types.
 *
 * @author Veld Framework Team
 * @since 1.0.0
 */
public abstract class OrderEvent extends Event {

    private final String orderId;
    private final double amount;
    private final String customerEmail;

    /**
     * Creates a new order event.
     *
     * @param source        the object that created this event
     * @param orderId       the unique order identifier
     * @param amount        the order amount
     * @param customerEmail the customer's email address
     */
    protected OrderEvent(Object source, String orderId, double amount, String customerEmail) {
        super(source);
        this.orderId = orderId;
        this.amount = amount;
        this.customerEmail = customerEmail;
    }

    /**
     * Returns the order ID.
     *
     * @return the order ID
     */
    public String getOrderId() {
        return orderId;
    }

    /**
     * Returns the order amount.
     *
     * @return the amount
     */
    public double getAmount() {
        return amount;
    }

    /**
     * Returns the customer email.
     *
     * @return the customer email
     */
    public String getCustomerEmail() {
        return customerEmail;
    }

    /**
     * Returns whether this is a VIP order (amount > 1000).
     *
     * @return true if VIP order
     */
    public boolean isVip() {
        return amount > 1000;
    }

    @Override
    public String toString() {
        return String.format("%s[orderId=%s, amount=%.2f, customer=%s]",
                getEventType(), orderId, amount, customerEmail);
    }
}
