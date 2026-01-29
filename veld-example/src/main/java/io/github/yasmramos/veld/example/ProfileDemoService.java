package io.github.yasmramos.veld.example;

import io.github.yasmramos.veld.annotation.Inject;
import io.github.yasmramos.veld.annotation.Optional;
import io.github.yasmramos.veld.annotation.PostConstruct;
import io.github.yasmramos.veld.annotation.Profile;
import io.github.yasmramos.veld.annotation.Singleton;

/**
 * Demonstrates how services can work with profile-specific dependencies.
 * Uses @Optional to gracefully handle dependencies that may not be present
 * based on the active profile.
 * Available in all profiles since it demonstrates features across profiles.
 */
@Profile({"dev", "test", "production"})
@Singleton
public class ProfileDemoService {
    
    @Inject
    @Optional
    DataSource dataSource;
    
    @Inject
    @Optional
    VerboseLoggingService verboseLogging;
    
    @Inject
    @Optional
    MockPaymentGateway mockPayment;
    
    // Setters for field injection by Veld
    public void setDataSource(DataSource dataSource) {
        this.dataSource = dataSource;
    }
    
    public void setVerboseLogging(VerboseLoggingService verboseLogging) {
        this.verboseLogging = verboseLogging;
    }
    
    public void setMockPayment(MockPaymentGateway mockPayment) {
        this.mockPayment = mockPayment;
    }
    
    @PostConstruct
    public void init() {
        System.out.println("[ProfileDemoService] Initialized with profile-specific dependencies:");
        System.out.println("  - DataSource: " + (dataSource != null ? dataSource.getName() : "NOT AVAILABLE"));
        System.out.println("  - VerboseLogging: " + (verboseLogging != null ? "AVAILABLE" : "NOT AVAILABLE"));
        System.out.println("  - MockPaymentGateway: " + (mockPayment != null ? "AVAILABLE" : "NOT AVAILABLE"));
    }
    
    /**
     * Demonstrates profile-aware database operations.
     */
    public void demonstrateDatabase() {
        System.out.println("\n=== Profile-Aware Database Demo ===\n");
        
        if (dataSource != null) {
            System.out.println("Active DataSource: " + dataSource.getName());
            System.out.println("Connection URL: " + dataSource.getConnectionUrl());
            dataSource.connect();
            dataSource.disconnect();
        } else {
            System.out.println("No DataSource available for current profile");
            System.out.println("Hint: Activate 'dev', 'prod', or 'test' profile");
        }
    }
    
    /**
     * Demonstrates profile-aware logging.
     */
    public void demonstrateLogging() {
        System.out.println("\n=== Profile-Aware Logging Demo ===\n");
        
        if (verboseLogging != null) {
            verboseLogging.debug("This is a debug message");
            verboseLogging.trace("Detailed trace", "param1=value1", "param2=value2");
        } else {
            System.out.println("Verbose logging not available (probably in prod profile)");
        }
    }
    
    /**
     * Demonstrates profile-aware payment processing.
     */
    public void demonstratePayment() {
        System.out.println("\n=== Profile-Aware Payment Demo ===\n");
        
        if (mockPayment != null) {
            String txnId = mockPayment.processPayment(99.99, "USD");
            System.out.println("Payment processed with mock gateway");
            System.out.println("Transaction ID: " + txnId);
        } else {
            System.out.println("Real payment gateway would be used in production");
        }
    }
    
    /**
     * Runs all profile demonstrations.
     */
    public void runAllDemos() {
        demonstrateDatabase();
        demonstrateLogging();
        demonstratePayment();
        System.out.println("\n=== Demo Complete ===\n");
    }
}
