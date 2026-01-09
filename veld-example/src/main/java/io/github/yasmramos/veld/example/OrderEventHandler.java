package io.github.yasmramos.veld.example;

import io.github.yasmramos.veld.annotation.Component;
import io.github.yasmramos.veld.annotation.Singleton;
import io.github.yasmramos.veld.annotation.Subscribe;

/**
 * Manejador de eventos de órdenes.
 */
@Singleton
@Component
public class OrderEventHandler {
    @Subscribe
    public void onOrderCompleted(OrderCompletedEvent event) {
        System.out.println("  [EventHandler] Orden completada: " + event.getOrderId() + 
                         " por $" + event.getAmount());
    }
    
    @Subscribe
    public void onPaymentProcessed(PaymentProcessedEvent event) {
        System.out.println("  [EventHandler] Pago procesado: " + event.getTransactionId() + 
                         " para orden " + event.getOrderId());
    }
}
