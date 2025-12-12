package io.github.yasmramos.veld.example;

import io.github.yasmramos.veld.annotation.ConditionalOnProperty;
import io.github.yasmramos.veld.annotation.PostConstruct;
import io.github.yasmramos.veld.annotation.Singleton;

/**
 * Feature X service that is activated when the feature flag is present.
 * 
 * This demonstrates @ConditionalOnProperty with matchIfMissing - this service 
 * is registered when "feature.x.enabled" exists (any value, not requiring specific value).
 */
@Singleton
@ConditionalOnProperty(name = "feature.x.enabled")
public class FeatureXService {
    
    @PostConstruct
    public void init() {
        System.out.println("[FeatureXService] Feature X is ENABLED");
    }
    
    public String getFeatureInfo() {
        return "Feature X: Advanced analytics and reporting";
    }
    
    public void executeFeature() {
        System.out.println("[FeatureXService] Executing Feature X functionality...");
    }
}
