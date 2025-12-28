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

/**
 * Event published when an order is cancelled.
 *
 * <p>Contains information about the cancelled order and the reason
 * for cancellation.
 *
 * @author Veld Framework Team
 * @since 1.0.0
 */
public class OrderCancelledEvent extends OrderEvent {

    private final String reason;
    private final boolean refundIssued;

    /**
     * Creates a new order cancelled event.
     *
     * @param source        the object that created this event
     * @param orderId       the unique order identifier
     * @param amount        the order amount
     * @param customerEmail the customer's email address
     * @param reason        the cancellation reason
     * @param refundIssued  whether a refund was issued
     */
    public OrderCancelledEvent(Object source, String orderId, double amount,
                               String customerEmail, String reason,
                               boolean refundIssued) {
        super(source, orderId, amount, customerEmail);
        this.reason = reason;
        this.refundIssued = refundIssued;
    }

    /**
     * Returns the cancellation reason.
     *
     * @return the reason
     */
    public String getReason() {
        return reason;
    }

    /**
     * Returns whether a refund was issued.
     *
     * @return true if refund was issued
     */
    public boolean isRefundIssued() {
        return refundIssued;
    }
}
