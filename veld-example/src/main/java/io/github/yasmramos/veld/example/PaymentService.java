package io.github.yasmramos.veld.example;

/**
 * Example service using JSR-330 (javax.inject) annotations.
 * Demonstrates Veld's compatibility with the standard Java DI API.
 * 
 * Note: @javax.inject.Singleton alone is sufficient - no @Component needed.
 * Veld recognizes JSR-330 annotations directly.
 */
@javax.inject.Singleton
@javax.inject.Named("paymentService")
public class PaymentService {
    
    private LogService logService;
    private ConfigService configService;
    
    /**
     * Constructor injection using javax.inject.Inject
     */
    @javax.inject.Inject
    public PaymentService(LogService logService) {
        this.logService = logService;
        System.out.println("[PaymentService] Created with javax.inject.Inject constructor");
    }
    
    /**
     * Method injection using javax.inject.Inject
     */
    @javax.inject.Inject
    public void setConfigService(ConfigService configService) {
        this.configService = configService;
        System.out.println("[PaymentService] ConfigService injected via javax.inject.Inject method");
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
