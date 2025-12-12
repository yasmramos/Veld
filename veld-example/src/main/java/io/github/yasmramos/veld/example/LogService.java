package io.github.yasmramos.veld.example;

import io.github.yasmramos.veld.annotation.Singleton;
import io.github.yasmramos.veld.annotation.PostConstruct;
import io.github.yasmramos.veld.annotation.PreDestroy;

/**
 * Simple logging service - Singleton scope.
 * Demonstrates @Singleton and lifecycle callbacks.
 * 
 * Note: @Singleton implies @Component, so we don't need both.
 */
@Singleton
public class LogService {
    
    private boolean initialized = false;
    
    public LogService() {
        System.out.println("[LogService] Constructor called");
    }
    
    @PostConstruct
    public void init() {
        this.initialized = true;
        System.out.println("[LogService] @PostConstruct - Service initialized");
    }
    
    @PreDestroy
    public void cleanup() {
        System.out.println("[LogService] @PreDestroy - Service shutting down");
    }
    
    public void log(String message) {
        System.out.println("[LOG] " + message);
    }
    
    public void debug(String message) {
        System.out.println("[DEBUG] " + message);
    }
    
    public boolean isInitialized() {
        return initialized;
    }
}
