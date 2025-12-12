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

import java.util.List;

/**
 * Event published when a new order is created.
 *
 * <p>Contains information about the newly created order including
 * the items purchased and shipping details.
 *
 * @author Veld Framework Team
 * @since 1.0.0-alpha.5
 */
public class OrderCreatedEvent extends OrderEvent {

    private final List<String> items;
    private final String shippingAddress;

    /**
     * Creates a new order created event.
     *
     * @param source          the object that created this event
     * @param orderId         the unique order identifier
     * @param amount          the total order amount
     * @param customerEmail   the customer's email address
     * @param items           the list of ordered items
     * @param shippingAddress the shipping address
     */
    public OrderCreatedEvent(Object source, String orderId, double amount,
                             String customerEmail, List<String> items,
                             String shippingAddress) {
        super(source, orderId, amount, customerEmail);
        this.items = items;
        this.shippingAddress = shippingAddress;
    }

    /**
     * Returns the list of ordered items.
     *
     * @return the items
     */
    public List<String> getItems() {
        return items;
    }

    /**
     * Returns the shipping address.
     *
     * @return the shipping address
     */
    public String getShippingAddress() {
        return shippingAddress;
    }

    /**
     * Returns the number of items in the order.
     *
     * @return item count
     */
    public int getItemCount() {
        return items != null ? items.size() : 0;
    }
}
