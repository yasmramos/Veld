package io.github.yasmramos.veld.example;

import io.github.yasmramos.veld.annotation.Component;
import io.github.yasmramos.veld.annotation.Inject;
import io.github.yasmramos.veld.annotation.Singleton;
import io.github.yasmramos.veld.annotation.Profile;
import io.github.yasmramos.veld.runtime.event.EventBus;

/**
 * Servicio para procesar pagos.
 * Only available in production profile since it depends on DatabaseService.
 */
@Profile("production")
@Singleton
@Component
public class PaymentService {
    private final DatabaseService databaseService;

    @Inject
    public PaymentService(DatabaseService databaseService) {
        this.databaseService = databaseService;
    }

    public boolean validatePayment(double amount) {
        return amount > 0;
    }

    public PaymentResult processPayment(String orderId, double amount) {
        // Simular procesamiento de pago
        String transactionId = "TXN-" + System.currentTimeMillis();

        // Publicar evento de pago procesado usando el singleton EventBus
        EventBus.getInstance().publish(new PaymentProcessedEvent(orderId, amount, transactionId));

        return new PaymentResult("SUCCESS", transactionId);
    }
}
