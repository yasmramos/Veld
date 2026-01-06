package io.github.yasmramos.veld.example;

import io.github.yasmramos.veld.annotation.ConditionalOnProperty;
import io.github.yasmramos.veld.annotation.PostConstruct;
import io.github.yasmramos.veld.annotation.Singleton;

/**
 * Debug service that is only activated when debug mode is enabled.
 * 
 * This demonstrates @ConditionalOnProperty - this service will only be
 * registered if the "app.debug" system property is set to "true".
 * 
 * To enable: java -Dapp.debug=true ...
 * Or: export APP_DEBUG=true
 */
@Singleton
@ConditionalOnProperty(name = "app.debug", havingValue = "true")
public class DebugService {
    
    @PostConstruct
    public void init() {
        System.out.println("[DebugService] Debug mode ENABLED - additional logging active");
    }
    
    public void logDebug(String message) {
        System.out.println("[DEBUG] " + message);
    }
    
    public void dumpState(Object obj) {
        System.out.println("[DEBUG] State dump for " + obj.getClass().getSimpleName() + ": " + obj);
    }
}
