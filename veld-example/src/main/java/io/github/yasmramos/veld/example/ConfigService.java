package io.github.yasmramos.veld.example;

import io.github.yasmramos.veld.annotation.Singleton;
import io.github.yasmramos.veld.annotation.Inject;
import io.github.yasmramos.veld.annotation.PostConstruct;

/**
 * Configuration service - Singleton scope.
 * Demonstrates field injection with @Inject.
 * 
 * Note: @Singleton implies @Component, so we don't need both.
 */
@Singleton
public class ConfigService {
    
    @Inject
    LogService logService;  // Package-private for bytecode injection (no reflection)
    
    private String appName = "Veld Example App";
    private String version = "1.0.0";
    private boolean debugMode = true;
    
    public ConfigService() {
        System.out.println("[ConfigService] Constructor called");
    }
    
    @PostConstruct
    public void init() {
        logService.log("ConfigService initialized with app: " + appName);
    }
    
    public String getAppName() {
        return appName;
    }
    
    public String getVersion() {
        return version;
    }
    
    public boolean isDebugMode() {
        return debugMode;
    }
    
    public LogService getLogService() {
        return logService;
    }
}
