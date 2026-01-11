package io.github.yasmramos.veld.example;

import io.github.yasmramos.veld.annotation.Inject;
import io.github.yasmramos.veld.annotation.Optional;
import io.github.yasmramos.veld.annotation.Singleton;
import io.github.yasmramos.veld.annotation.PostConstruct;

/**
 * Demonstrates optional dependency injection.
 * 
 * This service has dependencies that may or may not exist:
 * 1. CacheService with @Optional - will be null if not registered
 * 2. Optional<MetricsService> - will be Optional.empty() if not registered
 */
@Singleton
public class OptionalDemoService {
    
    // Field injection with @Optional annotation
    // Will be null if CacheService is not registered as a component
    @Inject
    @Optional
    CacheService cacheService;
    
    // Field injection with Optional<T> wrapper
    // Will be Optional.empty() if MetricsService is not registered
    @Inject
    java.util.Optional<io.github.yasmramos.veld.example.lifecycle.MetricsService> metricsService;
    
    // Required dependency (will fail if not found)
    private final LogService logService;
    
    @Inject
    public OptionalDemoService(LogService logService) {
        this.logService = logService;
    }
    
    // Setters for field injection by Veld
    public void setCacheService(CacheService cacheService) {
        this.cacheService = cacheService;
    }
    
    public void setMetricsService(java.util.Optional<io.github.yasmramos.veld.example.lifecycle.MetricsService> metricsService) {
        this.metricsService = metricsService;
    }
    
    @PostConstruct
    public void init() {
        System.out.println("  OptionalDemoService initialized");
        System.out.println("    -> CacheService present: " + (cacheService != null));
        System.out.println("    -> MetricsService present: " + metricsService.isPresent());
    }
    
    public void doWork() {
        logService.log("OptionalDemoService doing work...");
        
        // Safely use optional cache
        if (cacheService != null) {
            cacheService.put("lastWork", System.currentTimeMillis());
            logService.log("  -> Cached work timestamp");
        } else {
            logService.log("  -> Cache not available (optional dependency missing)");
        }
        
        // Safely use optional metrics with Optional API
        metricsService.ifPresentOrElse(
            metrics -> {
                metrics.recordEvent("work.completed");
                logService.log("  -> Recorded metrics");
            },
            () -> logService.log("  -> Metrics not available (optional dependency missing)")
        );
    }
}
