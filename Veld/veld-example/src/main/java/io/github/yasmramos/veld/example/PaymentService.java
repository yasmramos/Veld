package io.github.yasmramos.veld.example;

import io.github.yasmramos.veld.annotation.Component;
import io.github.yasmramos.veld.annotation.Inject;

/**
 * Example service using Veld annotations.
 * Demonstrates Veld's native annotation support for dependency injection.
 * 
 * Note: @Component provides the same functionality as @javax.inject.Singleton + @Named.
 * Veld recognizes Veld annotations natively.
 */
@Component("paymentService")
public class PaymentService {
    
    private LogService logService;
    private ConfigService configService;
    
    /**
     * Constructor injection using Veld @Inject
     */
    @Inject
    public PaymentService(LogService logService) {
        this.logService = logService;
        System.out.println("[PaymentService] Created with Veld @Inject constructor");
    }
    
    /**
     * Method injection using Veld @Inject
     */
    @Inject
    public void setConfigService(ConfigService configService) {
        this.configService = configService;
        System.out.println("[PaymentService] ConfigService injected via Veld @Inject method");
    }
    
    public void processPayment(String orderId, double amount) {
        logService.log("Processing payment for order: " + orderId + " amount: $" + amount);
        
        // Using existing ConfigService API
        String appName = configService.getAppName();
        String currency = "USD"; // Default currency
        System.out.println("[PaymentService] Payment for " + appName + " processed: " + amount + " " + currency);
    }
    
    public boolean validatePayment(double amount) {
        // Using a reasonable default max amount
        double maxAmount = 10000.0;
        boolean valid = amount <= maxAmount;
        if (configService.isDebugMode()) {
            logService.debug("Payment validation: " + amount + " <= " + maxAmount + " = " + valid);
        }
        return valid;
    }
}
