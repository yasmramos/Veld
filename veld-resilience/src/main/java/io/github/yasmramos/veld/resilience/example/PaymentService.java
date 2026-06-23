package io.github.yasmramos.veld.resilience.example;

import io.github.yasmramos.veld.resilience.annotation.Retry;

import java.io.IOException;
import java.util.concurrent.ThreadLocalRandom;

public class PaymentService {

    @Retry(maxAttempts = 3)
    public String chargeCard(String cardId, double amount) {
        if (ThreadLocalRandom.current().nextInt(10) < 7) {
            throw new RuntimeException("Transient gateway error");
        }
        return "charged:" + cardId + ":" + amount;
    }

    @Retry(maxAttempts = 5, delay = 200)
    public String authorizePayment(String orderId) {
        if (ThreadLocalRandom.current().nextInt(10) < 6) {
            throw new RuntimeException("Authorization service unavailable");
        }
        return "authorized:" + orderId;
    }

    @Retry(maxAttempts = 4, delay = 100, backoffMultiplier = 2.0)
    public String capturePayment(String authId) {
        if (ThreadLocalRandom.current().nextInt(10) < 5) {
            throw new RuntimeException("Capture temporarily failed");
        }
        return "captured:" + authId;
    }

    @Retry(maxAttempts = 5, delay = 250, backoffMultiplier = 1.5, jitter = 100)
    public String refundPayment(String transactionId) {
        if (ThreadLocalRandom.current().nextInt(10) < 5) {
            throw new RuntimeException("Refund queue saturated");
        }
        return "refunded:" + transactionId;
    }

    @Retry(maxAttempts = 4, delay = 150, retryOn = {IOException.class})
    public String contactGateway(String endpoint) throws IOException {
        if (ThreadLocalRandom.current().nextInt(10) < 6) {
            throw new IOException("Network failure contacting " + endpoint);
        }
        return "ok:" + endpoint;
    }

    @Retry(
            maxAttempts = 5,
            delay = 200,
            backoffMultiplier = 2.0,
            jitter = 50,
            retryOn = {IOException.class, RuntimeException.class},
            abortOn = {IllegalArgumentException.class}
    )
    public String processPayment(String orderId, double amount) throws IOException {
        if (amount <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
        int roll = ThreadLocalRandom.current().nextInt(10);
        if (roll < 4) {
            throw new IOException("Gateway timeout for order " + orderId);
        }
        if (roll < 7) {
            throw new RuntimeException("Temporary processing error");
        }
        return "processed:" + orderId + ":" + amount;
    }
}
