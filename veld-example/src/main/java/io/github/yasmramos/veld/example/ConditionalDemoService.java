package io.github.yasmramos.veld.example;

import io.github.yasmramos.veld.annotation.Inject;
import io.github.yasmramos.veld.annotation.PostConstruct;
import io.github.yasmramos.veld.annotation.Singleton;

import java.util.Optional;

/**
 * Demonstration service that shows how to use conditional beans.
 * Uses Optional injection for conditionally-registered beans.
 */
@Singleton
public class ConditionalDemoService {
    
    // DatabaseService should always be available (default or custom)
    @Inject
    DatabaseService databaseService;
    
    // DebugService is only available if app.debug=true
    @Inject
    Optional<DebugService> debugService;
    
    // FeatureXService is only available if feature.x.enabled is set
    @Inject
    Optional<FeatureXService> featureXService;
    
    // JacksonJsonService is only available if Jackson is on classpath
    @Inject
    Optional<JacksonJsonService> jacksonService;
    
    @PostConstruct
    public void init() {
        System.out.println("[ConditionalDemoService] Initialized with:");
        System.out.println("  - DatabaseService: " + databaseService.getConnectionInfo());
        System.out.println("  - DebugService: " + (debugService.isPresent() ? "AVAILABLE" : "NOT AVAILABLE"));
        System.out.println("  - FeatureXService: " + (featureXService.isPresent() ? "AVAILABLE" : "NOT AVAILABLE"));
        System.out.println("  - JacksonJsonService: " + (jacksonService.isPresent() ? "AVAILABLE" : "NOT AVAILABLE"));
    }
    
    public void runDemo() {
        System.out.println("\n=== Conditional Services Demo ===\n");
        
        // Database is always available
        databaseService.connect();
        
        // Debug logging only if debug mode is enabled
        debugService.ifPresent(debug -> {
            debug.logDebug("This message only appears in debug mode");
        });
        
        // Feature X only if enabled
        featureXService.ifPresent(featureX -> {
            System.out.println("Feature X info: " + featureX.getFeatureInfo());
            featureX.executeFeature();
        });
        
        // Jackson only if on classpath
        jacksonService.ifPresent(jackson -> {
            System.out.println("JSON output: " + jackson.toJson(new Object()));
        });
        
        databaseService.disconnect();
        
        System.out.println("\n=== Demo Complete ===\n");
    }
}
