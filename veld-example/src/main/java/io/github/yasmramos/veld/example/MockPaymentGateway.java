package io.github.yasmramos.veld.example;

import io.github.yasmramos.veld.annotation.PostConstruct;
import io.github.yasmramos.veld.annotation.Profile;
import io.github.yasmramos.veld.annotation.Singleton;

/**
 * Mock payment gateway for non-production environments.
 * Uses profile negation: registered when "prod" is NOT active.
 * 
 * <p>This demonstrates the negation syntax (!profile).
 */
@Singleton
@Profile("!prod")
public class MockPaymentGateway {
    
    private int processedCount = 0;
    
    @PostConstruct
    public void init() {
        System.out.println("[MockPaymentGateway] Mock payment gateway initialized (NON-PRODUCTION)");
        System.out.println("  -> All payments will be simulated, no real charges");
    }
    
    /**
     * Simulates processing a payment.
     * 
     * @param amount the payment amount
     * @param currency the currency code
     * @return a mock transaction ID
     */
    public String processPayment(double amount, String currency) {
        processedCount++;
        String txnId = "MOCK-TXN-" + System.currentTimeMillis();
        System.out.println("[MockPaymentGateway] SIMULATED payment: " + amount + " " + currency);
        System.out.println("  -> Transaction ID: " + txnId);
        return txnId;
    }
    
    /**
     * Gets the number of processed mock payments.
     * 
     * @return the count of processed payments
     */
    public int getProcessedCount() {
        return processedCount;
    }
    
    /**
     * Simulates a refund.
     * 
     * @param transactionId the original transaction ID
     * @param amount the refund amount
     * @return true if refund was successful
     */
    public boolean refund(String transactionId, double amount) {
        System.out.println("[MockPaymentGateway] SIMULATED refund for " + transactionId + ": " + amount);
        return true;
    }
}
