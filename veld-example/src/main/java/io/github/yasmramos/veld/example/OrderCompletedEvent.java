package io.github.yasmramos.veld.example;

import io.github.yasmramos.veld.runtime.event.Event;

/**
 * Evento de orden completada.
 */
public class OrderCompletedEvent extends Event {
    private final String orderId;
    private final String userId;
    private final double amount;
    
    public OrderCompletedEvent(String orderId, String userId, double amount) {
        super();
        this.orderId = orderId;
        this.userId = userId;
        this.amount = amount;
    }
    
    public String getOrderId() { return orderId; }
    public String getUserId() { return userId; }
    public double getAmount() { return amount; }
}
