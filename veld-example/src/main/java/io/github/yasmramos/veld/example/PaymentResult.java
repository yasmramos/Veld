package io.github.yasmramos.veld.example;

/**
 * Clase de dominio para resultados de pago.
 */
public class PaymentResult {
    private final String status;
    private final String transactionId;
    
    public PaymentResult(String status, String transactionId) {
        this.status = status;
        this.transactionId = transactionId;
    }
    
    public String getStatus() { return status; }
    public String getTransactionId() { return transactionId; }
}
