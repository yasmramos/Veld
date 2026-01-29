package io.github.yasmramos.veld.example;

import io.github.yasmramos.veld.annotation.Inject;
import io.github.yasmramos.veld.annotation.PostConstruct;
import io.github.yasmramos.veld.annotation.Singleton;
import io.github.yasmramos.veld.example.dependsOn.DatabaseService;

import java.util.Optional;

/**
 * Demonstration service that shows how to use conditional beans.
 * Uses Optional injection for conditionally-registered beans.
 */
@Singleton
public class ConditionalDemoService {
    
    // DatabaseService is optional - it may not be available in all profiles
    @Inject
    Optional<DatabaseService> databaseService;
    
    // DebugService is only available if app.debug=true
    @Inject
    Optional<DebugService> debugService;
    
    // FeatureXService is only available if feature.x.enabled is set
    @Inject
    Optional<FeatureXService> featureXService;
    
    // JacksonJsonService is only available if Jackson is on classpath
    @Inject
    Optional<JacksonJsonService> jacksonService;
    
    // Setters for field injection by Veld
    public void setDatabaseService(Optional<DatabaseService> databaseService) {
        this.databaseService = databaseService;
    }
    
    public void setDebugService(Optional<DebugService> debugService) {
        this.debugService = debugService;
    }
    
    public void setFeatureXService(Optional<FeatureXService> featureXService) {
        this.featureXService = featureXService;
    }
    
    public void setJacksonService(Optional<JacksonJsonService> jacksonService) {
        this.jacksonService = jacksonService;
    }
    
    @PostConstruct
    public void init() {
        System.out.println("[ConditionalDemoService] Initialized with:");
        System.out.println("  - DatabaseService: " + (databaseService.isPresent() ? databaseService.get().getConnectionInfo() : "NOT AVAILABLE"));
        System.out.println("  - DebugService: " + (debugService.isPresent() ? "AVAILABLE" : "NOT AVAILABLE"));
        System.out.println("  - FeatureXService: " + (featureXService.isPresent() ? "AVAILABLE" : "NOT AVAILABLE"));
        System.out.println("  - JacksonJsonService: " + (jacksonService.isPresent() ? "AVAILABLE" : "NOT AVAILABLE"));
    }
    
    public void runDemo() {
        System.out.println("\n=== Conditional Services Demo ===\n");
        
        // Database is available if present
        databaseService.ifPresent(db -> {
            db.connect();
            db.disconnect();
        });
        
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
        
        System.out.println("\n=== Demo Complete ===\n");
    }
}
