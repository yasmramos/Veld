package io.github.yasmramos.veld.example;

import io.github.yasmramos.veld.annotation.Component;
import io.github.yasmramos.veld.annotation.Inject;
import io.github.yasmramos.veld.annotation.Singleton;
import io.github.yasmramos.veld.runtime.event.EventBus;

/**
 * Servicio para procesar pagos.
 */
@Singleton
@Component
public class PaymentService {
    private final DatabaseService databaseService;
    private final EventBus eventBus;
    
    @Inject
    public PaymentService(DatabaseService databaseService) {
        this.databaseService = databaseService;
        this.eventBus = EventBus.getInstance();
    }
    
    public boolean validatePayment(double amount) {
        return amount > 0;
    }
    
    public PaymentResult processPayment(String orderId, double amount) {
        // Simular procesamiento de pago
        String transactionId = "TXN-" + System.currentTimeMillis();
        
        // Publicar evento de pago procesado
        eventBus.publish(new PaymentProcessedEvent(orderId, amount, transactionId));
        
        return new PaymentResult("SUCCESS", transactionId);
    }
}
