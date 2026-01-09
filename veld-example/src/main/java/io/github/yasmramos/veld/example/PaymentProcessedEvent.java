package io.github.yasmramos.veld.example;

import io.github.yasmramos.veld.runtime.event.Event;

/**
 * Evento de pago procesado.
 */
public class PaymentProcessedEvent extends Event {
    private final String orderId;
    private final double amount;
    private final String transactionId;
    
    public PaymentProcessedEvent(String orderId, double amount, String transactionId) {
        super();
        this.orderId = orderId;
        this.amount = amount;
        this.transactionId = transactionId;
    }
    
    public String getOrderId() { return orderId; }
    public double getAmount() { return amount; }
    public String getTransactionId() { return transactionId; }
}
