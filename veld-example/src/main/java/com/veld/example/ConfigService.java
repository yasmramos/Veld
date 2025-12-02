package com.veld.example;

import com.veld.annotation.Component;
import com.veld.annotation.Singleton;
import com.veld.annotation.Inject;
import com.veld.annotation.PostConstruct;

/**
 * Configuration service - Singleton scope.
 * Demonstrates field injection with @Inject.
 */
@Component
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
